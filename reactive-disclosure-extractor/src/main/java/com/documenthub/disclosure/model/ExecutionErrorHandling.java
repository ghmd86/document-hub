package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Execution-level error handling
 */
@Data
public class ExecutionErrorHandling {

    @JsonProperty("strategy")
    private String strategy = "fail-fast"; // fail-fast or continue-on-error

    @JsonProperty("defaultResponse")
    private Map<String, Object> defaultResponse;
}
