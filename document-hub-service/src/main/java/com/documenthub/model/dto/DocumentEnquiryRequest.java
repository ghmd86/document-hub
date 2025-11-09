package com.documenthub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for POST /documents-enquiry endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEnquiryRequest {

    @NotNull(message = "customerId is required")
    private UUID customerId;

    @NotEmpty(message = "At least one accountId is required")
    private List<UUID> accountId;

    private List<DocumentCategoryGroup> documentTypeCategoryGroup;

    private Long postedFromDate; // Epoch timestamp

    private Long postedToDate; // Epoch timestamp

    @Builder.Default
    private Integer pageNumber = 0;

    @Builder.Default
    @Positive(message = "pageSize must be positive")
    private Integer pageSize = 20;

    private List<SortOrder> sortOrder;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentCategoryGroup {
        private String category;
        private List<String> documentTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortOrder {
        private String orderBy;
        private String sortBy; // asc or desc
    }
}
