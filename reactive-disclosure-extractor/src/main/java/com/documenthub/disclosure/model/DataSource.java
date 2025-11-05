package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Data source configuration for API calls
 */
@Data
public class DataSource {

    @JsonProperty("id")
    private String id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("endpoint")
    private EndpointConfig endpoint;

    @JsonProperty("cache")
    private CacheConfig cache;

    @JsonProperty("responseMapping")
    private ResponseMapping responseMapping;

    @JsonProperty("errorHandling")
    private ErrorHandling errorHandling;

    @JsonProperty("nextCalls")
    private List<NextCall> nextCalls;

    @JsonProperty("storeAs")
    private String storeAs;
}
