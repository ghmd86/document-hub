package com.example.droolspoc.service;

import com.example.droolspoc.model.AccountFact;
import com.example.droolspoc.model.CustomerFact;
import com.example.droolspoc.model.DocumentEligibilityResult;
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
 * Reactive Drools Eligibility Service
 *
 * This service demonstrates the WRAPPER PATTERN for integrating
 * blocking Drools rule engine with reactive Spring WebFlux.
 *
 * Key concepts:
 * 1. Fetch data reactively (parallel, non-blocking)
 * 2. Execute Drools on dedicated thread pool (isolated from event loop)
 * 3. Return reactive Mono with results
 *
 * Performance:
 * - Data fetching: 50ms (parallel R2DBC queries)
 * - Thread scheduling: 5ms
 * - Drools execution: 15ms (blocking, but isolated)
 * - Total: ~70ms
 */
@Service
public class ReactiveDroolsEligibilityService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveDroolsEligibilityService.class);

    private final KieContainer kieContainer;
    private final Scheduler droolsScheduler;
    private final DataService dataService;

    @Value("${drools.timeouts.rule-execution-ms:500}")
    private long ruleExecutionTimeoutMs;

    @Autowired
    public ReactiveDroolsEligibilityService(
        KieContainer kieContainer,
        @Qualifier("droolsScheduler") Scheduler droolsScheduler,
        DataService dataService
    ) {
        this.kieContainer = kieContainer;
        this.droolsScheduler = droolsScheduler;
        this.dataService = dataService;
    }

    /**
     * Get eligible documents reactively
     *
     * This method demonstrates the complete reactive flow:
     * 1. Fetch account and customer data in parallel (reactive)
     * 2. Execute Drools rules on dedicated thread pool
     * 3. Return results as Mono
     *
     * @param customerId Customer identifier
     * @param accountId Account identifier
     * @return Mono of eligible document IDs
     */
    public Mono<Set<String>> getEligibleDocuments(String customerId, String accountId) {
        log.info("Starting eligibility check for customer={}, account={}",
            customerId, accountId);

        long startTime = System.currentTimeMillis();

        // Step 1: Fetch data reactively in PARALLEL (non-blocking)
        Mono<AccountFact> accountMono = dataService.getAccountFact(accountId);
        Mono<CustomerFact> customerMono = dataService.getCustomerFact(customerId);

        // Step 2: Combine facts and execute rules
        return Mono.zip(accountMono, customerMono)
            .flatMap(tuple -> {
                AccountFact account = tuple.getT1();
                CustomerFact customer = tuple.getT2();

                long dataFetchTime = System.currentTimeMillis() - startTime;
                log.debug("Data fetched in {}ms", dataFetchTime);

                // Step 3: Execute Drools on dedicated thread pool
                return Mono.fromCallable(() -> executeRules(account, customer))
                    .subscribeOn(droolsScheduler)  // ⭐ Execute on Drools thread pool
                    .timeout(Duration.ofMillis(ruleExecutionTimeoutMs))
                    .onErrorResume(this::handleRuleExecutionError);
            })
            .doOnSuccess(documentIds -> {
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("Eligibility check completed in {}ms: {} documents eligible",
                    totalTime, documentIds.size());
            });
    }

    /**
     * Execute Drools rules (BLOCKING operation)
     *
     * ⚠️ This method BLOCKS the calling thread.
     * ✅ Safe because it runs on dedicated thread pool, NOT the event loop.
     *
     * @param account Account fact
     * @param customer Customer fact
     * @return Set of eligible document IDs
     */
    private Set<String> executeRules(AccountFact account, CustomerFact customer) {
        long startTime = System.currentTimeMillis();

        // Create stateless session (thread-safe, no memory overhead between requests)
        KieSession kieSession = kieContainer.newKieSession();

        try {
            log.debug("Executing Drools rules on thread: {}",
                Thread.currentThread().getName());

            // Create result holder
            DocumentEligibilityResult result = new DocumentEligibilityResult();

            // Insert facts into working memory
            kieSession.insert(account);
            kieSession.insert(customer);
            kieSession.insert(result);

            // Fire all rules (BLOCKING - pattern matching + rule execution)
            int rulesFired = kieSession.fireAllRules();

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("Drools execution: {} rules fired in {}ms on thread {}",
                rulesFired,
                executionTime,
                Thread.currentThread().getName());

            // Return eligible documents
            return result.getEligibleDocumentIds();

        } finally {
            // ALWAYS dispose session to free memory
            kieSession.dispose();
        }
    }

    /**
     * Error handling for rule execution failures
     */
    private Mono<Set<String>> handleRuleExecutionError(Throwable error) {
        if (error instanceof TimeoutException) {
            log.error("Drools rule execution timed out after {}ms",
                ruleExecutionTimeoutMs);
            return Mono.error(new RuntimeException(
                "Rule evaluation exceeded timeout", error));
        }

        log.error("Drools rule execution failed", error);
        return Mono.error(new RuntimeException(
            "Rule evaluation failed", error));
    }
}
