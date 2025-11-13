package com.example.droolspoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Account Fact - Input data for Drools rules
 *
 * This represents account information used in eligibility rules.
 * In a real application, this would be fetched from database reactively.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountFact {

    private String accountId;
    private BigDecimal balance;
    private String status;           // ACTIVE, CLOSED, SUSPENDED
    private String accountType;      // CHECKING, SAVINGS, CREDIT_CARD
    private BigDecimal creditLimit;
    private String state;            // CA, NY, TX, etc.
}
