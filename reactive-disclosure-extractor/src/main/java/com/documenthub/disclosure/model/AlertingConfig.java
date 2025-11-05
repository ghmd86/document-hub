package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Alerting configuration
 */
@Data
public class AlertingConfig {

    @JsonProperty("slowRequestThresholdMs")
    private Integer slowRequestThresholdMs = 3000;

    @JsonProperty("errorRateThreshold")
    private Double errorRateThreshold = 0.05;
}
