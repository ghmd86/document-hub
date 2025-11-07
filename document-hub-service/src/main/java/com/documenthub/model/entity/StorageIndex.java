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
 * Entity representing the storage_index table.
 * Index for stored documents with denormalized template data for zero-join queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("storage_index")
public class StorageIndex {

    // Primary Key
    @Id
    @Column("storage_index_id")
    private UUID storageIndexId;

    // Template Reference (with version)
    @Column("template_id")
    private UUID templateId;

    @Column("template_version")
    private Integer templateVersion;

    // Denormalized Template Data (for zero-join queries)
    @Column("template_name")
    private String templateName;

    @Column("category_code")
    private String categoryCode;

    @Column("category_name")
    private String categoryName;

    @Column("doc_type")
    private String docType;

    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("language_code")
    private String languageCode;

    @Column("is_regulatory")
    private Boolean isRegulatory;

    // Storage Information
    @Column("storage_vendor")
    private String storageVendor;

    @Column("storage_document_key")
    private UUID storageDocumentKey;

    @Column("file_name")
    private String fileName;

    @Column("file_size_bytes")
    private Long fileSizeBytes;

    @Column("mime_type")
    private String mimeType;

    @Column("file_hash")
    private String fileHash;

    // Reference Keys (flexible key system)
    @Column("reference_key")
    private String referenceKey;

    @Column("reference_key_type")
    private String referenceKeyType;

    // Customer/Account Information
    @Column("account_id")
    private UUID accountId;

    @Column("customer_id")
    private UUID customerId;

    // Document Lifecycle
    @Column("doc_creation_date")
    private OffsetDateTime docCreationDate;

    @Column("is_accessible")
    private Boolean isAccessible;

    @Column("last_accessed_at")
    private OffsetDateTime lastAccessedAt;

    @Column("access_count")
    private Integer accessCount;

    // JSONB Fields (Document-Specific Data)
    @Column("doc_metadata")
    private JsonNode docMetadata;

    @Column("access_control")
    private JsonNode accessControl;

    @Column("compliance_tags")
    private JsonNode complianceTags;

    // Retention and Compliance
    @Column("retention_until")
    private OffsetDateTime retentionUntil;

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
