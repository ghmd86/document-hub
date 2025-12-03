package com.documenthub.model.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Execution strategy for API calls
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStrategy {

    /**
     * Execution mode: "parallel" or "sequential"
     */
    @JsonProperty("mode")
    private String mode;

    /**
     * Continue execution if one API fails
     */
    @JsonProperty("continueOnError")
    private Boolean continueOnError;

    /**
     * Overall execution timeout in milliseconds
     */
    @JsonProperty("timeout")
    private Integer timeout;
}
