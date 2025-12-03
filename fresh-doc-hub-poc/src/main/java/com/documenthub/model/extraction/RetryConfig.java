package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retry configuration for failed API calls
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {

    /**
     * Maximum retry attempts
     */
    @JsonProperty("maxAttempts")
    private Integer maxAttempts;

    /**
     * Delay between retries in milliseconds
     */
    @JsonProperty("delayMs")
    private Integer delayMs;
}
