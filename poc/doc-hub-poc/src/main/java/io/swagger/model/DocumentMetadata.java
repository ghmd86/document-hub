package io.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {

    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("templateType")
    private String templateType;

    @JsonProperty("templateVersion")
    private Integer templateVersion;

    @JsonProperty("documentName")
    private String documentName;

    @JsonProperty("documentDescription")
    private String documentDescription;

    @JsonProperty("storageLocation")
    private String storageLocation;

    @JsonProperty("fileFormat")
    private String fileFormat;

    @JsonProperty("createdDate")
    private Long createdDate;

    @JsonProperty("lastModifiedDate")
    private Long lastModifiedDate;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
