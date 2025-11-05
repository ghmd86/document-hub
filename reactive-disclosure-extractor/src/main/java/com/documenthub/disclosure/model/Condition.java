package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Condition for conditional execution
 */
@Data
public class Condition {

    @JsonProperty("field")
    private String field;

    @JsonProperty("operator")
    private String operator; // notNull, equals, greaterThan, etc.

    @JsonProperty("value")
    private Object value;
}
