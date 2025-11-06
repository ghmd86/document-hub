package com.documenthub.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing the master_template_definition table.
 * Stores master metadata and configuration details of each document template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("master_template_definition")
public class MasterTemplateDefinition {

    @Id
    @Column("template_id")
    private UUID templateId;

    @Column("version")
    private Integer version;

    @Column("legacy_template_id")
    private String legacyTemplateId;

    @Column("template_name")
    private String templateName;

    @Column("description")
    private String description;

    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("category")
    private String category;

    @Column("doc_type")
    private String docType;

    @Column("language_code")
    private String languageCode;

    @Column("owning_dept")
    private String owningDept;

    @Column("notification_needed")
    private Boolean notificationNeeded;

    @Column("doc_supporting_data")
    private String docSupportingData;

    @Column("is_regulatory")
    private Boolean isRegulatory;

    @Column("is_message_center_doc")
    private Boolean isMessageCenterDoc;

    @Column("document_channel")
    private String documentChannel;

    @Column("template_variables")
    private String templateVariables;

    @Column("template_status")
    private String templateStatus;

    @Column("effective_date")
    private Long effectiveDate;

    @Column("valid_until")
    private Long validUntil;

    // Sharing configuration
    @Column("is_shared_document")
    private Boolean isSharedDocument;

    @Column("sharing_scope")
    private String sharingScope;

    @Column("data_extraction_schema")
    private String dataExtractionSchema;

    // DBA Required columns
    @Column("created_by")
    private String createdBy;

    @Column("created_timestamp")
    private Instant createdTimestamp;

    @Column("update_by")
    private String updateBy;

    @Column("updated_timestamp")
    private Instant updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("archive_timestamp")
    private Instant archiveTimestamp;

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private Long recordStatus;
}
