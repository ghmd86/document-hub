package com.example.eligibility.service;

import com.example.eligibility.model.DataContext;
import com.example.eligibility.model.EligibilityRequest;
import com.example.eligibility.model.EligibilityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Eligibility Service (Main Orchestrator)
 *
 * Orchestrates the entire eligibility check process.
 *
 * Flow:
 * 1. Receive eligibility request
 * 2. Fetch data from external APIs (DataFetcherService)
 * 3. Evaluate rules (RuleEvaluatorService)
 * 4. Return eligible documents
 *
 * This service is fully reactive except for the rule evaluation step,
 * which is offloaded to a separate thread pool.
 */
@Service
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    private final DataFetcherService dataFetcher;
    private final RuleEvaluatorService ruleEvaluator;
    private final Scheduler droolsScheduler;

    public EligibilityService(
            DataFetcherService dataFetcher,
            RuleEvaluatorService ruleEvaluator,
            @Qualifier("droolsScheduler") Scheduler droolsScheduler
    ) {
        this.dataFetcher = dataFetcher;
        this.ruleEvaluator = ruleEvaluator;
        this.droolsScheduler = droolsScheduler;
    }

    /**
     * Check document eligibility for a customer
     *
     * @param request Eligibility request (customerId, accountId, arrangementId)
     * @return Mono of EligibilityResponse
     */
    public Mono<EligibilityResponse> checkEligibility(EligibilityRequest request) {
        log.info("Starting eligibility check for: {}", request);

        long startTime = System.currentTimeMillis();

        // Prepare parameters for API calls
        Map<String, String> parameters = new HashMap<>();
        parameters.put("customerId", request.getCustomerId());
        parameters.put("accountId", request.getAccountId());
        parameters.put("arrangementId", request.getArrangementId());

        // Step 1: Fetch data from external APIs (reactive)
        return dataFetcher.fetchAllData(parameters)
                // Step 2: Evaluate rules (blocking, so offload to drools scheduler)
                .flatMap(dataContext ->
                        Mono.fromCallable(() -> ruleEvaluator.evaluateRules(dataContext))
                                .subscribeOn(droolsScheduler)
                                .timeout(Duration.ofMillis(500))
                )
                // Step 3: Build response
                .map(eligibleDocuments -> {
                    long evaluationTime = System.currentTimeMillis() - startTime;

                    EligibilityResponse response = new EligibilityResponse(
                            request.getCustomerId(),
                            request.getAccountId(),
                            eligibleDocuments,
                            evaluationTime
                    );

                    log.info("Eligibility check completed in {}ms. Eligible documents: {}",
                            evaluationTime, eligibleDocuments.size());

                    return response;
                })
                .onErrorResume(error -> {
                    log.error("Error during eligibility check: {}", error.getMessage());
                    return Mono.error(error);
                });
    }
}
