package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Error action configuration
 */
@Data
public class ErrorAction {

    @JsonProperty("action")
    private String action; // fail, return-default, retry, log-and-fail

    @JsonProperty("defaultValue")
    private Map<String, Object> defaultValue;

    @JsonProperty("message")
    private String message;

    @JsonProperty("severity")
    private String severity; // ERROR, WARN, INFO
}
