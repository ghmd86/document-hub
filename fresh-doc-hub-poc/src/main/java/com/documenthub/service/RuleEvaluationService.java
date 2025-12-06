package com.documenthub.service;

import com.documenthub.model.AccountMetadata;
import com.documenthub.model.EligibilityCriteria;
import com.documenthub.model.Rule;
import com.documenthub.model.TemplateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for evaluating eligibility rules from template configuration
 */
@Service
@Slf4j
public class RuleEvaluationService {

    /**
     * Evaluate if account meets eligibility criteria defined in template config
     *
     * @param templateConfig  the template configuration containing eligibility criteria
     * @param accountMetadata the account metadata to evaluate against
     * @param requestContext  additional context from the request
     * @return true if eligible, false otherwise
     */
    public boolean evaluateEligibility(
        TemplateConfig templateConfig,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        if (templateConfig == null || templateConfig.getEligibilityCriteria() == null) {
            log.debug("No eligibility criteria defined, allowing access");
            return true;
        }

        EligibilityCriteria criteria = templateConfig.getEligibilityCriteria();

        if (criteria.getRules() == null || criteria.getRules().isEmpty()) {
            log.debug("No rules defined, allowing access");
            return true;
        }

        String operator = criteria.getOperator() != null ? criteria.getOperator() : "AND";

        if ("OR".equalsIgnoreCase(operator)) {
            return evaluateWithOR(criteria.getRules(), accountMetadata, requestContext);
        } else {
            return evaluateWithAND(criteria.getRules(), accountMetadata, requestContext);
        }
    }

    /**
     * Evaluate if account meets eligibility criteria directly
     *
     * @param criteria        the eligibility criteria to evaluate
     * @param accountMetadata the account metadata to evaluate against
     * @param requestContext  additional context from the request
     * @return true if eligible, false otherwise
     */
    public boolean evaluateEligibility(
        EligibilityCriteria criteria,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        if (criteria == null || criteria.getRules() == null || criteria.getRules().isEmpty()) {
            log.debug("No eligibility criteria or rules defined, allowing access");
            return true;
        }

        String operator = criteria.getOperator() != null ? criteria.getOperator() : "AND";

        if ("OR".equalsIgnoreCase(operator)) {
            return evaluateWithOR(criteria.getRules(), accountMetadata, requestContext);
        } else {
            return evaluateWithAND(criteria.getRules(), accountMetadata, requestContext);
        }
    }

    /**
     * Evaluate rules with AND logic (all must pass)
     */
    private boolean evaluateWithAND(
        List<Rule> rules,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        for (Rule rule : rules) {
            if (!evaluateRule(rule, accountMetadata, requestContext)) {
                log.debug("Rule failed: {}", rule);
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate rules with OR logic (at least one must pass)
     */
    private boolean evaluateWithOR(
        List<Rule> rules,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        for (Rule rule : rules) {
            if (evaluateRule(rule, accountMetadata, requestContext)) {
                log.debug("Rule passed: {}", rule);
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluate a single rule
     */
    private boolean evaluateRule(
        Rule rule,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        String field = rule.getField();
        String operator = rule.getOperator();
        Object expectedValue = rule.getValue();

        // Get actual value from account metadata or request context
        Object actualValue = getFieldValue(field, accountMetadata, requestContext);

        if (actualValue == null) {
            log.debug("Field '{}' not found in account metadata or request context", field);
            return false;
        }

        return evaluateOperator(operator, actualValue, expectedValue);
    }

    /**
     * Get field value from account metadata or request context
     */
    private Object getFieldValue(
        String field,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        // Handle metadata fields (e.g., "$metadata.disclosure_code")
        if (field.startsWith("$metadata.")) {
            String metadataField = field.substring("$metadata.".length());
            return requestContext != null ? requestContext.get(metadataField) : null;
        }

        // Handle request fields (e.g., "$request.referenceKey")
        if (field.startsWith("$request.")) {
            String requestField = field.substring("$request.".length());
            return requestContext != null ? requestContext.get(requestField) : null;
        }

        // Handle account metadata fields
        switch (field) {
            case "accountType":
                return accountMetadata.getAccountType();
            case "region":
                return accountMetadata.getRegion();
            case "state":
                return accountMetadata.getState();
            case "customerSegment":
                return accountMetadata.getCustomerSegment();
            case "lineOfBusiness":
                return accountMetadata.getLineOfBusiness();
            case "accountId":
                return accountMetadata.getAccountId() != null ?
                    accountMetadata.getAccountId().toString() : null;
            case "customerId":
                return accountMetadata.getCustomerId() != null ?
                    accountMetadata.getCustomerId().toString() : null;
            default:
                // Fallback: check requestContext for dynamically extracted fields (e.g., zipcode)
                if (requestContext != null && requestContext.containsKey(field)) {
                    log.debug("Field '{}' found in request context (extracted data)", field);
                    return requestContext.get(field);
                }
                log.warn("Unknown field: {}", field);
                return null;
        }
    }

    /**
     * Evaluate operator
     */
    private boolean evaluateOperator(String operator, Object actualValue, Object expectedValue) {
        if (operator == null) {
            operator = "EQUALS";
        }

        switch (operator.toUpperCase()) {
            case "EQUALS":
                return actualValue.equals(expectedValue);

            case "NOT_EQUALS":
                return !actualValue.equals(expectedValue);

            case "IN":
                if (expectedValue instanceof List) {
                    return ((List<?>) expectedValue).contains(actualValue);
                }
                return false;

            case "NOT_IN":
                if (expectedValue instanceof List) {
                    return !((List<?>) expectedValue).contains(actualValue);
                }
                return true;

            case "GREATER_THAN":
                return compareNumeric(actualValue, expectedValue) > 0;

            case "LESS_THAN":
                return compareNumeric(actualValue, expectedValue) < 0;

            case "GREATER_THAN_OR_EQUAL":
                return compareNumeric(actualValue, expectedValue) >= 0;

            case "LESS_THAN_OR_EQUAL":
                return compareNumeric(actualValue, expectedValue) <= 0;

            case "CONTAINS":
                return actualValue.toString().contains(expectedValue.toString());

            case "STARTS_WITH":
                return actualValue.toString().startsWith(expectedValue.toString());

            case "ENDS_WITH":
                return actualValue.toString().endsWith(expectedValue.toString());

            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Compare numeric values
     */
    private int compareNumeric(Object actual, Object expected) {
        try {
            double actualNum = Double.parseDouble(actual.toString());
            double expectedNum = Double.parseDouble(expected.toString());
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            log.warn("Failed to compare as numeric: {} vs {}", actual, expected);
            return 0;
        }
    }
}
