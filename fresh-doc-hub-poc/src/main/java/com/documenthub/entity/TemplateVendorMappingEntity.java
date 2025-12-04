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
 * Entity representing template vendor mapping
 * Maps to document_hub.template_vendor_mapping table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("template_vendor_mapping")
public class TemplateVendorMappingEntity {

    @Id
    @Column("template_vendor_id")
    private UUID templateVendorId;

    @Column("master_template_id")
    private UUID masterTemplateId;

    @Column("template_version")
    private Integer templateVersion;

    @Column("vendor")
    private String vendor;

    @Column("vendor_template_key")
    private String vendorTemplateKey;

    @Column("vendor_template_name")
    private String vendorTemplateName;

    @Column("reference_key_type")
    private String referenceKeyType;

    @Column("consumer_id")
    private UUID consumerId;

    @Column("template_content")
    private byte[] templateContent;

    @Column("start_date")
    private Long startDate;

    @Column("end_date")
    private Long endDate;

    @Column("vendor_mapping_version")
    private Integer vendorMappingVersion;

    @Column("primary_flag")
    private Boolean primaryFlag;

    @Column("active_flag")
    private Boolean activeFlag;

    @Column("template_status")
    private String templateStatus;

    @Column("schema_info")
    private JsonNode schemaInfo;

    @Column("template_fields")
    private JsonNode templateFields;

    @Column("vendor_config")
    private JsonNode vendorConfig;

    @Column("api_config")
    private JsonNode apiConfig;

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
