package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for a single field - defines WHERE and HOW to fetch it
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSourceConfig {

    /**
     * Description of what this field represents
     */
    @JsonProperty("description")
    private String description;

    /**
     * Primary API ID to fetch from (references DataExtractionConfig.dataSources key)
     */
    @JsonProperty("sourceApi")
    private String sourceApi;

    /**
     * Fallback API if primary fails
     */
    @JsonProperty("fallbackApi")
    private String fallbackApi;

    /**
     * JSONPath expression to extract value from API response
     * Example: "$.account.disclosureCode"
     */
    @JsonProperty("extractionPath")
    private String extractionPath;

    /**
     * Input fields required to call the API
     * Example: ["accountId"], ["customerId"]
     */
    @JsonProperty("requiredInputs")
    private List<String> requiredInputs;

    /**
     * Field data type: string, integer, decimal, date, boolean
     */
    @JsonProperty("fieldType")
    private String fieldType;

    /**
     * Default value if extraction fails
     */
    @JsonProperty("defaultValue")
    private Object defaultValue;

    /**
     * Regex pattern for validation
     */
    @JsonProperty("validationPattern")
    private String validationPattern;
}
