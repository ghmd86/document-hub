package com.documenthub.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for document retrieval.
 * Based on the OpenAPI schema definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRetrievalResponse {

    @JsonProperty("documentList")
    private List<DocumentDetailsNode> documentList;

    @JsonProperty("pagination")
    private PaginationResponse pagination;

    @JsonProperty("_links")
    private Links links;

    /**
     * HATEOAS links for pagination.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Links {

        @JsonProperty("self")
        private Link self;

        @JsonProperty("next")
        private Link next;

        @JsonProperty("prev")
        private Link prev;
    }

    /**
     * Individual link object.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {

        @JsonProperty("href")
        private String href;

        @JsonProperty("type")
        private String type;

        @JsonProperty("rel")
        private String rel;

        @JsonProperty("responseTypes")
        private List<String> responseTypes;
    }
}
