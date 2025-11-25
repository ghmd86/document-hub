package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorHandlingConfig {
    private String strategy; // fail-fast, skip, use-default, retry
    private Map<String, ErrorAction> on404;
    private Map<String, ErrorAction> on5xx;
    private Map<String, ErrorAction> onValidationError;
    private Map<String, Object> defaultResponse;
}
