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
 * Entity representing the template_vendor_mapping table.
 * Maps templates to vendor implementations with denormalized template data and field definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("template_vendor_mapping")
public class TemplateVendorMapping {

    // Primary Key
    @Id
    @Column("template_vendor_id")
    private UUID templateVendorId;

    // Template Reference (with version)
    @Column("template_id")
    private UUID templateId;

    @Column("template_version")
    private Integer templateVersion;

    // Denormalized Template Data
    @Column("template_name")
    private String templateName;

    @Column("doc_type")
    private String docType;

    @Column("category_code")
    private String categoryCode;

    // Vendor Information
    @Column("vendor")
    private String vendor;

    @Column("vendor_template_key")
    private String vendorTemplateKey;

    @Column("vendor_template_name")
    private String vendorTemplateName;

    @Column("api_endpoint")
    private String apiEndpoint;

    // Template Content Reference (NOT blob storage)
    @Column("template_content_uri")
    private String templateContentUri;

    @Column("template_content_hash")
    private String templateContentHash;

    // Version Management
    @Column("vendor_version")
    private String vendorVersion;

    @Column("is_active")
    private Boolean isActive;

    @Column("is_primary")
    private Boolean isPrimary;

    // Lifecycle
    @Column("effective_date")
    private OffsetDateTime effectiveDate;

    @Column("valid_until")
    private OffsetDateTime validUntil;

    // JSONB Fields
    @Column("schema_info")
    private JsonNode schemaInfo;

    @Column("template_fields")
    private JsonNode templateFields;

    @Column("vendor_config")
    private JsonNode vendorConfig;

    @Column("api_config")
    private JsonNode apiConfig;

    // Consumer Tracking (for Kafka consumers)
    @Column("consumer_id")
    private UUID consumerId;

    @Column("last_sync_at")
    private OffsetDateTime lastSyncAt;

    @Column("sync_status")
    private JsonNode syncStatus;

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
