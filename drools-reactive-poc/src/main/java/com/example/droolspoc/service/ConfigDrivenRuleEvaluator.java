package com.example.droolspoc.service;

import com.example.droolspoc.config.RuleEngineConfiguration;
import com.example.droolspoc.config.RuleEngineConfiguration.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Configuration-Driven Rule Evaluator
 *
 * Evaluates rules based on YAML configuration:
 * - Supports all operators (EQUALS, GREATER_THAN, IN, MATCHES, etc.)
 * - Handles nested AND/OR logic
 * - Returns eligible documents
 *
 * NO CODE CHANGES needed to add new rules!
 */
@Service
public class ConfigDrivenRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ConfigDrivenRuleEvaluator.class);

    private final RuleEngineConfiguration config;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConfigDrivenRuleEvaluator(
        RuleEngineConfiguration config,
        ObjectMapper objectMapper
    ) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate all rules against data context
     *
     * @param dataContext Data from all external APIs (source → fields)
     * @return Set of eligible document IDs
     */
    public Set<String> evaluateRules(Map<String, Map<String, Object>> dataContext) {
        log.info("Evaluating {} rules", config.getRules().size());

        long startTime = System.currentTimeMillis();

        Set<String> eligibleDocuments = new HashSet<>();

        // Sort rules by priority (highest first)
        List<RuleConfig> sortedRules = config.getRules().stream()
            .filter(RuleConfig::getEnabled)
            .sorted(Comparator.comparing(RuleConfig::getPriority).reversed())
            .collect(Collectors.toList());

        // Evaluate each rule
        for (RuleConfig rule : sortedRules) {
            try {
                if (evaluateRule(rule, dataContext)) {
                    log.debug("[RULE FIRED] {}", rule.getName());

                    // Add eligible documents
                    if (rule.getActions() != null && rule.getActions().getAddDocuments() != null) {
                        for (DocumentActionConfig doc : rule.getActions().getAddDocuments()) {
                            eligibleDocuments.add(doc.getDocumentId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getRuleId(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Rule evaluation completed in {}ms: {} documents eligible",
            duration, eligibleDocuments.size());

        return eligibleDocuments;
    }

    /**
     * Evaluate a single rule
     */
    private boolean evaluateRule(RuleConfig rule, Map<String, Map<String, Object>> dataContext) {
        if (rule.getConditions() == null) {
            return false;
        }

        return evaluateConditionGroup(rule.getConditions(), dataContext);
    }

    /**
     * Evaluate condition group (handles nested AND/OR logic)
     */
    private boolean evaluateConditionGroup(
        ConditionGroupConfig group,
        Map<String, Map<String, Object>> dataContext
    ) {
        if (group.getExpressions() == null || group.getExpressions().isEmpty()) {
            return true;
        }

        boolean isAllType = "ALL".equalsIgnoreCase(group.getType());

        for (Object expression : group.getExpressions()) {
            boolean result;

            if (expression instanceof Map) {
                // Convert Map to ConditionConfig
                @SuppressWarnings("unchecked")
                Map<String, Object> exprMap = (Map<String, Object>) expression;

                // Check if it's a nested group (has "type" field)
                if (exprMap.containsKey("type")) {
                    ConditionGroupConfig nestedGroup = objectMapper.convertValue(
                        expression,
                        ConditionGroupConfig.class
                    );
                    result = evaluateConditionGroup(nestedGroup, dataContext);
                } else {
                    ConditionConfig condition = objectMapper.convertValue(
                        expression,
                        ConditionConfig.class
                    );
                    result = evaluateCondition(condition, dataContext);
                }
            } else if (expression instanceof ConditionGroupConfig) {
                result = evaluateConditionGroup((ConditionGroupConfig) expression, dataContext);
            } else if (expression instanceof ConditionConfig) {
                result = evaluateCondition((ConditionConfig) expression, dataContext);
            } else {
                log.warn("Unknown expression type: {}", expression.getClass());
                result = false;
            }

            // Short-circuit evaluation
            if (isAllType && !result) {
                return false;  // ALL (AND): one false → entire group false
            }
            if (!isAllType && result) {
                return true;   // ANY (OR): one true → entire group true
            }
        }

        // If we get here:
        // - ALL type: all conditions were true
        // - ANY type: all conditions were false
        return isAllType;
    }

    /**
     * Evaluate a single condition
     */
    private boolean evaluateCondition(
        ConditionConfig condition,
        Map<String, Map<String, Object>> dataContext
    ) {
        // Get actual value from data context
        Map<String, Object> sourceData = dataContext.get(condition.getSource());
        if (sourceData == null) {
            log.debug("Source {} not found in data context", condition.getSource());
            return false;
        }

        Object actualValue = sourceData.get(condition.getField());
        if (actualValue == null) {
            log.debug("Field {} not found in source {}", condition.getField(), condition.getSource());
            return false;
        }

        Object expectedValue = condition.getValue();
        String operator = condition.getOperator();

        // Evaluate based on operator
        return evaluateOperator(operator, actualValue, expectedValue);
    }

    /**
     * Evaluate operator
     */
    private boolean evaluateOperator(String operator, Object actualValue, Object expectedValue) {
        switch (operator.toUpperCase()) {
            case "EQUALS":
                return equals(actualValue, expectedValue);

            case "NOT_EQUALS":
                return !equals(actualValue, expectedValue);

            case "GREATER_THAN":
                return greaterThan(actualValue, expectedValue);

            case "GREATER_THAN_OR_EQUAL":
                return greaterThanOrEqual(actualValue, expectedValue);

            case "LESS_THAN":
                return lessThan(actualValue, expectedValue);

            case "LESS_THAN_OR_EQUAL":
                return lessThanOrEqual(actualValue, expectedValue);

            case "IN":
                return in(actualValue, expectedValue);

            case "NOT_IN":
                return !in(actualValue, expectedValue);

            case "MATCHES":
                return matches(actualValue, expectedValue);

            case "CONTAINS":
                return contains(actualValue, expectedValue);

            case "STARTS_WITH":
                return startsWith(actualValue, expectedValue);

            case "ENDS_WITH":
                return endsWith(actualValue, expectedValue);

            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    // ========================================================================
    // Operator Implementation Methods
    // ========================================================================

    private boolean equals(Object actual, Object expected) {
        return Objects.equals(actual.toString(), expected.toString());
    }

    private boolean greaterThan(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum > expectedNum;
        } catch (NumberFormatException e) {
            log.warn("Cannot compare as numbers: {} > {}", actual, expected);
            return false;
        }
    }

    private boolean greaterThanOrEqual(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum >= expectedNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean lessThan(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum < expectedNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean lessThanOrEqual(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum <= expectedNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean in(Object actual, Object expected) {
        if (expected instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) expected;
            return list.stream()
                .anyMatch(item -> Objects.equals(actual.toString(), item.toString()));
        }
        return false;
    }

    private boolean matches(Object actual, Object expected) {
        try {
            Pattern pattern = Pattern.compile(expected.toString());
            return pattern.matcher(actual.toString()).matches();
        } catch (Exception e) {
            log.warn("Invalid regex pattern: {}", expected);
            return false;
        }
    }

    private boolean contains(Object actual, Object expected) {
        return actual.toString().contains(expected.toString());
    }

    private boolean startsWith(Object actual, Object expected) {
        return actual.toString().startsWith(expected.toString());
    }

    private boolean endsWith(Object actual, Object expected) {
        return actual.toString().endsWith(expected.toString());
    }

    /**
     * Convert value to double for numeric comparison
     */
    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
