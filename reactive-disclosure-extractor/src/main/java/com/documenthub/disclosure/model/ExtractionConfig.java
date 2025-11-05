package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Root configuration model for disclosure extraction
 */
@Data
public class ExtractionConfig {

    @JsonProperty("extractionStrategy")
    private List<DataSource> extractionStrategy;

    @JsonProperty("executionRules")
    private ExecutionRules executionRules;

    @JsonProperty("outputSchema")
    private Map<String, Object> outputSchema;
}
