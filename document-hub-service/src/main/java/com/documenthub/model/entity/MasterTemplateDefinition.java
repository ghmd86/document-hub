package com.documenthub.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing the master_template_definition table.
 * Stores master metadata and configuration details of each document template with versioning support.
 * Uses composite primary key (template_id, version).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("master_template_definition")
public class MasterTemplateDefinition {

    // Composite Primary Key (template_id, version)
    @Id
    @Column("template_id")
    private UUID templateId;

    @Column("version")
    private Integer version;

    // Template Identification
    @Column("legacy_template_id")
    private String legacyTemplateId;

    @Column("template_name")
    private String templateName;

    @Column("description")
    private String description;

    // Business Classification (Denormalized - no category lookup table)
    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("category_code")
    private String categoryCode;

    @Column("category_name")
    private String categoryName;

    @Column("doc_type")
    private String docType;

    @Column("language_code")
    private String languageCode;

    @Column("owning_dept")
    private String owningDept;

    // Flags and Settings
    @Column("notification_needed")
    private Boolean notificationNeeded;

    @Column("is_regulatory")
    private Boolean isRegulatory;

    @Column("is_message_center_doc")
    private Boolean isMessageCenterDoc;

    @Column("is_shared_document")
    private Boolean isSharedDocument;

    @Column("sharing_scope")
    private String sharingScope;

    // Status and Lifecycle
    @Column("template_status")
    private String templateStatus;

    @Column("effective_date")
    private OffsetDateTime effectiveDate;

    @Column("valid_until")
    private OffsetDateTime validUntil;

    @Column("retention_days")
    private Integer retentionDays;

    // JSONB Fields (Optimized Structure)
    @Column("access_control")
    private JsonNode accessControl;

    @Column("required_fields")
    private JsonNode requiredFields;

    @Column("channels")
    private JsonNode channels;

    @Column("template_variables")
    private JsonNode templateVariables;

    @Column("data_extraction_config")
    private JsonNode dataExtractionConfig;

    /**
     * Operational configuration for the template including:
     * - defaultPrintVendor: Vendor to use for print delivery
     * - defaultEmailVendor: Vendor to use for email delivery
     * - printVendorFailover: Failover strategy when primary vendor is down
     * - uploadReferenceKeyField: Field to use for reference_key during upload
     */
    @Column("template_config")
    private JsonNode templateConfig;

    // Audit Fields (DBA Required)
    @Column("created_by")
    private String createdBy;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_by")
    private String updatedBy;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Column("archived_at")
    private OffsetDateTime archivedAt;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("version_number")
    private Long versionNumber;
}
