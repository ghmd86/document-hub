package io.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListResponse {

    @JsonProperty("documents")
    private List<DocumentMetadata> documents;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("pageNumber")
    private Integer pageNumber;

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("_links")
    private Map<String, String> links;
}
