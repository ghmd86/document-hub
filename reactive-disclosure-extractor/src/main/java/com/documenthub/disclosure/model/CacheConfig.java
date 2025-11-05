package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Cache configuration
 */
@Data
public class CacheConfig {

    @JsonProperty("enabled")
    private Boolean enabled = false;

    @JsonProperty("ttl")
    private Integer ttl = 3600; // seconds

    @JsonProperty("keyPattern")
    private String keyPattern;

    @JsonProperty("invalidateOn")
    private List<String> invalidateOn;
}
