package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExtractionConfig {
    private ExecutionRules executionRules;
    private List<DataSourceConfig> extractionStrategy;
    private DocumentMatchingStrategy documentMatchingStrategy;
}
