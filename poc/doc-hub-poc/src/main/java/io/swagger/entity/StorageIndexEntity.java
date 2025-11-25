package io.swagger.entity;

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

@Table("storage_index")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column("reference_key")
    private String referenceKey;

    @Column("reference_key_type")
    private String referenceKeyType;

    @Column("is_shared")
    private Boolean isShared;

    @Column("account_key")
    private UUID accountKey;

    @Column("customer_key")
    private UUID customerKey;

    @Column("storage_vendor")
    private String storageVendor;

    @Column("storage_document_key")
    private UUID storageDocumentKey;

    @Column("generation_vendor")
    private String generationVendor;

    @Column("file_name")
    private String fileName;

    @Column("doc_creation_date")
    private Long docCreationDate;

    @Column("is_accessible")
    private Integer isAccessible;

    @Column("doc_metadata")
    private JsonNode docMetadata;

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

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private Long recordStatus;
}
