package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Root configuration model for dynamic extraction logic
 * Parsed from master_template_definition.data_extraction_schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionConfig {

    private DocumentMatchingStrategy documentMatchingStrategy;

    private List<DataSourceConfig> extractionStrategy;

    private InclusionRules inclusionRules;

    private OutputMapping outputMapping;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentMatchingStrategy {
        private String matchBy; // reference_key, metadata, template_only
        private String referenceKeyType; // DISCLOSURE_CODE, LOAN_OFFER_CODE, etc.
        private Map<String, String> metadataFields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceConfig {
        private String id;
        private String description;
        private EndpointConfig endpoint;
        private CacheConfig cache;
        private ResponseMapping responseMapping;
        private List<NextCall> nextCalls;
        private List<String> dependencies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointConfig {
        private String url;
        private String method;
        private Map<String, String> headers;
        private Object body;
        private Integer timeout;
        private RetryPolicy retryPolicy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryPolicy {
        private Integer maxAttempts;
        private String backoffStrategy; // exponential, linear, fixed
        private Integer initialDelayMs;
        private Integer maxDelayMs;
        private List<Integer> retryOn; // HTTP status codes
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheConfig {
        private Boolean enabled;
        private Integer ttl;
        private String keyPattern;
        private List<String> invalidateOn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMapping {
        private Map<String, String> extract; // fieldName -> JSONPath expression
        private Map<String, TransformConfig> transform;
        private Map<String, ValidationRule> validate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransformConfig {
        private String type; // calculateAge, classification, uppercase, etc.
        private String sourceField;
        private Object config; // Transform-specific configuration
        private List<TierMapping> classifications;
        private List<TierMapping> tiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierMapping {
        private Object min;
        private Object max;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRule {
        private String type; // string, integer, number, date, etc.
        private Boolean required;
        private String pattern;
        private Object min;
        private Object max;
        private List<String> enumValues;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextCall {
        private Condition condition;
        private String dependsOn;
        private String targetDataSource;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String field;
        private String operator; // notNull, equals, greaterThan, etc.
        private Object value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InclusionRules {
        private String operator; // AND, OR, NOT
        private List<RuleCondition> conditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleCondition {
        private String field;
        private String operator; // ==, !=, >, >=, <, <=, in, notIn, exists, matches, between
        private Object value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputMapping {
        private String documentReferenceKey;
        private Map<String, String> documentMetadata;
    }
}
