package com.documenthub.rules.evaluator;

import com.documenthub.rules.model.CriteriaRule;
import com.documenthub.rules.model.OperatorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Core rule evaluation engine.
 * Evaluates criteria rules against extracted data using various operators and data types.
 */
@Slf4j
@Component
public class RuleEvaluator {

    /**
     * Evaluate all criteria against extracted data.
     *
     * @param eligibilityCriteria Map of field name to criteria rule
     * @param extractedData Map of field name to extracted value
     * @return true if all criteria pass, false otherwise
     */
    public boolean evaluateAll(Map<String, CriteriaRule> eligibilityCriteria, Map<String, Object> extractedData) {
        if (eligibilityCriteria == null || eligibilityCriteria.isEmpty()) {
            log.warn("No eligibility criteria defined, defaulting to false");
            return false;
        }

        for (Map.Entry<String, CriteriaRule> entry : eligibilityCriteria.entrySet()) {
            String fieldName = entry.getKey();
            CriteriaRule rule = entry.getValue();

            // Special handling for duration-based criteria
            Object actualValue;
            if ("duration".equalsIgnoreCase(rule.getDataType()) && rule.getCompareField() != null) {
                actualValue = calculateDuration(extractedData.get(rule.getCompareField()), rule.getUnit());
            } else {
                actualValue = extractedData.get(fieldName);
            }

            if (!evaluateSingle(rule, actualValue)) {
                log.debug("Eligibility check failed for field: {} with value: {}", fieldName, actualValue);
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluate a single criteria rule against a value.
     */
    public boolean evaluateSingle(CriteriaRule rule, Object actualValue) {
        if (actualValue == null) {
            log.debug("Actual value is null, criteria evaluation failed");
            return false;
        }

        try {
            OperatorType operator = rule.getOperatorType();

            switch (operator) {
                case IN:
                    return evaluateIn(actualValue, rule.getValues());
                case NOT_IN:
                    return !evaluateIn(actualValue, rule.getValues());
                case EQUALS:
                    return evaluateEquals(actualValue, rule.getValue(), rule.getDataType());
                case NOT_EQUALS:
                    return !evaluateEquals(actualValue, rule.getValue(), rule.getDataType());
                case LESS_THAN:
                    return evaluateLessThan(actualValue, rule.getValue(), rule.getDataType());
                case LESS_THAN_OR_EQUAL:
                    return evaluateLessThanOrEqual(actualValue, rule.getValue(), rule.getDataType());
                case GREATER_THAN:
                    return evaluateGreaterThan(actualValue, rule.getValue(), rule.getDataType());
                case GREATER_THAN_OR_EQUAL:
                    return evaluateGreaterThanOrEqual(actualValue, rule.getValue(), rule.getDataType());
                case BETWEEN:
                    return evaluateBetween(actualValue, rule.getMinValue(), rule.getMaxValue(), rule.getDataType());
                case CONTAINS:
                    return evaluateContains(actualValue, rule.getValue());
                case NOT_CONTAINS:
                    return !evaluateContains(actualValue, rule.getValue());
                case STARTS_WITH:
                    return evaluateStartsWith(actualValue, rule.getValue());
                case ENDS_WITH:
                    return evaluateEndsWith(actualValue, rule.getValue());
                case MATCHES:
                    return evaluateMatches(actualValue, rule.getValue());
                default:
                    log.error("Unsupported operator: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error evaluating rule: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean evaluateIn(Object actualValue, List<Object> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        String actualStr = String.valueOf(actualValue);
        return values.stream()
                .map(String::valueOf)
                .anyMatch(actualStr::equals);
    }

    private boolean evaluateEquals(Object actualValue, Object expectedValue, String dataType) {
        if ("number".equalsIgnoreCase(dataType)) {
            return compareNumbers(actualValue, expectedValue) == 0;
        }
        return String.valueOf(actualValue).equals(String.valueOf(expectedValue));
    }

    private boolean evaluateLessThan(Object actualValue, Object compareValue, String dataType) {
        if ("number".equalsIgnoreCase(dataType) || "duration".equalsIgnoreCase(dataType)) {
            return compareNumbers(actualValue, compareValue) < 0;
        }
        if ("date".equalsIgnoreCase(dataType)) {
            return compareDates(actualValue, compareValue) < 0;
        }
        return false;
    }

    private boolean evaluateLessThanOrEqual(Object actualValue, Object compareValue, String dataType) {
        if ("number".equalsIgnoreCase(dataType) || "duration".equalsIgnoreCase(dataType)) {
            return compareNumbers(actualValue, compareValue) <= 0;
        }
        if ("date".equalsIgnoreCase(dataType)) {
            return compareDates(actualValue, compareValue) <= 0;
        }
        return false;
    }

    private boolean evaluateGreaterThan(Object actualValue, Object compareValue, String dataType) {
        if ("number".equalsIgnoreCase(dataType) || "duration".equalsIgnoreCase(dataType)) {
            return compareNumbers(actualValue, compareValue) > 0;
        }
        if ("date".equalsIgnoreCase(dataType)) {
            return compareDates(actualValue, compareValue) > 0;
        }
        return false;
    }

    private boolean evaluateGreaterThanOrEqual(Object actualValue, Object compareValue, String dataType) {
        if ("number".equalsIgnoreCase(dataType) || "duration".equalsIgnoreCase(dataType)) {
            return compareNumbers(actualValue, compareValue) >= 0;
        }
        if ("date".equalsIgnoreCase(dataType)) {
            return compareDates(actualValue, compareValue) >= 0;
        }
        return false;
    }

    private boolean evaluateBetween(Object actualValue, Object minValue, Object maxValue, String dataType) {
        if ("number".equalsIgnoreCase(dataType)) {
            return compareNumbers(actualValue, minValue) >= 0 && compareNumbers(actualValue, maxValue) <= 0;
        }
        if ("date".equalsIgnoreCase(dataType)) {
            return compareDates(actualValue, minValue) >= 0 && compareDates(actualValue, maxValue) <= 0;
        }
        return false;
    }

    private boolean evaluateContains(Object actualValue, Object searchValue) {
        return String.valueOf(actualValue).contains(String.valueOf(searchValue));
    }

    private boolean evaluateStartsWith(Object actualValue, Object prefix) {
        return String.valueOf(actualValue).startsWith(String.valueOf(prefix));
    }

    private boolean evaluateEndsWith(Object actualValue, Object suffix) {
        return String.valueOf(actualValue).endsWith(String.valueOf(suffix));
    }

    private boolean evaluateMatches(Object actualValue, Object pattern) {
        try {
            Pattern regex = Pattern.compile(String.valueOf(pattern));
            return regex.matcher(String.valueOf(actualValue)).matches();
        } catch (Exception e) {
            log.error("Error evaluating regex pattern: {}", e.getMessage());
            return false;
        }
    }

    private int compareNumbers(Object value1, Object value2) {
        BigDecimal bd1 = new BigDecimal(String.valueOf(value1));
        BigDecimal bd2 = new BigDecimal(String.valueOf(value2));
        return bd1.compareTo(bd2);
    }

    private int compareDates(Object value1, Object value2) {
        long timestamp1 = parseTimestamp(value1);
        long timestamp2 = parseTimestamp(value2);
        return Long.compare(timestamp1, timestamp2);
    }

    private long parseTimestamp(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            // Try parsing as ISO date
            Instant instant = Instant.parse(String.valueOf(value));
            return instant.getEpochSecond();
        }
    }

    /**
     * Calculate duration from a date field to current time.
     *
     * @param dateValue The date value (epoch seconds or ISO string)
     * @param unit The unit to calculate (years, months, days)
     * @return Duration value as a number
     */
    private Object calculateDuration(Object dateValue, String unit) {
        if (dateValue == null) {
            return 0;
        }

        try {
            long timestamp = parseTimestamp(dateValue);
            Instant pastInstant = Instant.ofEpochSecond(timestamp);
            Instant now = Instant.now();

            if ("years".equalsIgnoreCase(unit)) {
                LocalDate pastDate = pastInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate currentDate = now.atZone(ZoneId.systemDefault()).toLocalDate();
                return ChronoUnit.YEARS.between(pastDate, currentDate);
            } else if ("months".equalsIgnoreCase(unit)) {
                LocalDate pastDate = pastInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate currentDate = now.atZone(ZoneId.systemDefault()).toLocalDate();
                return ChronoUnit.MONTHS.between(pastDate, currentDate);
            } else if ("days".equalsIgnoreCase(unit)) {
                return ChronoUnit.DAYS.between(pastInstant, now);
            }

            return 0;
        } catch (Exception e) {
            log.error("Error calculating duration: {}", e.getMessage());
            return 0;
        }
    }
}
