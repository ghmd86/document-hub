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
public class ResponseMapping {
    private Map<String, String> extract; // Field name -> JSONPath expression
    private Map<String, ValidationRule> validate;
    private Map<String, TransformConfig> transform;
}
