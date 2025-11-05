package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Metrics configuration
 */
@Data
public class MetricsConfig {

    @JsonProperty("latency")
    private Boolean latency = true;

    @JsonProperty("cacheHitRate")
    private Boolean cacheHitRate = true;

    @JsonProperty("errorRate")
    private Boolean errorRate = true;

    @JsonProperty("extractionSuccess")
    private Boolean extractionSuccess = true;
}
