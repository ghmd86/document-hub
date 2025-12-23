package com.documenthub.dto;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for document query parameters.
 * Used by DocumentMatchingService for querying documents.
 */
@Data
@Builder
public class DocumentQueryParams {
    private MasterTemplateDefinitionEntity template;
    private UUID accountId;
    private Map<String, Object> extractedData;
    private Long postedFromDate;
    private Long postedToDate;
}
