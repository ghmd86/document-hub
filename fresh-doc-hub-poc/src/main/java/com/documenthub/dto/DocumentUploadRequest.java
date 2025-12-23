package com.documenthub.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * DTO for document upload request parameters.
 * Consolidates all upload-related parameters into a single object.
 */
@Data
@Builder
public class DocumentUploadRequest {
    private MultipartFile content;
    private String documentType;
    private String createdBy;
    private String metadataJson;
    private UUID templateId;
    private String referenceKey;
    private String referenceKeyType;
    private UUID accountKey;
    private UUID customerKey;
    private String category;
    private String fileName;
    private Long activeStartDate;
    private Long activeEndDate;
    private UUID threadId;
    private UUID correlationId;
}
