package com.documenthub.entity;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing master template definition
 * Maps to document_hub.master_template_definition table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("document_hub.master_template_definition")
public class MasterTemplateDefinitionEntity {

    @Id
    @Column("master_template_id")
    private UUID masterTemplateId;

    @Column("template_version")
    private Integer templateVersion;

    @Column("legacy_template_id")
    private String legacyTemplateId;

    @Column("legacy_template_name")
    private String legacyTemplateName;

    @Column("template_name")
    private String templateName;

    @Column("template_description")
    private String templateDescription;

    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("template_category")
    private String templateCategory;

    @Column("template_type_old")
    private String templateTypeOld;

    @Column("template_type")
    private String templateType;

    @Column("language_code")
    private String languageCode;

    @Column("owning_dept")
    private String owningDept;

    @Column("notification_needed")
    private Boolean notificationNeeded;

    @Column("regulatory_flag")
    private Boolean regulatoryFlag;

    @Column("message_center_doc_flag")
    private Boolean messageCenterDocFlag;

    @Column("display_name")
    private String displayName;

    @Column("active_flag")
    private Boolean activeFlag;

    @Column("shared_document_flag")
    private Boolean sharedDocumentFlag;

    @Column("sharing_scope")
    private String sharingScope;

    @Column("document_channel_old")
    private Json documentChannelOld;

    @Column("template_variables")
    private Json templateVariables;

    @Column("data_extraction_config")
    private Json dataExtractionConfig;

    @Column("access_control")
    private Json accessControl;

    @Column("required_fields")
    private Json requiredFields;

    @Column("template_config")
    private Json templateConfig;

    @Column("start_date")
    private Long startDate;

    @Column("end_date")
    private Long endDate;

    @Column("created_by")
    private String createdBy;

    @Column("created_timestamp")
    private LocalDateTime createdTimestamp;

    @Column("updated_by")
    private String updatedBy;

    @Column("updated_timestamp")
    private LocalDateTime updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("archive_timestamp")
    private LocalDateTime archiveTimestamp;

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private String recordStatus;

    // ====================================================================
    // P0 Fields - Added December 2024
    // ====================================================================

    /**
     * Communication channel for document delivery.
     * Values: LETTER, EMAIL, SMS, PUSH
     * Default: LETTER
     * Source: P0-002
     */
    @Column("communication_type")
    private String communicationType;

    /**
     * WCM workflow type for template approval.
     * Values: 2_EYES (single approver), 4_EYES (dual approver/maker-checker)
     * Source: P0-006
     */
    @Column("workflow")
    private String workflow;

    /**
     * Controls whether queries return single or multiple documents.
     * true (default): Return only the most recent document
     * false: Return all matching documents (e.g., statements)
     * Source: P0-009
     */
    @Column("single_document_flag")
    private Boolean singleDocumentFlag;
}
