package com.documenthub.rules.model;

/**
 * Enumeration of supported operators for rule evaluation.
 */
public enum OperatorType {
    IN("in"),
    NOT_IN("notIn"),
    EQUALS("equals"),
    NOT_EQUALS("notEquals"),
    LESS_THAN("lessThan"),
    LESS_THAN_OR_EQUAL("lessThanOrEqual"),
    GREATER_THAN("greaterThan"),
    GREATER_THAN_OR_EQUAL("greaterThanOrEqual"),
    BETWEEN("between"),
    CONTAINS("contains"),
    NOT_CONTAINS("notContains"),
    STARTS_WITH("startsWith"),
    ENDS_WITH("endsWith"),
    MATCHES("matches");

    private final String value;

    OperatorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OperatorType fromValue(String value) {
        for (OperatorType type : OperatorType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown operator type: " + value);
    }
}
