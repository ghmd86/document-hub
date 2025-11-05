package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Monitoring configuration
 */
@Data
public class MonitoringConfig {

    @JsonProperty("logLevel")
    private String logLevel = "INFO";

    @JsonProperty("trackMetrics")
    private Boolean trackMetrics = true;

    @JsonProperty("metrics")
    private MetricsConfig metrics;

    @JsonProperty("alerting")
    private AlertingConfig alerting;
}
