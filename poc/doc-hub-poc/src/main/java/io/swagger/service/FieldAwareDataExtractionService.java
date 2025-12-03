package io.swagger.service;

import io.swagger.model.config.*;
import io.swagger.model.context.ExtractionContext;
import io.swagger.model.DocumentListRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Field-aware data extraction service that knows which APIs to call
 * to retrieve specific fields based on the Data Source Registry
 */
@Service
@Slf4j
public class FieldAwareDataExtractionService {

    @Autowired
    private DataExtractionEngine dataExtractionEngine;

    /**
     * Extract data for specific fields using the registry
     *
     * @param requiredFields Fields needed (e.g., ["disclosureCode", "customerLocation"])
     * @param registry Data source registry with field-to-API mappings
     * @param request Initial request with accountId/customerId
     * @return ExtractionContext with extracted fields
     */
    public Mono<ExtractionContext> extractFieldsFromSources(
        Set<String> requiredFields,
        DataSourceRegistry registry,
        DocumentListRequest request
    ) {
        log.info("Extracting {} fields using data source registry", requiredFields.size());

        // Create initial context with input data
        ExtractionContext context = createInitialContext(request);

        // Build extraction plan: which APIs to call to get required fields
        ExtractionPlan plan = buildExtractionPlan(requiredFields, registry, context);

        if (plan.getDataSourcesToExecute().isEmpty()) {
            log.warn("No data sources to execute for required fields");
            return Mono.just(context);
        }

        log.info("Extraction plan: calling {} APIs to retrieve fields",
            plan.getDataSourcesToExecute().size());

        // Create DataExtractionConfig from the plan
        DataExtractionConfig config = DataExtractionConfig.builder()
            .extractionStrategy(plan.getDataSourcesToExecute())
            .executionRules(ExecutionRules.builder()
                .executionMode("sequential") // Sequential to handle dependencies
                .build())
            .build();

        // Execute the extraction
        return dataExtractionEngine.executeExtractionStrategy(config, request)
            .doOnSuccess(result -> {
                log.info("Extraction completed. Retrieved {} variables",
                    result.getVariables().size());
                logExtractionResults(result, requiredFields);
            });
    }

    /**
     * Build an execution plan: determine which APIs to call and in what order
     */
    private ExtractionPlan buildExtractionPlan(
        Set<String> requiredFields,
        DataSourceRegistry registry,
        ExtractionContext context
    ) {
        ExtractionPlan plan = new ExtractionPlan();
        Set<String> fieldsToRetrieve = new HashSet<>(requiredFields);
        Set<String> availableFields = new HashSet<>(context.getVariables().keySet());
        Set<String> processedDataSources = new HashSet<>();

        log.debug("Building extraction plan for fields: {}", requiredFields);
        log.debug("Available fields in context: {}", availableFields);

        // Iteratively add data sources until all fields are covered
        int iterations = 0;
        int maxIterations = 10; // Prevent infinite loops

        while (!fieldsToRetrieve.isEmpty() && iterations < maxIterations) {
            iterations++;
            boolean progressMade = false;

            // Find fields that can be retrieved with current available data
            for (String fieldName : new ArrayList<>(fieldsToRetrieve)) {
                FieldSourceMapping mapping = registry.getFieldSource(fieldName);

                if (mapping == null) {
                    log.warn("No source mapping found for field: {}", fieldName);
                    fieldsToRetrieve.remove(fieldName);
                    continue;
                }

                // Check if we have all required inputs
                if (hasRequiredInputs(mapping, availableFields)) {
                    String dataSourceId = mapping.getSourceDataSourceId();

                    if (!processedDataSources.contains(dataSourceId)) {
                        DataSourceConfig dataSource = registry.getDataSource(dataSourceId);

                        if (dataSource != null) {
                            plan.addDataSource(dataSource);
                            processedDataSources.add(dataSourceId);

                            // Mark fields from this data source as available
                            List<String> fieldsFromSource = registry.getFieldsFromDataSource(dataSourceId);
                            availableFields.addAll(fieldsFromSource);

                            log.debug("Added data source '{}' to plan (provides: {})",
                                dataSourceId, fieldsFromSource);

                            progressMade = true;
                        } else {
                            log.warn("Data source '{}' not found in registry", dataSourceId);
                        }
                    }

                    fieldsToRetrieve.remove(fieldName);
                    progressMade = true;
                }
            }

            if (!progressMade) {
                log.warn("Cannot make progress. Remaining fields: {}. Missing inputs?",
                    fieldsToRetrieve);
                break;
            }
        }

        if (!fieldsToRetrieve.isEmpty()) {
            log.warn("Could not plan extraction for fields: {}", fieldsToRetrieve);
            plan.setUnresolvableFields(fieldsToRetrieve);
        }

        return plan;
    }

    /**
     * Check if all required inputs are available
     */
    private boolean hasRequiredInputs(FieldSourceMapping mapping, Set<String> availableFields) {
        if (mapping.getRequiredInputs() == null || mapping.getRequiredInputs().isEmpty()) {
            return true;
        }

        boolean hasAll = mapping.getRequiredInputs().stream()
            .allMatch(availableFields::contains);

        if (!hasAll) {
            log.debug("Missing inputs for field '{}': required={}, available={}",
                mapping.getFieldName(),
                mapping.getRequiredInputs(),
                availableFields);
        }

        return hasAll;
    }

    /**
     * Create initial context with input data from request
     */
    private ExtractionContext createInitialContext(DocumentListRequest request) {
        ExtractionContext context = ExtractionContext.builder()
            .customerId(request.getCustomerId())
            .correlationId(UUID.randomUUID().toString())
            .build();

        // Add request data to available fields
        if (request.getAccountId() != null && !request.getAccountId().isEmpty()) {
            context.addVariable("accountId", request.getAccountId().get(0));
        }
        if (request.getCustomerId() != null) {
            context.addVariable("customerId", request.getCustomerId().toString());
        }
        if (request.getReferenceKey() != null) {
            context.addVariable("referenceKey", request.getReferenceKey());
        }
        if (request.getReferenceKeyType() != null) {
            context.addVariable("referenceKeyType", request.getReferenceKeyType());
        }

        return context;
    }

    /**
     * Log extraction results
     */
    private void logExtractionResults(ExtractionContext context, Set<String> requiredFields) {
        for (String field : requiredFields) {
            Object value = context.getVariables().get(field);
            if (value != null) {
                log.info("Successfully extracted '{}': {}", field, value);
            } else {
                log.warn("Field '{}' was not extracted", field);
            }
        }
    }

    /**
     * Internal class to represent an extraction plan
     */
    private static class ExtractionPlan {
        private final List<DataSourceConfig> dataSourcesToExecute = new ArrayList<>();
        private Set<String> unresolvableFields = new HashSet<>();

        public void addDataSource(DataSourceConfig dataSource) {
            dataSourcesToExecute.add(dataSource);
        }

        public List<DataSourceConfig> getDataSourcesToExecute() {
            return dataSourcesToExecute;
        }

        public Set<String> getUnresolvableFields() {
            return unresolvableFields;
        }

        public void setUnresolvableFields(Set<String> fields) {
            this.unresolvableFields = fields;
        }
    }
}
