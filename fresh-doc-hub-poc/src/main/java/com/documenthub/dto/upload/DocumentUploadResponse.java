package com.documenthub.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for document upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private UUID storageIndexId;

    private UUID ecmsDocumentId;

    private String fileName;

    private String displayName;

    private String templateType;

    private Integer templateVersion;

    private UUID accountId;

    private UUID customerId;

    private String referenceKey;

    private String referenceKeyType;

    private String documentLink;

    private FileSize fileSize;

    private LocalDateTime createdAt;

    private String status;

    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSize {
        private Integer value;
        private String unit;
    }
}
