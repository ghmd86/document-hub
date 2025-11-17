package com.example.eligibility.model;

import java.util.Set;

/**
 * Eligibility Response
 *
 * Response containing eligible documents for a customer.
 */
public class EligibilityResponse {

    private String customerId;
    private String accountId;
    private Set<String> eligibleDocuments;
    private Long evaluationTimeMs;

    public EligibilityResponse() {
    }

    public EligibilityResponse(String customerId, String accountId,
                                Set<String> eligibleDocuments, Long evaluationTimeMs) {
        this.customerId = customerId;
        this.accountId = accountId;
        this.eligibleDocuments = eligibleDocuments;
        this.evaluationTimeMs = evaluationTimeMs;
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

    public Set<String> getEligibleDocuments() {
        return eligibleDocuments;
    }

    public void setEligibleDocuments(Set<String> eligibleDocuments) {
        this.eligibleDocuments = eligibleDocuments;
    }

    public Long getEvaluationTimeMs() {
        return evaluationTimeMs;
    }

    public void setEvaluationTimeMs(Long evaluationTimeMs) {
        this.evaluationTimeMs = evaluationTimeMs;
    }

    @Override
    public String toString() {
        return "EligibilityResponse{" +
                "customerId='" + customerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", eligibleDocuments=" + eligibleDocuments +
                ", evaluationTimeMs=" + evaluationTimeMs +
                '}';
    }
}
