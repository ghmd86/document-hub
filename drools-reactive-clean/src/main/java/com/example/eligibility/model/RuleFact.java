package com.example.eligibility.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Rule Fact
 *
 * Represents a fact that can be inserted into Drools working memory.
 * Contains data from external APIs that will be evaluated against rules.
 *
 * Example:
 * - source: "cardholder_agreements_api"
 * - data: {"cardholderAgreementsTNCCode": "TNC_GOLD_2024", "agreementStatus": "ACTIVE"}
 */
public class RuleFact {

    private String source;
    private Map<String, Object> data;

    public RuleFact() {
        this.data = new HashMap<>();
    }

    public RuleFact(String source) {
        this.source = source;
        this.data = new HashMap<>();
    }

    public RuleFact(String source, Map<String, Object> data) {
        this.source = source;
        this.data = data != null ? data : new HashMap<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Get a specific field value from the data
     *
     * @param fieldName Field name
     * @return Field value or null if not found
     */
    public Object getField(String fieldName) {
        return data.get(fieldName);
    }

    /**
     * Set a field value in the data
     *
     * @param fieldName Field name
     * @param value Field value
     */
    public void setField(String fieldName, Object value) {
        data.put(fieldName, value);
    }

    @Override
    public String toString() {
        return "RuleFact{" +
                "source='" + source + '\'' +
                ", data=" + data +
                '}';
    }
}
