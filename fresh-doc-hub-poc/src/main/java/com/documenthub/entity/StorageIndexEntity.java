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
 * Entity representing document storage index
 * Maps to document_hub.storage_index table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("document_hub.storage_index")
public class StorageIndexEntity {

    @Id
    @Column("storage_index_id")
    private UUID storageIndexId;

    @Column("master_template_id")
    private UUID masterTemplateId;

    @Column("template_version")
    private Integer templateVersion;

    @Column("template_type")
    private String templateType;

    @Column("storage_vendor")
    private String storageVendor;

    @Column("reference_key")
    private String referenceKey;

    @Column("reference_key_type")
    private String referenceKeyType;

    @Column("account_key")
    private UUID accountKey;

    @Column("customer_key")
    private UUID customerKey;

    @Column("storage_document_key")
    private UUID storageDocumentKey;

    @Column("file_name")
    private String fileName;

    @Column("doc_creation_date")
    private Long docCreationDate;

    @Column("accessible_flag")
    private Boolean accessibleFlag;

    @Column("doc_metadata")
    private Json docMetadata;

    @Column("valid_from")
    private Long validFrom;

    @Column("valid_until")
    private Long validUntil;

    @Column("shared_flag")
    private Boolean sharedFlag;

    @Column("generation_vendor_id")
    private UUID generationVendorId;

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
