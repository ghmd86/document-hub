package com.documenthub.rules.evaluator;

import com.documenthub.rules.model.CriteriaRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RuleEvaluator.
 */
class RuleEvaluatorTest {

    private RuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        ruleEvaluator = new RuleEvaluator();
    }

    @Test
    void testEvaluateIn_Success() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("in")
                .values(List.of("90001", "90002", "90210"))
                .dataType("string")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, "90001");
        assertTrue(result);
    }

    @Test
    void testEvaluateIn_Failure() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("in")
                .values(List.of("90001", "90002", "90210"))
                .dataType("string")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, "10001");
        assertFalse(result);
    }

    @Test
    void testEvaluateEquals_Number() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("equals")
                .value(5000)
                .dataType("number")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, 5000);
        assertTrue(result);
    }

    @Test
    void testEvaluateLessThan_Number() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("lessThan")
                .value(5000)
                .dataType("number")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, 4999);
        assertTrue(result);
    }

    @Test
    void testEvaluateGreaterThan_Number() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("greaterThan")
                .value(5)
                .dataType("number")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, 10);
        assertTrue(result);
    }

    @Test
    void testEvaluateBetween_Number() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("between")
                .minValue(1000)
                .maxValue(10000)
                .dataType("number")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, 5000);
        assertTrue(result);
    }

    @Test
    void testEvaluateContains_String() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("contains")
                .value("premium")
                .dataType("string")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, "premium_account");
        assertTrue(result);
    }

    @Test
    void testEvaluateStartsWith_String() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("startsWith")
                .value("ACC")
                .dataType("string")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, "ACC12345");
        assertTrue(result);
    }

    @Test
    void testEvaluateAll_MultipleRules() {
        Map<String, CriteriaRule> criteria = new HashMap<>();

        criteria.put("balance", CriteriaRule.builder()
                .operator("lessThan")
                .value(5000)
                .dataType("number")
                .build());

        criteria.put("zipcode", CriteriaRule.builder()
                .operator("in")
                .values(List.of("90001", "90002"))
                .dataType("string")
                .build());

        Map<String, Object> extractedData = new HashMap<>();
        extractedData.put("balance", 4500);
        extractedData.put("zipcode", "90001");

        boolean result = ruleEvaluator.evaluateAll(criteria, extractedData);
        assertTrue(result);
    }

    @Test
    void testEvaluateAll_OneFails() {
        Map<String, CriteriaRule> criteria = new HashMap<>();

        criteria.put("balance", CriteriaRule.builder()
                .operator("lessThan")
                .value(5000)
                .dataType("number")
                .build());

        criteria.put("zipcode", CriteriaRule.builder()
                .operator("in")
                .values(List.of("90001", "90002"))
                .dataType("string")
                .build());

        Map<String, Object> extractedData = new HashMap<>();
        extractedData.put("balance", 4500);
        extractedData.put("zipcode", "10001"); // This doesn't match

        boolean result = ruleEvaluator.evaluateAll(criteria, extractedData);
        assertFalse(result);
    }

    @Test
    void testEvaluateDuration_Years() {
        // Customer since 6 years ago
        long customerSince = Instant.now().minus(6 * 365, ChronoUnit.DAYS).getEpochSecond();

        CriteriaRule rule = CriteriaRule.builder()
                .operator("greaterThan")
                .value(5)
                .dataType("duration")
                .unit("years")
                .compareField("customerSinceDate")
                .build();

        Map<String, Object> extractedData = new HashMap<>();
        extractedData.put("customerSinceDate", customerSince);

        Map<String, CriteriaRule> criteria = new HashMap<>();
        criteria.put("tenureYears", rule);

        boolean result = ruleEvaluator.evaluateAll(criteria, extractedData);
        assertTrue(result);
    }

    @Test
    void testEvaluateMatches_Regex() {
        CriteriaRule rule = CriteriaRule.builder()
                .operator("matches")
                .value("^[A-Z]{3}\\d{5}$")
                .dataType("string")
                .build();

        boolean result = ruleEvaluator.evaluateSingle(rule, "ABC12345");
        assertTrue(result);

        result = ruleEvaluator.evaluateSingle(rule, "invalid");
        assertFalse(result);
    }
}
