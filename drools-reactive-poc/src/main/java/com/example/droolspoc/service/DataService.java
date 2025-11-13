package com.example.droolspoc.service;

import com.example.droolspoc.model.AccountFact;
import com.example.droolspoc.model.CustomerFact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;

/**
 * Data Service - Simulates reactive data fetching
 *
 * In a real application, this would fetch data from:
 * - R2DBC database (reactive)
 * - WebClient HTTP calls (reactive)
 * - Other reactive sources
 *
 * This POC simulates realistic latency to demonstrate performance.
 */
@Service
public class DataService {

    private static final Logger log = LoggerFactory.getLogger(DataService.class);
    private final Random random = new Random();

    /**
     * Fetch account data reactively (simulated)
     *
     * Simulates 20-50ms database query latency
     */
    public Mono<AccountFact> getAccountFact(String accountId) {
        return Mono.fromCallable(() -> {
                log.debug("Fetching account data for: {}", accountId);

                // Simulate different account types
                return AccountFact.builder()
                    .accountId(accountId)
                    .balance(new BigDecimal("15000.00"))
                    .status("ACTIVE")
                    .accountType("CREDIT_CARD")
                    .creditLimit(new BigDecimal("25000.00"))
                    .state("CA")
                    .build();
            })
            .delayElement(Duration.ofMillis(20 + random.nextInt(30))) // 20-50ms
            .doOnSuccess(account ->
                log.debug("Account data fetched: balance={}", account.getBalance())
            );
    }

    /**
     * Fetch customer data reactively (simulated)
     *
     * Simulates 20-50ms database query latency
     */
    public Mono<CustomerFact> getCustomerFact(String customerId) {
        return Mono.fromCallable(() -> {
                log.debug("Fetching customer data for: {}", customerId);

                return CustomerFact.builder()
                    .customerId(customerId)
                    .tier("GOLD")
                    .enrollmentDate(LocalDate.of(2020, 1, 15))
                    .creditScore(750)
                    .state("CA")
                    .age(35)
                    .build();
            })
            .delayElement(Duration.ofMillis(20 + random.nextInt(30))) // 20-50ms
            .doOnSuccess(customer ->
                log.debug("Customer data fetched: tier={}", customer.getTier())
            );
    }
}
