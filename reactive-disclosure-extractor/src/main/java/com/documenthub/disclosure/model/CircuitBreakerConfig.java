package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Circuit breaker configuration
 */
@Data
public class CircuitBreakerConfig {

    @JsonProperty("enabled")
    private Boolean enabled = false;

    @JsonProperty("failureThreshold")
    private Integer failureThreshold = 5;

    @JsonProperty("resetTimeoutMs")
    private Long resetTimeoutMs = 60000L;

    @JsonProperty("halfOpenRequests")
    private Integer halfOpenRequests = 3;
}
