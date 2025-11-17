package com.example.eligibility.model;

/**
 * Eligibility Request
 *
 * Request parameters for checking document eligibility.
 */
public class EligibilityRequest {

    private String customerId;
    private String accountId;
    private String arrangementId;

    public EligibilityRequest() {
    }

    public EligibilityRequest(String customerId, String accountId, String arrangementId) {
        this.customerId = customerId;
        this.accountId = accountId;
        this.arrangementId = arrangementId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getArrangementId() {
        return arrangementId;
    }

    public void setArrangementId(String arrangementId) {
        this.arrangementId = arrangementId;
    }

    @Override
    public String toString() {
        return "EligibilityRequest{" +
                "customerId='" + customerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", arrangementId='" + arrangementId + '\'' +
                '}';
    }
}
