package com.example.eligibility.service;

import com.example.eligibility.entity.EligibilityRuleEntity;
import com.example.eligibility.model.DataContext;
import com.example.eligibility.model.RuleFact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rule Evaluator Service
 *
 * Evaluates eligibility rules using Drools rule engine.
 *
 * Flow:
 * 1. Load rules from database
 * 2. Create Drools facts from fetched data
 * 3. Evaluate each rule's conditions
 * 4. Collect eligible documents
 *
 * Note: This is a simplified implementation that evaluates rules programmatically.
 * In a production system, you might dynamically generate DRL files from database config.
 */
@Service
public class RuleEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluatorService.class);

    private final ConfigurationLoaderService configLoader;
    private final ObjectMapper objectMapper;

    public RuleEvaluatorService(
            ConfigurationLoaderService configLoader,
            ObjectMapper objectMapper
    ) {
        this.configLoader = configLoader;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate rules and return eligible documents
     *
     * @param dataContext Data fetched from external APIs
     * @return Set of eligible document IDs
     */
    public Set<String> evaluateRules(DataContext dataContext) {
        log.info("Evaluating rules with data: {}", dataContext);

        Set<String> eligibleDocuments = new HashSet<>();

        // Load rules and evaluate
        configLoader.loadEligibilityRules()
                .subscribe(rules -> {
                    for (EligibilityRuleEntity rule : rules) {
                        if (evaluateRule(rule, dataContext)) {
                            eligibleDocuments.add(rule.getDocumentId());
                            log.debug("Rule {} matched. Adding document: {}",
                                    rule.getRuleId(), rule.getDocumentId());
                        }
                    }
                });

        // Wait for evaluation to complete (simplified - in production use reactive approach)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Rule evaluation completed. Eligible documents: {}", eligibleDocuments);
        return eligibleDocuments;
    }

    /**
     * Evaluate a single rule
     *
     * @param rule Rule entity
     * @param dataContext Data context
     * @return true if rule matches
     */
    private boolean evaluateRule(EligibilityRuleEntity rule, DataContext dataContext) {
        try {
            JsonNode conditions = objectMapper.readTree(rule.getConditions().asString());

            String type = conditions.path("type").asText("ALL");
            JsonNode expressions = conditions.path("expressions");

            if (type.equals("ALL")) {
                return evaluateAllConditions(expressions, dataContext);
            } else {
                return evaluateAnyCondition(expressions, dataContext);
            }

        } catch (Exception e) {
            log.error("Error evaluating rule {}: {}", rule.getRuleId(), e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate ALL conditions (AND logic)
     *
     * @param expressions JSON array of expressions
     * @param dataContext Data context
     * @return true if all conditions match
     */
    private boolean evaluateAllConditions(JsonNode expressions, DataContext dataContext) {
        if (!expressions.isArray()) {
            return false;
        }

        for (JsonNode expr : expressions) {
            if (!evaluateExpression(expr, dataContext)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluate ANY condition (OR logic)
     *
     * @param expressions JSON array of expressions
     * @param dataContext Data context
     * @return true if any condition matches
     */
    private boolean evaluateAnyCondition(JsonNode expressions, DataContext dataContext) {
        if (!expressions.isArray()) {
            return false;
        }

        for (JsonNode expr : expressions) {
            if (evaluateExpression(expr, dataContext)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Evaluate a single expression
     *
     * @param expr Expression JSON node
     * @param dataContext Data context
     * @return true if expression matches
     */
    private boolean evaluateExpression(JsonNode expr, DataContext dataContext) {
        String source = expr.path("source").asText();
        String field = expr.path("field").asText();
        String operator = expr.path("operator").asText();
        JsonNode valueNode = expr.path("value");

        // Get actual value from data context
        Object actualValue = dataContext.getField(source, field);

        if (actualValue == null) {
            log.debug("Field {}.{} not found in data context", source, field);
            return false;
        }

        // Evaluate based on operator
        return evaluateOperator(operator, actualValue, valueNode);
    }

    /**
     * Evaluate operator
     *
     * @param operator Operator (EQUALS, GREATER_THAN, etc.)
     * @param actualValue Actual value from data
     * @param expectedValueNode Expected value from rule
     * @return true if condition matches
     */
    private boolean evaluateOperator(String operator, Object actualValue, JsonNode expectedValueNode) {
        switch (operator) {
            case "EQUALS":
                return compareEquals(actualValue, expectedValueNode);

            case "NOT_EQUALS":
                return !compareEquals(actualValue, expectedValueNode);

            case "GREATER_THAN":
                return compareGreaterThan(actualValue, expectedValueNode);

            case "GREATER_THAN_OR_EQUAL":
                return compareGreaterThanOrEqual(actualValue, expectedValueNode);

            case "LESS_THAN":
                return compareLessThan(actualValue, expectedValueNode);

            case "LESS_THAN_OR_EQUAL":
                return compareLessThanOrEqual(actualValue, expectedValueNode);

            case "IN":
                return compareIn(actualValue, expectedValueNode);

            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Compare for equality
     */
    private boolean compareEquals(Object actualValue, JsonNode expectedValueNode) {
        String actual = String.valueOf(actualValue);
        String expected = expectedValueNode.asText();
        return actual.equals(expected);
    }

    /**
     * Compare greater than
     */
    private boolean compareGreaterThan(Object actualValue, JsonNode expectedValueNode) {
        try {
            BigDecimal actual = new BigDecimal(String.valueOf(actualValue));
            BigDecimal expected = new BigDecimal(expectedValueNode.asText());
            return actual.compareTo(expected) > 0;
        } catch (Exception e) {
            log.warn("Error comparing numbers: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compare greater than or equal
     */
    private boolean compareGreaterThanOrEqual(Object actualValue, JsonNode expectedValueNode) {
        try {
            BigDecimal actual = new BigDecimal(String.valueOf(actualValue));
            BigDecimal expected = new BigDecimal(expectedValueNode.asText());
            return actual.compareTo(expected) >= 0;
        } catch (Exception e) {
            log.warn("Error comparing numbers: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compare less than
     */
    private boolean compareLessThan(Object actualValue, JsonNode expectedValueNode) {
        try {
            BigDecimal actual = new BigDecimal(String.valueOf(actualValue));
            BigDecimal expected = new BigDecimal(expectedValueNode.asText());
            return actual.compareTo(expected) < 0;
        } catch (Exception e) {
            log.warn("Error comparing numbers: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compare less than or equal
     */
    private boolean compareLessThanOrEqual(Object actualValue, JsonNode expectedValueNode) {
        try {
            BigDecimal actual = new BigDecimal(String.valueOf(actualValue));
            BigDecimal expected = new BigDecimal(expectedValueNode.asText());
            return actual.compareTo(expected) <= 0;
        } catch (Exception e) {
            log.warn("Error comparing numbers: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compare IN (value in list)
     */
    private boolean compareIn(Object actualValue, JsonNode expectedValueNode) {
        if (!expectedValueNode.isArray()) {
            return false;
        }

        String actual = String.valueOf(actualValue);

        for (JsonNode item : expectedValueNode) {
            if (actual.equals(item.asText())) {
                return true;
            }
        }

        return false;
    }
}
