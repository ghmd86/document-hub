package com.example.droolspoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Enhanced Account Fact - Includes data from multiple APIs
 *
 * This fact includes:
 * - Basic account data (from Account Service API)
 * - Cardholder Agreements TNC Code (from chained API calls)
 * - Pricing ID (from Arrangements API)
 *
 * All data is assembled BEFORE calling Drools.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedAccountFact {

    // Basic account data
    private String accountId;
    private BigDecimal balance;
    private String status;
    private String accountType;
    private BigDecimal creditLimit;
    private String state;

    // ⭐ Data from external APIs (chained calls)
    private String pricingId;                      // From Arrangements API
    private String cardholderAgreementsTNCCode;    // From Cardholder Agreements API (via pricingId)

    /**
     * Create from AccountFact and external API data
     */
    public static EnhancedAccountFact from(AccountFact account, CompleteEligibilityData data) {
        return EnhancedAccountFact.builder()
            .accountId(account.getAccountId())
            .balance(account.getBalance())
            .status(account.getStatus())
            .accountType(account.getAccountType())
            .creditLimit(account.getCreditLimit())
            .state(account.getState())
            .pricingId(data.getPricingId())
            .cardholderAgreementsTNCCode(data.getCardholderAgreementsTNCCode())
            .build();
    }
}
