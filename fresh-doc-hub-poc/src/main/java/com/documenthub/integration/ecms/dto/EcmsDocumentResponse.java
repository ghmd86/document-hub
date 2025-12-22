package com.documenthub.integration.ecms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response from ECMS API when creating/retrieving a document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcmsDocumentResponse {

    private UUID id;

    private String name;

    private String fileName;

    private FileSize fileSize;

    private UUID folderId;

    private String path;

    private String link;

    private String activeStartDate;

    private String activeEndDate;

    private Boolean isSearchable;

    private Boolean isArchived;

    private String scope;

    private List<AttributeWithId> attributes;

    private List<TagWithId> tags;

    private AuditInfo auditInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileSize {
        private Integer value;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttributeWithId {
        private UUID id;
        private String name;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagWithId {
        private UUID id;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuditInfo {
        private UUID createdBy;
        private String createdByUserIdpProvider;
        private String createdDateTime;
        private UUID lastUpdatedBy;
        private String lastUpdatedByUserIdpProvider;
        private String lastUpdatedDateTime;
    }
}
