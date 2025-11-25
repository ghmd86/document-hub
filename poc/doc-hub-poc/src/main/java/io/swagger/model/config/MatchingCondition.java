package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingCondition {
    private String type; // reference_key, metadata_field, date_range
    private String metadataKey;
    private String valueSource;
    private String operator; // EQUALS, IN, GREATER_THAN, LESS_THAN
    private Integer priority;
}
