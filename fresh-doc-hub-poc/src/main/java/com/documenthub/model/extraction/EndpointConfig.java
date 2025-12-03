package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * API endpoint configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointConfig {

    /**
     * API URL with placeholders
     * Example: "https://api.example.com/accounts/${accountId}/details"
     */
    @JsonProperty("url")
    private String url;

    /**
     * HTTP method: GET, POST, PUT, DELETE
     */
    @JsonProperty("method")
    private String method;

    /**
     * Timeout in milliseconds
     */
    @JsonProperty("timeout")
    private Integer timeout;

    /**
     * HTTP headers with placeholder support
     * Example: {"Authorization": "Bearer ${auth.token}"}
     */
    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * Request body template for POST/PUT (with placeholders)
     */
    @JsonProperty("body")
    private String body;
}
