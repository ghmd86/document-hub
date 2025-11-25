package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRules {
    private String startFrom;
    private String executionMode; // sequential or parallel
    private Boolean stopOnError;
    private ErrorHandlingConfig errorHandling;
}
