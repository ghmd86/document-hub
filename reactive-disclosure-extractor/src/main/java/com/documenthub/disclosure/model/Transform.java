package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Transform configuration for extracted data
 */
@Data
public class Transform {

    @JsonProperty("type")
    private String type; // selectFirst, uppercase, lowercase, trim

    @JsonProperty("fallback")
    private Object fallback;

    @JsonProperty("source")
    private String source;

    @JsonProperty("operation")
    private String operation;
}
