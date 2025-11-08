package com.documenthub.service.extraction;

import com.documenthub.model.extraction.ExtractionConfig;
import com.documenthub.model.extraction.ExtractionContext;
import com.documenthub.model.extraction.ExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Service for evaluating inclusion rules
 */
@Slf4j
@Service
public class RuleEvaluationService {

    private ExtractionResult.RuleEvaluation lastEvaluation;

    public boolean evaluate(ExtractionConfig.InclusionRules rules, ExtractionContext context) {
        List<ExtractionResult.ConditionResult> conditionResults = new ArrayList<>();
        List<Boolean> results = new ArrayList<>();

        for (ExtractionConfig.RuleCondition condition : rules.getConditions()) {
            boolean result = evaluateCondition(condition, context);
            results.add(result);

            conditionResults.add(ExtractionResult.ConditionResult.builder()
                    .field(condition.getField())
                    .operator(condition.getOperator())
                    .value(condition.getValue())
                    .result(result)
                    .build());
        }

        boolean finalResult = combineResults(results, rules.getOperator());

        lastEvaluation = ExtractionResult.RuleEvaluation.builder()
                .result(finalResult)
                .matchedConditions(conditionResults)
                .build();

        return finalResult;
    }

    private boolean evaluateCondition(ExtractionConfig.RuleCondition condition, ExtractionContext context) {
        Object fieldValue = context.getVariable(condition.getField());
        String operator = condition.getOperator();
        Object expectedValue = condition.getValue();

        switch (operator) {
            case "exists":
                return fieldValue != null;
            case "notExists":
                return fieldValue == null;
            case "==":
                return Objects.equals(fieldValue, expectedValue);
            case "!=":
                return !Objects.equals(fieldValue, expectedValue);
            case ">":
                return compareNumbers(fieldValue, expectedValue) > 0;
            case ">=":
                return compareNumbers(fieldValue, expectedValue) >= 0;
            case "<":
                return compareNumbers(fieldValue, expectedValue) < 0;
            case "<=":
                return compareNumbers(fieldValue, expectedValue) <= 0;
            case "in":
                return isInList(fieldValue, expectedValue);
            case "notIn":
                return !isInList(fieldValue, expectedValue);
            case "matches":
                return matchesPattern(fieldValue, expectedValue);
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    private int compareNumbers(Object value1, Object value2) {
        if (value1 == null || value2 == null) {
            return -1;
        }

        double num1 = ((Number) value1).doubleValue();
        double num2 = ((Number) value2).doubleValue();

        return Double.compare(num1, num2);
    }

    @SuppressWarnings("unchecked")
    private boolean isInList(Object value, Object list) {
        if (value == null || !(list instanceof List)) {
            return false;
        }

        return ((List<Object>) list).contains(value);
    }

    private boolean matchesPattern(Object value, Object pattern) {
        if (value == null || pattern == null) {
            return false;
        }

        return Pattern.matches(String.valueOf(pattern), String.valueOf(value));
    }

    private boolean combineResults(List<Boolean> results, String operator) {
        if ("AND".equalsIgnoreCase(operator)) {
            return results.stream().allMatch(Boolean::booleanValue);
        } else if ("OR".equalsIgnoreCase(operator)) {
            return results.stream().anyMatch(Boolean::booleanValue);
        } else if ("NOT".equalsIgnoreCase(operator)) {
            return results.stream().noneMatch(Boolean::booleanValue);
        }
        return false;
    }

    public ExtractionResult.RuleEvaluation getLastEvaluation() {
        return lastEvaluation;
    }
}
