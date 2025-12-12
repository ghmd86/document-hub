package com.documenthub.service.extraction;

import com.documenthub.model.extraction.DataExtractionConfig;
import com.documenthub.model.extraction.DataSourceConfig;
import com.documenthub.model.extraction.FieldSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds extraction plans that determine which APIs to call and in what order.
 *
 * <p><b>What:</b> Analyzes the data extraction configuration and creates an
 * ordered list of API calls needed to extract all required fields.</p>
 *
 * <p><b>Why:</b> Fields may have dependencies - some fields require values
 * extracted from other APIs. For example:
 * <ul>
 *   <li>Field "disclosureCode" requires "accountId" (from request)</li>
 *   <li>Field "productDetails" requires "disclosureCode" (from API-A)</li>
 * </ul>
 * The plan builder determines the correct order of API calls to satisfy
 * these dependencies.</p>
 *
 * <p><b>How:</b> Uses an iterative algorithm:
 * <ol>
 *   <li>Start with available context (accountId, customerId from request)</li>
 *   <li>Find fields whose required inputs are all available</li>
 *   <li>Add those fields' source APIs to the plan</li>
 *   <li>Mark the API's output fields as available</li>
 *   <li>Repeat until all fields are covered or no progress can be made</li>
 * </ol>
 * </p>
 *
 * <p><b>Example:</b>
 * <pre>
 * Config: {
 *   fieldsToExtract: ["disclosureCode", "productName"],
 *   fieldSources: {
 *     "disclosureCode": { sourceApi: "account-api", requiredInputs: ["accountId"] },
 *     "productName": { sourceApi: "product-api", requiredInputs: ["disclosureCode"] }
 *   }
 * }
 * Context: { accountId: "123" }
 *
 * Result Plan:
 *   1. Call account-api (has accountId) -> provides disclosureCode
 *   2. Call product-api (now has disclosureCode) -> provides productName
 * </pre>
 * </p>
 *
 * @see ExtractionPlan
 * @see DataExtractionConfig
 */
@Component
@Slf4j
public class ExtractionPlanBuilder {

    /**
     * Maximum number of iterations to prevent infinite loops.
     * If we can't resolve all dependencies in 10 iterations, something is wrong
     * with the configuration (circular dependencies or missing data sources).
     */
    private static final int MAX_ITERATIONS = 10;

    /**
     * Builds an execution plan for data extraction.
     *
     * <p><b>What:</b> Main entry point that creates the extraction plan.</p>
     *
     * <p><b>Why:</b> Before executing API calls, we need to determine which
     * APIs to call and in what order to satisfy field dependencies.</p>
     *
     * <p><b>How:</b> Iteratively processes fields, adding APIs to the plan
     * when their required inputs are available, until all fields are covered.</p>
     *
     * @param config The data extraction configuration from the template
     * @param availableContext Initial context with values from the request
     * @return An ExtractionPlan containing ordered API calls
     */
    public ExtractionPlan buildPlan(
            DataExtractionConfig config,
            Map<String, Object> availableContext) {

        ExtractionPlan plan = new ExtractionPlan();
        Set<String> fieldsToExtract = new HashSet<>(config.getFieldsToExtract());
        Set<String> availableFields = new HashSet<>(availableContext.keySet());
        Set<String> processedApis = new HashSet<>();

        log.debug("Planning extraction for fields: {}", fieldsToExtract);

        int iterations = 0;
        while (!fieldsToExtract.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            boolean progressMade = processIteration(
                    config, plan, fieldsToExtract,
                    availableFields, processedApis);

            if (!progressMade) {
                log.warn("Cannot make progress. Remaining: {}", fieldsToExtract);
                break;
            }
        }

        logUnextractedFields(fieldsToExtract);
        return plan;
    }

