package com.documenthub.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard error response structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @JsonProperty("errorMsg")
    private List<String> errorMsg;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("timestamp")
    private String timestamp;
}
