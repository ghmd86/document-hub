package com.documenthub.mock.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock Account Service API.
 * Provides account details, balance, arrangements, and product information.
 */
@Slf4j
@RestController
public class AccountServiceController {

    // Mock account data
    private static final Map<String, Map<String, Object>> MOCK_ACCOUNTS = new HashMap<>();

    static {
        // Account 1: Low balance ($4,500), credit card
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", "770e8400-e29b-41d4-a716-446655440001");
        account1.put("customerId", "880e8400-e29b-41d4-a716-446655440001");
        account1.put("accountNumber", "****1234");
        account1.put("accountType", "CREDIT_CARD");
        account1.put("lineOfBusiness", "credit_card");
        account1.put("productCode", "REWARDS_PLUS");
        account1.put("productFamily", "CASHBACK_CARDS");
        account1.put("creditLimit", 10000.00);
        account1.put("currentBalance", 4500.00);
        account1.put("availableBalance", 5500.00);
        account1.put("outstandingBalance", 4500.00);
        account1.put("status", "ACTIVE");
        MOCK_ACCOUNTS.put("770e8400-e29b-41d4-a716-446655440001", account1);

        // Account 2: High balance ($12,500), credit card
        Map<String, Object> account2 = new HashMap<>();
        account2.put("accountId", "770e8400-e29b-41d4-a716-446655440002");
        account2.put("customerId", "880e8400-e29b-41d4-a716-446655440001");
        account2.put("accountNumber", "****5678");
        account2.put("accountType", "CREDIT_CARD");
        account2.put("lineOfBusiness", "credit_card");
        account2.put("productCode", "PREMIUM_PLATINUM");
        account2.put("productFamily", "PREMIUM_CARDS");
        account2.put("creditLimit", 25000.00);
        account2.put("currentBalance", 12500.00);
        account2.put("availableBalance", 12500.00);
        account2.put("outstandingBalance", 12500.00);
        account2.put("status", "ACTIVE");
        MOCK_ACCOUNTS.put("770e8400-e29b-41d4-a716-446655440002", account2);

        // Account 3: Medium balance ($8,200), credit card
        Map<String, Object> account3 = new HashMap<>();
        account3.put("accountId", "770e8400-e29b-41d4-a716-446655440003");
        account3.put("customerId", "880e8400-e29b-41d4-a716-446655440002");
        account3.put("accountNumber", "****9012");
        account3.put("accountType", "CREDIT_CARD");
        account3.put("lineOfBusiness", "credit_card");
        account3.put("productCode", "TRAVEL_REWARDS");
        account3.put("productFamily", "REWARDS_CARDS");
        account3.put("creditLimit", 15000.00);
        account3.put("currentBalance", 8200.00);
        account3.put("availableBalance", 6800.00);
        account3.put("outstandingBalance", 8200.00);
        account3.put("status", "ACTIVE");
        MOCK_ACCOUNTS.put("770e8400-e29b-41d4-a716-446655440003", account3);
    }

    /**
     * GET /accounts-service/accounts/{accountId}
     * Returns account details including line of business and product type.
     */
    @GetMapping("/accounts-service/accounts/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccountDetails(@PathVariable String accountId) {
        log.info("Mock API: Getting account details for accountId: {}", accountId);

        Map<String, Object> account = MOCK_ACCOUNTS.get(accountId);

        if (account != null) {
            log.debug("Returning account details: {}", account);
            return ResponseEntity.ok(account);
        } else {
            // Return default account
            log.warn("Account not found, returning default for: {}", accountId);
            Map<String, Object> defaultAccount = new HashMap<>();
            defaultAccount.put("accountId", accountId);
            defaultAccount.put("lineOfBusiness", "credit_card");
            defaultAccount.put("accountType", "CREDIT_CARD");
            defaultAccount.put("creditLimit", 5000.00);
            return ResponseEntity.ok(defaultAccount);
        }
    }

    /**
     * GET /accounts-service/accounts/{accountId}/balance
     * Returns current account balance information.
     */
    @GetMapping("/accounts-service/accounts/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable String accountId) {
        log.info("Mock API: Getting account balance for accountId: {}", accountId);

        Map<String, Object> account = MOCK_ACCOUNTS.get(accountId);

        Map<String, Object> balanceInfo = new HashMap<>();
        balanceInfo.put("accountId", accountId);

        if (account != null) {
            balanceInfo.put("currentBalance", account.get("currentBalance"));
            balanceInfo.put("availableBalance", account.get("availableBalance"));
            balanceInfo.put("outstandingBalance", account.get("outstandingBalance"));
            balanceInfo.put("creditLimit", account.get("creditLimit"));
        } else {
            balanceInfo.put("currentBalance", 1000.00);
            balanceInfo.put("availableBalance", 1000.00);
            balanceInfo.put("creditLimit", 5000.00);
        }

        log.debug("Returning balance info: {}", balanceInfo);
        return ResponseEntity.ok(balanceInfo);
    }

    /**
     * GET /creditcard/accounts/{accountId}/arrangements
     * Returns account arrangements including pricing information.
     */
    @GetMapping("/creditcard/accounts/{accountId}/arrangements")
    public ResponseEntity<Map<String, Object>> getAccountArrangements(@PathVariable String accountId) {
        log.info("Mock API: Getting account arrangements for accountId: {}", accountId);

        Map<String, Object> arrangements = new HashMap<>();
        arrangements.put("accountId", accountId);

        Map<String, Object>[] content = new Map[2];

        // Pricing arrangement
        Map<String, Object> pricingArrangement = new HashMap<>();
        pricingArrangement.put("domain", "PRICING");
        pricingArrangement.put("domainId", "PRICING_" + accountId.substring(0, 8));
        pricingArrangement.put("status", "ACTIVE");
        content[0] = pricingArrangement;

        // Product arrangement
        Map<String, Object> productArrangement = new HashMap<>();
        productArrangement.put("domain", "PRODUCT");
        productArrangement.put("domainId", "PRODUCT_" + accountId.substring(0, 8));
        productArrangement.put("status", "ACTIVE");
        content[1] = productArrangement;

        arrangements.put("content", content);

        return ResponseEntity.ok(arrangements);
    }

    /**
     * GET /creditcard/accounts/{accountId}/product
     * Returns account product information.
     */
    @GetMapping("/creditcard/accounts/{accountId}/product")
    public ResponseEntity<Map<String, Object>> getAccountProduct(@PathVariable String accountId) {
        log.info("Mock API: Getting account product for accountId: {}", accountId);

        Map<String, Object> account = MOCK_ACCOUNTS.get(accountId);

        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("accountId", accountId);

        if (account != null) {
            productInfo.put("productCode", account.get("productCode"));
            productInfo.put("productFamily", account.get("productFamily"));
            productInfo.put("productName", account.get("productCode") + " Card");

            Map<String, Object> features = new HashMap<>();
            Map<String, Object> rewards = new HashMap<>();
            rewards.put("enabled", true);
            rewards.put("type", "CASHBACK");
            features.put("rewards", rewards);
            productInfo.put("features", features);
        } else {
            productInfo.put("productCode", "BASIC");
            productInfo.put("productFamily", "STANDARD_CARDS");
        }

        return ResponseEntity.ok(productInfo);
    }
}