    /**
     * Processes one iteration of the planning algorithm.
     *
     * <p><b>What:</b> Attempts to add APIs for fields whose inputs are ready.</p>
     *
     * <p><b>Why:</b> Each iteration may unlock new APIs. For example, after
     * adding API-A which provides "disclosureCode", the next iteration can
     * add API-B which requires "disclosureCode".</p>
     *
     * <p><b>How:</b> Iterates through remaining fields, checks if their
     * required inputs are available, and adds their source APIs to the plan.</p>
     *
     * @param config The extraction configuration
     * @param plan The plan being built
     * @param fieldsToExtract Set of fields still needing extraction
     * @param availableFields Set of fields currently available
     * @param processedApis Set of APIs already added to the plan
     * @return true if any progress was made (fields were processed)
     */
    private boolean processIteration(
            DataExtractionConfig config,
            ExtractionPlan plan,
            Set<String> fieldsToExtract,
            Set<String> availableFields,
            Set<String> processedApis) {

        boolean progressMade = false;

        for (String fieldName : new ArrayList<>(fieldsToExtract)) {
            FieldSourceConfig fieldSource = config.getFieldSources().get(fieldName);

            if (fieldSource == null) {
                log.warn("No source config for field: {}", fieldName);
                fieldsToExtract.remove(fieldName);
                continue;
            }

            if (hasRequiredInputs(fieldSource, availableFields)) {
                progressMade = tryAddApiCall(
                        config, plan, fieldSource, fieldName,
                        availableFields, processedApis, fieldsToExtract);
            }
        }

        return progressMade;
    }

    /**
     * Attempts to add an API call to the plan for a field.
     *
     * <p><b>What:</b> Adds the field's source API to the plan if not already added.</p>
     *
     * <p><b>Why:</b> Multiple fields may come from the same API. We only want
     * to call each API once, even if it provides multiple fields.</p>
     *
     * <p><b>How:</b> Checks if the API is already in the plan. If not, adds it
     * and marks all its output fields as available for subsequent iterations.</p>
     *
     * @param config The extraction configuration
     * @param plan The plan being built
     * @param fieldSource Configuration for the field being processed
     * @param fieldName Name of the field being processed
     * @param availableFields Set to update with newly available fields
     * @param processedApis Set to track APIs already in the plan
     * @param fieldsToExtract Set to remove processed fields from
     * @return true (always makes progress by removing the field)
     */
    private boolean tryAddApiCall(
            DataExtractionConfig config,
            ExtractionPlan plan,
            FieldSourceConfig fieldSource,
            String fieldName,
            Set<String> availableFields,
            Set<String> processedApis,
            Set<String> fieldsToExtract) {

        String apiId = fieldSource.getSourceApi();

        if (!processedApis.contains(apiId)) {
            DataSourceConfig dataSource = config.getDataSources().get(apiId);

            if (dataSource != null) {
                plan.addApiCall(apiId, dataSource, config.getFieldSources());
                processedApis.add(apiId);
                availableFields.addAll(dataSource.getProvidesFields());

                log.debug("Added API '{}' to plan", apiId);
            }
        }

        fieldsToExtract.remove(fieldName);
        return true;
    }

    /**
     * Checks if all required inputs for a field are available.
     *
     * <p><b>What:</b> Determines if we can extract this field now.</p>
     *
     * <p><b>Why:</b> Fields may depend on values from other APIs. We can only
     * call an API when all its required inputs are available in the context.</p>
     *
     * <p><b>How:</b> Checks that every required input field exists in the
     * available fields set. Returns true if no inputs are required.</p>
     *
     * @param fieldSource Configuration for the field
     * @param availableFields Set of currently available field names
     * @return true if all required inputs are available (or no inputs needed)
     */
    private boolean hasRequiredInputs(
            FieldSourceConfig fieldSource,
            Set<String> availableFields) {

        if (fieldSource.getRequiredInputs() == null) {
            return true;
        }
        if (fieldSource.getRequiredInputs().isEmpty()) {
            return true;
        }

        return fieldSource.getRequiredInputs().stream()
                .allMatch(availableFields::contains);
    }

    /**
     * Logs a warning for fields that couldn't be planned for extraction.
     *
     * <p><b>What:</b> Reports fields that remain after planning completes.</p>
     *
     * <p><b>Why:</b> Remaining fields indicate configuration issues:
     * <ul>
     *   <li>Missing data source definitions</li>
     *   <li>Circular dependencies between APIs</li>
     *   <li>Required inputs that are never provided</li>
     * </ul>
     * Logging helps diagnose these configuration problems.</p>
     *
     * @param fieldsToExtract Set of fields that couldn't be planned
     */
    private void logUnextractedFields(Set<String> fieldsToExtract) {
        if (!fieldsToExtract.isEmpty()) {
            log.warn("Could not plan extraction for: {}", fieldsToExtract);
        }
    }
}
