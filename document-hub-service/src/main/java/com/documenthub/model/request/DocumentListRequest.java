package com.documenthub.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for document list enquiry.
 * Based on the OpenAPI schema definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListRequest {

    @JsonProperty("customerId")
    private UUID customerId;

    @JsonProperty("accountId")
    private List<UUID> accountId;

    @JsonProperty("documentTypeCategoryGroup")
    @Valid
    private List<DocumentCategoryGroup> documentTypeCategoryGroup;

    @JsonProperty("postedFromDate")
    private Long postedFromDate;

    @JsonProperty("postedToDate")
    private Long postedToDate;

    @JsonProperty("pageNumber")
    @Min(1)
    private Integer pageNumber;

    @JsonProperty("pageSize")
    @Min(1)
    private Integer pageSize;

    @JsonProperty("sortOrder")
    @Valid
    private List<SortOrder> sortOrder;

    /**
     * Nested class for document category grouping.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentCategoryGroup {

        @JsonProperty("category")
        private String category;

        @JsonProperty("documentTypes")
        private List<String> documentTypes;
    }

    /**
     * Nested class for sort order.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortOrder {

        @JsonProperty("orderBy")
        private String orderBy;

        @JsonProperty("sortBy")
        private String sortBy; // asc or desc
    }
}
