package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule {
    private String type; // string, integer, date, boolean
    private Boolean required;
    private String pattern; // Regex pattern
}
