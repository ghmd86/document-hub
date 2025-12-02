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
 * Maps to master_template_definition table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("master_template_definition")
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

    @Column("display_name")
    private String displayName;

    @Column("template_name")
    private String templateName;

    @Column("template_description")
    private String templateDescription;

    @Column("template_category")
    private String templateCategory;

    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("template_type")
    private String templateType;

    @Column("language_code")
    private String languageCode;

    @Column("owning_dept")
    private String owningDept;

    @Column("notification_needed")
    private Boolean notificationNeeded;

    @Column("is_active")
    private Boolean isActive;

    @Column("is_regulatory")
    private Boolean isRegulatory;

    @Column("is_message_center_doc")
    private Boolean isMessageCenterDoc;

    @Column("is_shared_document")
    private Boolean isSharedDocument;

    @Column("sharing_scope")
    private String sharingScope;

    @Column("data_extraction_config")
    private JsonNode dataExtractionConfig;

    @Column("access_control")
    private JsonNode accessControl;

    @Column("channels")
    private JsonNode channels;

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

    @Column("update_by")
    private String updateBy;

    @Column("updated_timestamp")
    private LocalDateTime updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("archive_timestamp")
    private LocalDateTime archiveTimestamp;

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private Long recordStatus;
}
