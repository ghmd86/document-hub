package com.documenthub.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
    private JsonNode documentChannelOld;

    @Column("template_variables")
    private JsonNode templateVariables;

    @Column("data_extraction_config")
    private JsonNode dataExtractionConfig;

    @Column("access_control")
    private JsonNode accessControl;

    @Column("required_fields")
    private JsonNode requiredFields;

    @Column("template_config")
    private JsonNode templateConfig;

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
}
