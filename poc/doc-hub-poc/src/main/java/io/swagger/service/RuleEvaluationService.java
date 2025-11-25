package io.swagger.service;

import io.swagger.model.access.AccessControl;
import io.swagger.model.access.EligibilityCriteria;
import io.swagger.model.context.ExtractionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class RuleEvaluationService {

    /**
     * Evaluate if a template is eligible based on access control rules
     */
    public boolean evaluateEligibility(
        AccessControl accessControl,
        ExtractionContext context
    ) {
        if (accessControl == null) {
            log.debug("No access control defined, allowing access");
            return true;
        }

        log.debug("Evaluating eligibility for type: {}", accessControl.getEligibilityType());

        // Evaluate based on eligibility type
        if ("criteria-based".equalsIgnoreCase(accessControl.getEligibilityType())) {
            return evaluateCriteriaBased(accessControl, context);
        } else {
            log.warn("Unknown eligibility type: {}", accessControl.getEligibilityType());
            return true; // Default to allowing access
        }
    }

    /**
     * Evaluate criteria-based eligibility
     */
    private boolean evaluateCriteriaBased(
        AccessControl accessControl,
        ExtractionContext context
    ) {
        Map<String, EligibilityCriteria> criteriaMap = accessControl.getEligibilityCriteria();

        if (criteriaMap == null || criteriaMap.isEmpty()) {
            log.debug("No eligibility criteria defined");
            return true;
        }

        String logic = accessControl.getLogic() != null ?
            accessControl.getLogic().toUpperCase() : "AND";

        log.debug("Evaluating {} criteria with {} logic", criteriaMap.size(), logic);

        if ("AND".equals(logic)) {
            // All criteria must be satisfied
            return criteriaMap.entrySet().stream()
                .allMatch(entry -> evaluateCriteria(entry.getKey(), entry.getValue(), context));
        } else if ("OR".equals(logic)) {
            // At least one criterion must be satisfied
            return criteriaMap.entrySet().stream()
                .anyMatch(entry -> evaluateCriteria(entry.getKey(), entry.getValue(), context));
        } else {
            log.warn("Unknown logic operator: {}", logic);
            return false;
        }
    }

    /**
     * Evaluate a single eligibility criterion
     */
    private boolean evaluateCriteria(
        String criteriaName,
        EligibilityCriteria criteria,
        ExtractionContext context
    ) {
        String field = criteria.getField();
        String operator = criteria.getOperator();
        Object expectedValue = criteria.getValue();

        // Get actual value from context
        Object actualValue = context.getVariables().get(field);

        log.debug("Evaluating criteria '{}': field={}, operator={}, expected={}, actual={}",
            criteriaName, field, operator, expectedValue, actualValue);

        // Evaluate based on operator
        boolean result = switch (operator.toLowerCase()) {
            case "equals" -> Objects.equals(actualValue, expectedValue);
            case "notequals" -> !Objects.equals(actualValue, expectedValue);
            case "in" -> {
                if (expectedValue instanceof List) {
                    yield ((List<?>) expectedValue).contains(actualValue);
                }
                yield false;
            }
            case "notin" -> {
                if (expectedValue instanceof List) {
                    yield !((List<?>) expectedValue).contains(actualValue);
                }
                yield false;
            }
            case "contains" -> {
                if (actualValue instanceof String && expectedValue instanceof String) {
                    yield ((String) actualValue).contains((String) expectedValue);
                }
                yield false;
            }
            case "startswith" -> {
                if (actualValue instanceof String && expectedValue instanceof String) {
                    yield ((String) actualValue).startsWith((String) expectedValue);
                }
                yield false;
            }
            case "endswith" -> {
                if (actualValue instanceof String && expectedValue instanceof String) {
                    yield ((String) actualValue).endsWith((String) expectedValue);
                }
                yield false;
            }
            case "greaterthan" -> compareValues(actualValue, expectedValue) > 0;
            case "lessthan" -> compareValues(actualValue, expectedValue) < 0;
            case "greaterthanorequal" -> compareValues(actualValue, expectedValue) >= 0;
            case "lessthanorequal" -> compareValues(actualValue, expectedValue) <= 0;
            case "notnull" -> actualValue != null;
            case "isnull" -> actualValue == null;
            case "matches" -> {
                if (actualValue instanceof String && expectedValue instanceof String) {
                    yield ((String) actualValue).matches((String) expectedValue);
                }
                yield false;
            }
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield false;
            }
        };

        log.debug("Criteria '{}' evaluation result: {}", criteriaName, result);
        return result;
    }

    /**
     * Compare two values for ordering
     */
    private int compareValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }

        // Handle numeric comparisons
        if (value1 instanceof Number && value2 instanceof Number) {
            double d1 = ((Number) value1).doubleValue();
            double d2 = ((Number) value2).doubleValue();
            return Double.compare(d1, d2);
        }

        // Handle comparable types
        if (value1 instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable = (Comparable<Object>) value1;
                return comparable.compareTo(value2);
            } catch (ClassCastException e) {
                log.warn("Cannot compare {} and {}",
                    value1.getClass().getSimpleName(),
                    value2.getClass().getSimpleName());
                return 0;
            }
        }

        log.warn("Values are not comparable: {} and {}",
            value1.getClass().getSimpleName(),
            value2.getClass().getSimpleName());
        return 0;
    }
}
