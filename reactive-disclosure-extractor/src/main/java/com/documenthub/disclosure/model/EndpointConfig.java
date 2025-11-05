package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Endpoint configuration for API calls
 */
@Data
public class EndpointConfig {

    @JsonProperty("url")
    private String url;

    @JsonProperty("method")
    private String method;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("queryParams")
    private Map<String, String> queryParams;

    @JsonProperty("timeout")
    private Integer timeout = 5000; // default 5 seconds

    @JsonProperty("retryPolicy")
    private RetryPolicy retryPolicy;
}
