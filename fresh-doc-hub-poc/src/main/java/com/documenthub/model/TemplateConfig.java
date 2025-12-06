package com.documenthub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Template configuration including policies and eligibility criteria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateConfig {

    @JsonProperty("reprint_policy")
    private ReprintPolicy reprintPolicy;

    @JsonProperty("print_policy")
    private PrintPolicy printPolicy;

    @JsonProperty("eligibility_criteria")
    private EligibilityCriteria eligibilityCriteria;

    /**
     * Reprint policy configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReprintPolicy {
        @JsonProperty("cooldown_period_days")
        private Integer cooldownPeriodDays;
    }

    /**
     * Print policy configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrintPolicy {
        @JsonProperty("default")
        private String defaultValue;

        private Boolean duplex;

        private Map<String, Object> additionalConfig;
    }
}
