package com.documenthub.disclosure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Configuration for next call in the chain
 */
@Data
public class NextCall {

    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("dependsOn")
    private String dependsOn;

    @JsonProperty("targetDataSource")
    private String targetDataSource;
}
