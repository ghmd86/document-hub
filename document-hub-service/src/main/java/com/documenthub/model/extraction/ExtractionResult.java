package com.documenthub.model.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result of extraction execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    private Boolean shouldInclude;
    private Map<String, Object> extractedVariables;
    private MatchingCriteria matchingCriteria;
    private RuleEvaluation ruleEvaluation;
    private ExecutionMetrics executionMetrics;
    private String failureReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchingCriteria {
        private String matchBy;
        private String referenceKeyType;
        private String referenceKeyValue;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleEvaluation {
        private Boolean result;
        private List<ConditionResult> matchedConditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionResult {
        private String field;
        private String operator;
        private Object value;
        private Boolean result;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMetrics {
        private Integer totalApiCalls;
        private Integer cacheHits;
        private Long executionTimeMs;
        private List<String> dataSourcesExecuted;
    }
}
