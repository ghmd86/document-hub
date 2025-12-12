package com.documenthub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual rule for eligibility evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rule {

    private String field;      // Field to evaluate (e.g., "accountType", "region", "customerSegment")
    private String operator;   // "EQUALS", "IN", "NOT_IN", "GREATER_THAN", "LESS_THAN", etc.
    private Object value;      // Value to compare against (String, List, Number, etc.)
}
