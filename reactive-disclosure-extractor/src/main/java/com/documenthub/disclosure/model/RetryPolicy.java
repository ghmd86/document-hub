package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Retry policy configuration
 */
@Data
public class RetryPolicy {

    @JsonProperty("maxAttempts")
    private Integer maxAttempts = 3;

    @JsonProperty("backoffStrategy")
    private String backoffStrategy = "exponential"; // linear or exponential

    @JsonProperty("initialDelayMs")
    private Integer initialDelayMs = 100;

    @JsonProperty("maxDelayMs")
    private Integer maxDelayMs = 2000;

    @JsonProperty("retryOn")
    private List<Integer> retryOn; // HTTP status codes to retry on
}
