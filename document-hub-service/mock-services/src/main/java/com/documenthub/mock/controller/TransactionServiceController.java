package com.documenthub.mock.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock Transaction Service API.
 * Provides transaction history and pattern information.
 */
@Slf4j
@RestController
@RequestMapping("/transaction-service/accounts")
public class TransactionServiceController {

    /**
     * GET /transaction-service/accounts/{accountId}/transactions/summary
     * Returns transaction summary including international activity.
     */
    @GetMapping("/{accountId}/transactions/summary")
    public ResponseEntity<Map<String, Object>> getTransactionSummary(
            @PathVariable String accountId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String type) {

        log.info("Mock API: Getting transaction summary for accountId: {}, period: {}, type: {}",
                accountId, period, type);

        Map<String, Object> summary = new HashMap<>();
        summary.put("accountId", accountId);
        summary.put("period", period != null ? period : "last_90_days");

        // Different data based on account
        boolean hasInternationalActivity = accountId.contains("440001") || accountId.contains("440003");

        if ("international".equals(type)) {
            Map<String, Object> internationalTransactions = new HashMap<>();
            internationalTransactions.put("count", hasInternationalActivity ? 15 : 0);
            internationalTransactions.put("totalAmount", hasInternationalActivity ? 2500.00 : 0.00);
            internationalTransactions.put("hasActivity", hasInternationalActivity);

            summary.put("internationalTransactions", internationalTransactions);
        } else {
            // General summary
            summary.put("totalTransactionCount", 125);
            summary.put("totalAmount", 8500.00);
            summary.put("averageTransactionAmount", 68.00);

            Map<String, Object> internationalTransactions = new HashMap<>();
            internationalTransactions.put("count", hasInternationalActivity ? 15 : 0);
            internationalTransactions.put("totalAmount", hasInternationalActivity ? 2500.00 : 0.00);
            internationalTransactions.put("hasActivity", hasInternationalActivity);

            Map<String, Object> domesticTransactions = new HashMap<>();
            domesticTransactions.put("count", 110);
            domesticTransactions.put("totalAmount", 6000.00);

            summary.put("internationalTransactions", internationalTransactions);
            summary.put("domesticTransactions", domesticTransactions);

            // Top categories
            Map<String, Object>[] topCategories = new Map[3];

            Map<String, Object> cat1 = new HashMap<>();
            cat1.put("category", "DINING");
            cat1.put("count", 45);
            cat1.put("amount", 3200.00);
            topCategories[0] = cat1;

            Map<String, Object> cat2 = new HashMap<>();
            cat2.put("category", "SHOPPING");
            cat2.put("count", 38);
            cat2.put("amount", 2800.00);
            topCategories[1] = cat2;

            Map<String, Object> cat3 = new HashMap<>();
            cat3.put("category", "TRAVEL");
            cat3.put("count", 18);
            cat3.put("amount", 1500.00);
            topCategories[2] = cat3;

            summary.put("topCategories", topCategories);
        }

        log.debug("Returning transaction summary: {}", summary);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /transaction-service/accounts/{accountId}/transactions/recent
     * Returns recent transactions.
     */
    @GetMapping("/{accountId}/transactions/recent")
    public ResponseEntity<Map<String, Object>> getRecentTransactions(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Mock API: Getting recent transactions for accountId: {}, limit: {}", accountId, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("count", limit);

        Map<String, Object>[] transactions = new Map[Math.min(limit, 5)];

        for (int i = 0; i < transactions.length; i++) {
            Map<String, Object> txn = new HashMap<>();
            txn.put("transactionId", "TXN-" + accountId.substring(0, 8) + "-" + (i + 1));
            txn.put("date", System.currentTimeMillis() / 1000 - (i * 86400));
            txn.put("amount", 50.00 + (i * 25));
            txn.put("merchant", "Merchant " + (i + 1));
            txn.put("category", i % 2 == 0 ? "DINING" : "SHOPPING");
            txn.put("type", i == 0 && accountId.contains("440001") ? "INTERNATIONAL" : "DOMESTIC");
            transactions[i] = txn;
        }

        response.put("transactions", transactions);

        return ResponseEntity.ok(response);
    }
}
