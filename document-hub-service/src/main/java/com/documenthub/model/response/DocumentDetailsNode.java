package com.documenthub.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Document details node in the response.
 * Represents individual document metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailsNode {

    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("sizeInMb")
    private Long sizeInMb;

    @JsonProperty("languageCode")
    private String languageCode;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("description")
    private String description;

    @JsonProperty("lineOfBusiness")
    private List<String> lineOfBusiness;

    @JsonProperty("category")
    private String category;

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("datePosted")
    private Long datePosted;

    @JsonProperty("lastDownloaded")
    private Long lastDownloaded;

    @JsonProperty("lastClientDownload")
    private String lastClientDownload;

    @JsonProperty("metadata")
    private List<MetadataNode> metadata;

    @JsonProperty("_links")
    private DocumentLinks links;

    /**
     * Document-specific links (download, delete).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentLinks {

        @JsonProperty("download")
        private LinkDetail download;

        @JsonProperty("delete")
        private LinkDetail delete;
    }

    /**
     * Link detail with href, type, rel.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkDetail {

        @JsonProperty("href")
        private String href;

        @JsonProperty("type")
        private String type;

        @JsonProperty("rel")
        private String rel;

        @JsonProperty("title")
        private String title;

        @JsonProperty("responseTypes")
        private List<String> responseTypes;
    }

    /**
     * Metadata key-value pair.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataNode {

        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;

        @JsonProperty("dataType")
        private String dataType;
    }
}
