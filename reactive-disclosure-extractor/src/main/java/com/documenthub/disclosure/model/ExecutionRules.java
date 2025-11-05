package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Execution rules configuration
 */
@Data
public class ExecutionRules {

    @JsonProperty("startFrom")
    private String startFrom;

    @JsonProperty("executionMode")
    private String executionMode = "sequential";

    @JsonProperty("stopOnError")
    private Boolean stopOnError = true;

    @JsonProperty("errorHandling")
    private ExecutionErrorHandling errorHandling;

    @JsonProperty("monitoring")
    private MonitoringConfig monitoring;

    @JsonProperty("circuitBreaker")
    private CircuitBreakerConfig circuitBreaker;
}
