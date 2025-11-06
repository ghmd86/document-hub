package com.documenthub.rules.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents the complete extraction rule configuration for custom rules.
 * Mirrors the structure from extractor_logic.json
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRule {

    @JsonProperty("ruleType")
    private String ruleType;

    @JsonProperty("extractionStrategy")
    private List<DataSource> extractionStrategy;

    @JsonProperty("eligibilityCriteria")
    private Map<String, CriteriaRule> eligibilityCriteria;

    @JsonProperty("errorHandling")
    private ErrorHandlingConfig errorHandling;

    @JsonProperty("logicOperator")
    private String logicOperator; // For composite rules: AND, OR

    @JsonProperty("rules")
    private List<ExtractionRule> rules; // For composite rules

    /**
     * Get rule type as enum.
     */
    public RuleType getRuleTypeEnum() {
        return RuleType.fromValue(ruleType);
    }

    /**
     * Data source configuration for API calls.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSource {

        @JsonProperty("id")
        private String id;

        @JsonProperty("description")
        private String description;

        @JsonProperty("endpoint")
        private EndpointConfig endpoint;

        @JsonProperty("cache")
        private CacheConfig cache;

        @JsonProperty("responseMapping")
        private ResponseMapping responseMapping;
    }

    /**
     * Endpoint configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointConfig {

        @JsonProperty("url")
        private String url;

        @JsonProperty("method")
        private String method;

        @JsonProperty("headers")
        private Map<String, String> headers;

        @JsonProperty("queryParams")
        private Map<String, String> queryParams;

        @JsonProperty("timeout")
        private Integer timeout;
    }

    /**
     * Cache configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheConfig {

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("ttl")
        private Integer ttl;

        @JsonProperty("keyPattern")
        private String keyPattern;
    }

    /**
     * Response mapping configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMapping {

        @JsonProperty("extract")
        private Map<String, String> extract; // Field name -> JSONPath expression
    }

    /**
     * Error handling configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorHandlingConfig {

        @JsonProperty("onExtractionFailure")
        private String onExtractionFailure; // exclude, include, use_cache

        @JsonProperty("onTimeout")
        private String onTimeout;

        @JsonProperty("onValidationError")
        private String onValidationError;

        @JsonProperty("fallbackBehavior")
        private String fallbackBehavior;

        @JsonProperty("maxRetries")
        private Integer maxRetries;

        @JsonProperty("timeoutMs")
        private Integer timeoutMs;
    }
}
