package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Validation configuration for extracted data
 */
@Data
public class Validation {

    @JsonProperty("type")
    private String type; // string, number, array, date

    @JsonProperty("required")
    private Boolean required = false;

    @JsonProperty("pattern")
    private String pattern; // regex pattern

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("validateInPast")
    private Boolean validateInPast;

    @JsonProperty("validateInFuture")
    private Boolean validateInFuture;
}
