package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for an API endpoint - defines HOW to call it
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceConfig {

    /**
     * Description of what this API provides
     */
    @JsonProperty("description")
    private String description;

    /**
     * API endpoint configuration
     */
    @JsonProperty("endpoint")
    private EndpointConfig endpoint;

    /**
     * Caching configuration
     */
    @JsonProperty("cache")
    private CacheConfig cache;

    /**
     * Retry configuration
     */
    @JsonProperty("retry")
    private RetryConfig retry;

    /**
     * List of fields this API provides
     */
    @JsonProperty("providesFields")
    private List<String> providesFields;
}
