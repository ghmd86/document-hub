package com.documenthub.rules.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single criteria rule for eligibility evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaRule {

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("values")
    private List<Object> values;

    @JsonProperty("minValue")
    private Object minValue;

    @JsonProperty("maxValue")
    private Object maxValue;

    @JsonProperty("dataType")
    private String dataType; // string, number, boolean, date, duration, array, object

    @JsonProperty("unit")
    private String unit; // For duration type: days, months, years

    @JsonProperty("compareField")
    private String compareField; // Field to compare against for duration calculations

    /**
     * Get the operator as enum.
     */
    public OperatorType getOperatorType() {
        return OperatorType.fromValue(operator);
    }
}
