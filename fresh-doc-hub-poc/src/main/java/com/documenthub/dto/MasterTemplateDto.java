package com.documenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for master template definition.
 * Used by Processors and Services instead of entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterTemplateDto {

    private UUID masterTemplateId;
    private Integer templateVersion;
    private String legacyTemplateId;
    private String legacyTemplateName;
    private String templateName;
    private String templateDescription;
    private String lineOfBusiness;
    private String templateCategory;
    private String templateTypeOld;
    private String templateType;
    private String languageCode;
    private String owningDept;
    private Boolean notificationNeeded;
    private Boolean regulatoryFlag;
    private Boolean messageCenterDocFlag;
    private String displayName;
    private Boolean activeFlag;
    private Boolean sharedDocumentFlag;
    private String sharingScope;

    // JSON fields as String (parsed from io.r2dbc.postgresql.codec.Json)
    private String documentChannelOld;
    private String templateVariables;
    private String dataExtractionConfig;
    private String documentMatchingConfig;
    private String eligibilityCriteria;
    private String accessControl;
    private String requiredFields;
    private String templateConfig;

    private Long startDate;
    private Long endDate;
    private String createdBy;
    private LocalDateTime createdTimestamp;
    private String updatedBy;
    private LocalDateTime updatedTimestamp;
    private Boolean archiveIndicator;
    private LocalDateTime archiveTimestamp;
    private Long versionNumber;
    private String recordStatus;

    // P0 Fields
    private String communicationType;
    private String workflow;
    private Boolean singleDocumentFlag;
}
