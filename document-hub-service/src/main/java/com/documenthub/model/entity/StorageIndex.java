package com.documenthub.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Entity representing the storage_index table.
 * Stores actual document-related data and indexing information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("storage_index")
public class StorageIndex {

    @Id
    @Column("storage_index_id")
    private UUID storageIndexId;

    @Column("template_id")
    private UUID templateId;

    @Column("doc_type")
    private String docType;

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

    @Column("is_accessible")
    private Boolean isAccessible;

    @Column("last_referenced")
    private Long lastReferenced;

    @Column("time_referenced")
    private Integer timeReferenced;

    @Column("doc_info")
    private JsonNode docInfo;

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
