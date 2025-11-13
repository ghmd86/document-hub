package com.example.droolspoc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration-Driven Document Eligibility Service
 *
 * This service demonstrates a FULLY CONFIGURATION-DRIVEN approach:
 *
 * ✅ Add new external APIs via YAML (no code changes)
 * ✅ Add new rules via YAML (no code changes)
 * ✅ Supports chained API calls (e.g., arrangements → cardholder agreements)
 * ✅ Supports complex AND/OR logic
 * ✅ Reactive (non-blocking)
 *
 * Flow:
 * 1. Fetch data from all configured external APIs (chained and parallel)
 * 2. Evaluate all configured rules against the data
 * 3. Return eligible document IDs
 *
 * Example Usage:
 * service.getEligibleDocuments("CUST123", "ACC456", "ARR789")
 *     → Fetches:
 *       - Arrangements API (using ARR789) → pricingId
 *       - Cardholder Agreements API (using pricingId) → TNC code
 *       - Account API (using ACC456) → balance, status, etc.
 *       - Customer API (using CUST123) → tier, credit score, etc.
 *     → Evaluates all rules
 *     → Returns: ["DOC-001", "DOC-002", ...]
 */
@Service
public class ConfigDrivenEligibilityService {

    private static final Logger log = LoggerFactory.getLogger(ConfigDrivenEligibilityService.class);

    private final DynamicDataFetcherService dataFetcher;
    private final ConfigDrivenRuleEvaluator ruleEvaluator;
    private final Scheduler ruleEvaluationScheduler;

    @Value("${drools.timeouts.rule-execution-ms:500}")
    private long ruleExecutionTimeoutMs;

    @Autowired
    public ConfigDrivenEligibilityService(
        DynamicDataFetcherService dataFetcher,
        ConfigDrivenRuleEvaluator ruleEvaluator,
        @Qualifier("droolsScheduler") Scheduler ruleEvaluationScheduler
    ) {
        this.dataFetcher = dataFetcher;
        this.ruleEvaluator = ruleEvaluator;
        this.ruleEvaluationScheduler = ruleEvaluationScheduler;
    }

    /**
     * Get eligible documents using configuration-driven approach
     *
     * @param customerId Customer ID
     * @param accountId Account ID
     * @param arrangementId Arrangement ID (for chained API call)
     * @return Mono of eligible document IDs
     */
    public Mono<Set<String>> getEligibleDocuments(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        log.info("Starting config-driven eligibility check: customer={}, account={}, arrangement={}",
            customerId, accountId, arrangementId);

        long startTime = System.currentTimeMillis();

        // Build input parameters for data fetching
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("accountId", accountId);
        params.put("arrangementId", arrangementId);

        // ⭐ STEP 1: Fetch all data from external APIs (reactive)
        // This handles:
        // - CHAINED calls: arrangements → cardholder agreements
        // - PARALLEL calls: account, customer
        return dataFetcher.fetchAllData(params)
            .flatMap(dataContext -> {
                long dataFetchTime = System.currentTimeMillis() - startTime;
                log.debug("All external data fetched in {}ms", dataFetchTime);
                log.debug("Data context sources: {}", dataContext.keySet());

                // ⭐ STEP 2: Evaluate all rules (on dedicated thread pool)
                // Rules are evaluated synchronously but on isolated thread pool
                // to avoid blocking the event loop
                return Mono.fromCallable(() -> ruleEvaluator.evaluateRules(dataContext))
                    .subscribeOn(ruleEvaluationScheduler)  // Execute on dedicated thread pool
                    .timeout(Duration.ofMillis(ruleExecutionTimeoutMs))
                    .onErrorResume(this::handleEvaluationError);
            })
            .doOnSuccess(documentIds -> {
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("Config-driven eligibility check completed in {}ms: {} documents eligible",
                    totalTime, documentIds.size());
            });
    }

    /**
     * Simplified version (if you already have pricingId)
     */
    public Mono<Set<String>> getEligibleDocumentsWithPricingId(
        String customerId,
        String accountId,
        String pricingId
    ) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("accountId", accountId);
        params.put("pricingId", pricingId);

        return dataFetcher.fetchAllData(params)
            .flatMap(dataContext ->
                Mono.fromCallable(() -> ruleEvaluator.evaluateRules(dataContext))
                    .subscribeOn(ruleEvaluationScheduler)
                    .timeout(Duration.ofMillis(ruleExecutionTimeoutMs))
            );
    }

    /**
     * Error handling for rule evaluation failures
     */
    private Mono<Set<String>> handleEvaluationError(Throwable error) {
        log.error("Rule evaluation failed", error);
        return Mono.error(new RuntimeException("Rule evaluation failed: " + error.getMessage(), error));
    }

    /**
     * Get data context for debugging (without rule evaluation)
     */
    public Mono<Map<String, Map<String, Object>>> getDataContext(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("accountId", accountId);
        params.put("arrangementId", arrangementId);

        return dataFetcher.fetchAllData(params);
    }
}
