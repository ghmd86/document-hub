package com.example.droolspoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete Eligibility Data
 *
 * Aggregates all data needed for Drools rule evaluation:
 * - Account details
 * - Customer details
 * - Cardholder agreements TNC code (fetched via chained API calls)
 * - Pricing ID
 *
 * This object is assembled by fetching from multiple external APIs,
 * then passed to Drools for rule evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteEligibilityData {

    /**
     * Account details (from Account Service API)
     */
    private AccountFact account;

    /**
     * Customer details (from Customer Service API)
     */
    private CustomerFact customer;

    /**
     * Cardholder Agreements TNC Code
     * Fetched via chained calls:
     * 1. Arrangements API → pricingId
     * 2. Cardholder Agreements API (using pricingId) → TNC code
     */
    private String cardholderAgreementsTNCCode;

    /**
     * Pricing ID (from Arrangements API)
     */
    private String pricingId;
}
