package com.documenthub.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for document upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    @NotBlank(message = "Template type is required")
    private String templateType;

    @NotNull(message = "Template version is required")
    private Integer templateVersion;

    @NotBlank(message = "File name is required")
    private String fileName;

    private String displayName;

    private UUID accountId;

    private UUID customerId;

    private String referenceKey;

    private String referenceKeyType;

    private Boolean sharedFlag;

    private Long startDate;

    private Long endDate;

    private Map<String, Object> metadata;

    private List<DocumentAttribute> attributes;

    private List<String> tags;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentAttribute {
        private String name;
        private String value;
    }
}
