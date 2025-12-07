package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Root configuration for data extraction stored in master_template_definition.data_extraction_config
 * Defines WHAT fields are needed and WHERE to fetch them from
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExtractionConfig {

    /**
     * List of field names to extract from external APIs for eligibility/document matching.
     * Note: This is different from the `required_fields` column which is used for upload validation.
     */
    @JsonProperty("fieldsToExtract")
    private List<String> fieldsToExtract;

    /**
     * Maps field names to their source configurations
     * Key: field name (e.g., "disclosureCode")
     * Value: FieldSourceConfig defining where/how to fetch it
     */
    @JsonProperty("fieldSources")
    private Map<String, FieldSourceConfig> fieldSources;

    /**
     * Maps API IDs to their endpoint configurations
     * Key: API ID (e.g., "accountDetailsApi")
     * Value: DataSourceConfig defining the API endpoint
     */
    @JsonProperty("dataSources")
    private Map<String, DataSourceConfig> dataSources;

    /**
     * Execution strategy (parallel/sequential, timeout, etc.)
     */
    @JsonProperty("executionStrategy")
    private ExecutionStrategy executionStrategy;
}
