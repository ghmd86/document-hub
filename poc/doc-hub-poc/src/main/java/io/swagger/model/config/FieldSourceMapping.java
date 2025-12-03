package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Maps a business field to its data source(s)
 * This tells the system which API to call to retrieve a specific field
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSourceMapping {
    /**
     * The field name in the context (e.g., "disclosureCode", "customerLocation")
     */
    private String fieldName;

    /**
     * Description of what this field represents
     */
    private String description;

    /**
     * The data source ID that provides this field
     */
    private String sourceDataSourceId;

    /**
     * Alternative data sources (fallback options)
     */
    private List<String> fallbackDataSources;

    /**
     * JSONPath expression to extract this field from the source response
     */
    private String extractionPath;

    /**
     * Required input fields to call this data source
     * (e.g., ["customerId"] or ["accountId"])
     */
    private List<String> requiredInputs;

    /**
     * Whether this field is cacheable
     */
    private Boolean cacheable;

    /**
     * Cache TTL in seconds (if cacheable)
     */
    private Integer cacheTtlSeconds;

    /**
     * Field type (for validation)
     */
    private String fieldType;

    /**
     * Default value if extraction fails
     */
    private Object defaultValue;
}
