package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Caching configuration for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheConfig {

    /**
     * Enable caching
     */
    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * Cache TTL in seconds
     */
    @JsonProperty("ttlSeconds")
    private Integer ttlSeconds;

    /**
     * Cache key pattern with placeholders
     * Example: "account:${accountId}:details"
     */
    @JsonProperty("keyPattern")
    private String keyPattern;
}
