package com.documenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for storage index.
 * Used by Processors and Services instead of entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageIndexDto {

    private UUID storageIndexId;
    private UUID masterTemplateId;
    private Integer templateVersion;
    private String templateType;
    private String storageVendor;
    private String referenceKey;
    private String referenceKeyType;
    private UUID accountKey;
    private UUID customerKey;
    private UUID storageDocumentKey;
    private String fileName;
    private Long docCreationDate;
    private Boolean accessibleFlag;

    // JSON field as String (parsed from io.r2dbc.postgresql.codec.Json)
    private String docMetadata;

    private Long startDate;
    private Long endDate;
    private Boolean sharedFlag;
    private UUID generationVendorId;
    private String createdBy;
    private LocalDateTime createdTimestamp;
    private String updatedBy;
    private LocalDateTime updatedTimestamp;
    private Boolean archiveIndicator;
    private LocalDateTime archiveTimestamp;
    private Long versionNumber;
    private String recordStatus;
}
