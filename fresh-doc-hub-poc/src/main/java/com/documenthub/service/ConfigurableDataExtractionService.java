package com.documenthub.service;

import com.documenthub.model.DocumentListRequest;
import com.documenthub.model.extraction.DataExtractionConfig;
import com.documenthub.service.extraction.ApiCallExecutor;
import com.documenthub.service.extraction.ExtractionPlan;
import com.documenthub.service.extraction.ExtractionPlanBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configurable Data Extraction Service.
 *
 * <p><b>What:</b> Coordinates the extraction of data from external APIs based on
 * JSON configuration stored in the database (master_template_definition.data_extraction_config).</p>
 *
 * <p><b>Why:</b> Different document templates require different contextual data
 * (e.g., disclosureCode, customerLocation, accountStatus) that must be fetched from
 * various internal APIs. Instead of hardcoding these API calls, this service reads
 * a JSON configuration that defines which APIs to call and how to extract the needed fields.</p>
 *
 * <p><b>How:</b> The service follows these steps:
 * <ol>
 *   <li>Parse the JSON configuration from the database</li>
 *   <li>Create an initial context with data from the request (accountId, customerId, etc.)</li>
 *   <li>Build an execution plan using {@link ExtractionPlanBuilder}</li>
 *   <li>Execute the plan using {@link ApiCallExecutor} (sequential or parallel)</li>
 *   <li>Return a map of extracted field values</li>
 * </ol>
 * </p>
 *
 * @see ExtractionPlanBuilder
 * @see ApiCallExecutor
 * @see DataExtractionConfig
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigurableDataExtractionService {

    private final ObjectMapper objectMapper;
    private final ExtractionPlanBuilder planBuilder;
    private final ApiCallExecutor apiCallExecutor;

    /**
     * Extracts required fields based on the data_extraction_config JSON.
     *
     * <p><b>What:</b> Main entry point that orchestrates the entire data extraction process.</p>
     *
     * <p><b>Why:</b> Templates need additional context data (beyond accountId/customerId)
     * to properly filter and retrieve documents. This method fetches that data from
     * configured API endpoints.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Validates the config JSON is present</li>
     *   <li>Parses JSON into {@link DataExtractionConfig} object</li>
     *   <li>Creates initial context from the request</li>
     *   <li>Builds and executes the extraction plan</li>
     * </ol>
     * </p>
     *
     * @param dataExtractionConfigJson JSON from master_template_definition.data_extraction_config
     * @param request Original request containing accountId, customerId, etc.
     * @return Mono containing a map of extracted field names to their values
     */
    public Mono<Map<String, Object>> extractData(
            String dataExtractionConfigJson,
            DocumentListRequest request) {

        log.info("Starting data extraction");

        if (dataExtractionConfigJson == null) {
            log.warn("No data extraction config provided");
            return Mono.just(Collections.emptyMap());
        }

        try {
            DataExtractionConfig config = parseConfig(dataExtractionConfigJson);
            Map<String, Object> context = createInitialContext(request);
            ExtractionPlan plan = planBuilder.buildPlan(config, context);

            if (plan.isEmpty()) {
                log.info("No API calls needed");
                return Mono.just(context);
            }

            return executePlan(plan, config, context);
        } catch (Exception e) {
            log.error("Failed to parse config: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    /**
     * Overload for backward compatibility with Json type (for entity layer).
     */
    public Mono<Map<String, Object>> extractData(
            Json dataExtractionConfigJson,
            DocumentListRequest request) {
        if (dataExtractionConfigJson == null) {
            return Mono.just(Collections.emptyMap());
        }
        return extractData(dataExtractionConfigJson.asString(), request);
    }

    /**
     * Parses the PostgreSQL JSON column into a DataExtractionConfig object.
     *
     * <p><b>What:</b> Converts raw JSON into a strongly-typed configuration object.</p>
     *
     * <p><b>Why:</b> Working with typed objects provides compile-time safety and
     * makes the code easier to understand and maintain.</p>
     *
     * <p><b>How:</b> Uses Jackson ObjectMapper to first convert the PostgreSQL Json
     * type to a JsonNode, then deserializes it into DataExtractionConfig.</p>
     *
     * @param configJson The raw JSON from the database
     * @return Parsed DataExtractionConfig object
     * @throws Exception If JSON parsing fails
     */
    private DataExtractionConfig parseConfig(String configJson) throws Exception {
        JsonNode configNode = objectMapper.readTree(configJson);
        DataExtractionConfig config = objectMapper.treeToValue(
                configNode, DataExtractionConfig.class);

        log.info("Config parsed - fields: {}, sources: {}",
                getFieldCount(config), getSourceCount(config));

        return config;
    }

    /**
     * Gets the count of fields to extract from the config.
     *
     * @param config The extraction configuration
     * @return Number of fields to extract, or 0 if not defined
     */
    private int getFieldCount(DataExtractionConfig config) {
        return config.getFieldsToExtract() != null
                ? config.getFieldsToExtract().size() : 0;
    }

    /**
     * Gets the count of data sources in the config.
     *
     * @param config The extraction configuration
     * @return Number of data sources, or 0 if not defined
     */
    private int getSourceCount(DataExtractionConfig config) {
        return config.getDataSources() != null
                ? config.getDataSources().size() : 0;
    }

    /**
     * Creates the initial context map from the incoming request.
     *
     * <p><b>What:</b> Builds a map containing all input values that can be used
     * as placeholders in API calls.</p>
     *
     * <p><b>Why:</b> API endpoints often need request parameters like accountId
     * or customerId in their URLs or request bodies. This context map provides
     * those values for placeholder substitution.</p>
     *
     * <p><b>How:</b> Extracts relevant fields from the DocumentListRequest and
     * adds system variables (correlationId, auth token).</p>
     *
     * @param request The incoming document list request
     * @return Map of field names to values for use in API calls
     */
    private Map<String, Object> createInitialContext(DocumentListRequest request) {
        Map<String, Object> context = new HashMap<>();

        addAccountId(context, request);
        addCustomerId(context, request);
        addReferenceKey(context, request);
        addSystemVariables(context);

        log.debug("Initial context: {}", context.keySet());
        return context;
    }

    /**
     * Adds the first account ID from the request to the context.
     *
     * <p><b>Why:</b> APIs typically need a single account ID, so we use the first one.</p>
     *
     * @param context The context map to populate
     * @param request The incoming request
     */
    private void addAccountId(Map<String, Object> context, DocumentListRequest request) {
        if (request.getAccountId() != null && !request.getAccountId().isEmpty()) {
            context.put("accountId", request.getAccountId().get(0));
        }
    }

    /**
     * Adds the customer ID from the request to the context.
     *
     * @param context The context map to populate
     * @param request The incoming request
     */
    private void addCustomerId(Map<String, Object> context, DocumentListRequest request) {
        if (request.getCustomerId() != null) {
            context.put("customerId", request.getCustomerId().toString());
        }
    }

    /**
     * Adds reference key and type from the request to the context.
     *
     * <p><b>Why:</b> Reference keys are used for conditional document matching
     * and may be needed by external APIs.</p>
     *
     * @param context The context map to populate
     * @param request The incoming request
     */
    private void addReferenceKey(Map<String, Object> context, DocumentListRequest request) {
        if (request.getReferenceKey() != null) {
            context.put("referenceKey", request.getReferenceKey());
        }
        if (request.getReferenceKeyType() != null) {
            context.put("referenceKeyType", request.getReferenceKeyType());
        }
    }

    /**
     * Adds system-level variables to the context.
     *
     * <p><b>What:</b> Adds correlationId and auth token to the context.</p>
     *
     * <p><b>Why:</b> These are needed for API authentication and request tracing.</p>
     *
     * <p><b>How:</b> Generates a new UUID for correlation and uses a mock token
     * (TODO: should be retrieved from security context in production).</p>
     *
     * @param context The context map to populate
     */
    private void addSystemVariables(Map<String, Object> context) {
        context.put("correlationId", UUID.randomUUID().toString());
        context.put("auth.token", "mock-token-12345");
    }

    /**
     * Executes the extraction plan using the appropriate execution mode.
     *
     * <p><b>What:</b> Delegates plan execution to ApiCallExecutor.</p>
     *
     * <p><b>Why:</b> The config can specify parallel or sequential execution.
     * Parallel is faster but sequential is needed when APIs depend on each other.</p>
     *
     * <p><b>How:</b> Checks the executionStrategy.mode in config and calls the
     * appropriate executor method.</p>
     *
     * @param plan The execution plan containing API calls to make
     * @param config The extraction configuration with execution strategy
     * @param context The context map with available field values
     * @return Mono containing the updated context with extracted fields
     */
    private Mono<Map<String, Object>> executePlan(
            ExtractionPlan plan,
            DataExtractionConfig config,
            Map<String, Object> context) {

        log.info("Executing {} API call(s)", plan.size());

        String mode = getExecutionMode(config);

        if ("parallel".equalsIgnoreCase(mode)) {
            return apiCallExecutor.executeParallel(plan, context)
                    .doOnSuccess(this::logSuccess);
        }

        return apiCallExecutor.executeSequential(plan, context)
                .doOnSuccess(this::logSuccess);
    }

    /**
     * Gets the execution mode from the config.
     *
     * @param config The extraction configuration
     * @return "parallel" or "sequential" (default)
     */
    private String getExecutionMode(DataExtractionConfig config) {
        if (config.getExecutionStrategy() == null) {
            return "sequential";
        }
        return config.getExecutionStrategy().getMode();
    }

    /**
     * Logs successful extraction completion.
     *
     * @param result The map of extracted fields
     */
    private void logSuccess(Map<String, Object> result) {
        log.info("Extraction completed - {} fields: {}",
                result.size(), result.keySet());
    }
}
