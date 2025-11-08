package com.documenthub.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for POST /documents-enquiry endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentEnquiryResponse {

    private List<DocumentDetailsNode> documentList;
    private PaginationResponse pagination;
    private DocumentSummary summary;
    private ResponseLinks _links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentDetailsNode {
        private String documentId;
        private Integer sizeInMb;
        private String languageCode;
        private String displayName;
        private String mimeType;
        private String description;
        private List<String> lineOfBusiness;
        private String category;
        private String documentType;
        private Long datePosted;
        private Long lastDownloaded;
        private Boolean isShared;
        private String sharingScope;
        private List<MetadataNode> metadata;
        private ExtractionMetadata extractionMetadata;
        private DocumentLinks _links;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataNode {
        private String key;
        private String value;
        private String dataType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExtractionMetadata {
        private Map<String, Object> extractedVariables;
        private RuleEvaluation ruleEvaluation;
        private MatchingCriteria matchingCriteria;
        private ExecutionMetrics executionMetrics;
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
    public static class ExecutionMetrics {
        private Integer totalApiCalls;
        private Integer cacheHits;
        private Long executionTimeMs;
        private List<String> dataSourcesExecuted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentLinks {
        private Link download;
        private Link delete;
        private Link metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String href;
        private String type;
        private String rel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationResponse {
        private Integer pageSize;
        private Long totalItems;
        private Integer totalPages;
        private Integer pageNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSummary {
        private Long totalDocuments;
        private Long accountSpecificDocuments;
        private Long sharedDocuments;
        private Integer customRuleTemplatesEvaluated;
        private Integer customRuleTemplatesIncluded;
        private Map<String, Integer> sharedBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseLinks {
        private Link self;
        private Link next;
        private Link prev;
    }
}
