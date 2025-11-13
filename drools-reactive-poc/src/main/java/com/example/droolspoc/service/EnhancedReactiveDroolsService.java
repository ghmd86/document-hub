package com.example.droolspoc.service;

import com.example.droolspoc.model.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Enhanced Reactive Drools Service
 *
 * Demonstrates how to integrate external API calls with Drools:
 *
 * Flow:
 * 1. Fetch data from multiple external APIs (chained and parallel)
 *    - Arrangements API → pricingId
 *    - Cardholder Agreements API (using pricingId) → TNC code
 *    - Account Service API → account details
 *    - Customer Service API → customer details
 *
 * 2. Assemble all data into fact objects
 *
 * 3. Execute Drools rules on dedicated thread pool
 *
 * 4. Return results reactively
 *
 * Key Pattern:
 * - External API calls are REACTIVE (non-blocking)
 * - Drools execution is BLOCKING (but on dedicated thread pool)
 * - Everything is orchestrated with Mono/Flux reactive streams
 */
@Service
public class EnhancedReactiveDroolsService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedReactiveDroolsService.class);

    private final KieContainer kieContainer;
    private final Scheduler droolsScheduler;
    private final ExternalApiService externalApiService;

    @Value("${drools.timeouts.rule-execution-ms:500}")
    private long ruleExecutionTimeoutMs;

    @Autowired
    public EnhancedReactiveDroolsService(
        KieContainer kieContainer,
        @Qualifier("droolsScheduler") Scheduler droolsScheduler,
        ExternalApiService externalApiService
    ) {
        this.kieContainer = kieContainer;
        this.droolsScheduler = droolsScheduler;
        this.externalApiService = externalApiService;
    }

    /**
     * ⭐ COMPLETE EXAMPLE: External APIs + Drools
     *
     * This method demonstrates the complete flow:
     *
     * 1. REACTIVE DATA FETCHING (with chained and parallel calls):
     *    - Get arrangement → extract pricingId → get cardholder agreement (SEQUENTIAL)
     *    - Get account and customer (PARALLEL with above)
     *
     * 2. ASSEMBLE FACTS:
     *    - Combine all API responses into fact objects
     *
     * 3. EXECUTE DROOLS:
     *    - Run rules on dedicated thread pool (blocking, but isolated)
     *
     * 4. RETURN RESULTS:
     *    - Reactive Mono<Set<String>> with eligible documents
     *
     * @param customerId Customer ID
     * @param accountId Account ID
     * @param arrangementId Arrangement ID (used to fetch pricingId)
     * @return Mono of eligible document IDs
     */
    public Mono<Set<String>> getEligibleDocumentsWithExternalApis(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        log.info("Starting eligibility check with external APIs: customer={}, account={}, arrangement={}",
            customerId, accountId, arrangementId);

        long startTime = System.currentTimeMillis();

        // ⭐ STEP 1: Fetch all data from external APIs
        // This includes CHAINED calls (arrangement → cardholder agreement)
        // and PARALLEL calls (account, customer)
        return externalApiService.assembleCompleteData(customerId, accountId, arrangementId)
            .flatMap(data -> {
                long dataFetchTime = System.currentTimeMillis() - startTime;
                log.debug("External API data fetched in {}ms", dataFetchTime);
                log.debug("  - TNC Code: {}", data.getCardholderAgreementsTNCCode());
                log.debug("  - Pricing ID: {}", data.getPricingId());

                // ⭐ STEP 2: Create enhanced fact objects with all data
                EnhancedAccountFact enhancedAccount = EnhancedAccountFact.from(data.getAccount(), data);
                CustomerFact customer = data.getCustomer();

                // ⭐ STEP 3: Execute Drools on dedicated thread pool
                return Mono.fromCallable(() -> executeRules(enhancedAccount, customer))
                    .subscribeOn(droolsScheduler)  // Isolated thread pool
                    .timeout(Duration.ofMillis(ruleExecutionTimeoutMs))
                    .onErrorResume(this::handleRuleExecutionError);
            })
            .doOnSuccess(documentIds -> {
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("Eligibility check with external APIs completed in {}ms: {} documents eligible",
                    totalTime, documentIds.size());
            });
    }

    /**
     * Alternative: Fetch data in parallel (no chaining)
     *
     * Use this if you already have pricingId and don't need to fetch it from arrangements.
     */
    public Mono<Set<String>> getEligibleDocumentsWithPricingId(
        String customerId,
        String accountId,
        String pricingId
    ) {
        log.info("Starting eligibility check with pricingId: customer={}, account={}, pricingId={}",
            customerId, accountId, pricingId);

        // Fetch all data in PARALLEL (no dependency chain)
        Mono<AccountFact> accountMono = externalApiService.getAccount(accountId);
        Mono<CustomerFact> customerMono = externalApiService.getCustomer(customerId);
        Mono<CardholderAgreementResponse> agreementMono =
            externalApiService.getCardholderAgreement(pricingId);

        return Mono.zip(accountMono, customerMono, agreementMono)
            .flatMap(tuple -> {
                // Create enhanced account fact
                EnhancedAccountFact enhancedAccount = EnhancedAccountFact.builder()
                    .accountId(tuple.getT1().getAccountId())
                    .balance(tuple.getT1().getBalance())
                    .status(tuple.getT1().getStatus())
                    .accountType(tuple.getT1().getAccountType())
                    .creditLimit(tuple.getT1().getCreditLimit())
                    .state(tuple.getT1().getState())
                    .pricingId(pricingId)
                    .cardholderAgreementsTNCCode(tuple.getT3().getCardholderAgreementsTNCCode())
                    .build();

                // Execute Drools
                return Mono.fromCallable(() -> executeRules(enhancedAccount, tuple.getT2()))
                    .subscribeOn(droolsScheduler)
                    .timeout(Duration.ofMillis(ruleExecutionTimeoutMs));
            });
    }

    /**
     * Execute Drools rules with enhanced account fact
     *
     * The enhanced account includes:
     * - Basic account data
     * - pricingId (from Arrangements API)
     * - cardholderAgreementsTNCCode (from Cardholder Agreements API)
     *
     * Rules can now use these fields for eligibility checks.
     */
    private Set<String> executeRules(EnhancedAccountFact account, CustomerFact customer) {
        long startTime = System.currentTimeMillis();

        KieSession kieSession = kieContainer.newKieSession();

        try {
            log.debug("Executing Drools rules with enhanced facts on thread: {}",
                Thread.currentThread().getName());

            DocumentEligibilityResult result = new DocumentEligibilityResult();

            // Insert facts into working memory
            kieSession.insert(account);   // Enhanced account with TNC code
            kieSession.insert(customer);
            kieSession.insert(result);

            // Fire all rules
            int rulesFired = kieSession.fireAllRules();

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("Drools execution: {} rules fired in {}ms", rulesFired, executionTime);

            return result.getEligibleDocumentIds();

        } finally {
            kieSession.dispose();
        }
    }

    /**
     * Error handling for rule execution failures
     */
    private Mono<Set<String>> handleRuleExecutionError(Throwable error) {
        if (error instanceof TimeoutException) {
            log.error("Drools rule execution timed out after {}ms", ruleExecutionTimeoutMs);
            return Mono.error(new RuntimeException("Rule evaluation exceeded timeout", error));
        }

        log.error("Drools rule execution failed", error);
        return Mono.error(new RuntimeException("Rule evaluation failed", error));
    }
}
