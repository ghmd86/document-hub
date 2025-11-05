package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Response mapping configuration for data extraction
 */
@Data
public class ResponseMapping {

    @JsonProperty("extract")
    private Map<String, String> extract; // field -> JSONPath expression

    @JsonProperty("transform")
    private Map<String, Transform> transform;

    @JsonProperty("validate")
    private Map<String, Validation> validate;

    @JsonProperty("returnFields")
    private java.util.List<String> returnFields;
}
