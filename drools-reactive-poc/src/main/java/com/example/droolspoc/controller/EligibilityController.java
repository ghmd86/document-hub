package com.example.droolspoc.controller;

import com.example.droolspoc.model.EligibilityRequest;
import com.example.droolspoc.model.EligibilityResponse;
import com.example.droolspoc.service.ReactiveDroolsEligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Eligibility REST Controller
 *
 * Demonstrates reactive REST endpoints with Drools integration.
 *
 * Endpoints:
 * - GET  /api/health - Health check
 * - POST /api/eligibility - Check document eligibility
 */
@RestController
@RequestMapping("/api")
public class EligibilityController {

    private static final Logger log = LoggerFactory.getLogger(EligibilityController.class);

    private final ReactiveDroolsEligibilityService eligibilityService;

    @Autowired
    public EligibilityController(ReactiveDroolsEligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Health check endpoint
     *
     * GET /api/health
     */
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK - Drools Reactive POC is running");
    }

    /**
     * Check document eligibility
     *
     * POST /api/eligibility
     * Body: {"customerId": "CUST123", "accountId": "ACC456"}
     *
     * This demonstrates a fully reactive request/response flow:
     * 1. Request received on event loop (non-blocking)
     * 2. Data fetched reactively (parallel R2DBC queries)
     * 3. Drools executed on dedicated thread pool
     * 4. Response returned reactively
     */
    @PostMapping(
        value = "/eligibility",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<EligibilityResponse> checkEligibility(@RequestBody EligibilityRequest request) {
        log.info("Received eligibility request: customerId={}, accountId={}",
            request.getCustomerId(), request.getAccountId());

        long startTime = System.currentTimeMillis();

        return eligibilityService.getEligibleDocuments(
                request.getCustomerId(),
                request.getAccountId()
            )
            .map(documentIds -> {
                long executionTime = System.currentTimeMillis() - startTime;

                return EligibilityResponse.builder()
                    .customerId(request.getCustomerId())
                    .accountId(request.getAccountId())
                    .eligibleDocumentIds(documentIds)
                    .eligibleCount(documentIds.size())
                    .executionTimeMs(executionTime)
                    .evaluatedAt(Instant.now())
                    .build();
            })
            .doOnSuccess(response ->
                log.info("Eligibility check completed: {} documents eligible in {}ms",
                    response.getEligibleCount(), response.getExecutionTimeMs())
            )
            .timeout(Duration.ofSeconds(2)); // Overall timeout
    }
}
