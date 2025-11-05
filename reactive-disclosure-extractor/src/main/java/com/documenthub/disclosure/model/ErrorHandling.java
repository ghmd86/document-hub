package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Error handling configuration
 */
@Data
public class ErrorHandling {

    @JsonProperty("onValidationError")
    private ErrorAction onValidationError;

    @JsonProperty("on404")
    private ErrorAction on404;

    @JsonProperty("on5xx")
    private ErrorAction on5xx;
}
