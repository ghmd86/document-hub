package com.documenthub.rules.model;

/**
 * Enumeration of supported rule types for custom document sharing rules.
 */
public enum RuleType {
    LOCATION_BASED("location_based"),
    TENURE_BASED("tenure_based"),
    BALANCE_BASED("balance_based"),
    CREDIT_LIMIT_BASED("credit_limit_based"),
    TRANSACTION_PATTERN_BASED("transaction_pattern_based"),
    CUSTOMER_SEGMENT_BASED("customer_segment_based"),
    PRODUCT_TYPE_BASED("product_type_based"),
    COMPOSITE("composite");

    private final String value;

    RuleType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RuleType fromValue(String value) {
        for (RuleType type : RuleType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown rule type: " + value);
    }
}
