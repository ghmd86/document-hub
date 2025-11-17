package com.example.eligibility.controller;

import com.example.eligibility.model.EligibilityRequest;
import com.example.eligibility.model.EligibilityResponse;
import com.example.eligibility.service.EligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Eligibility Controller
 *
 * REST API for checking document eligibility.
 *
 * Endpoints:
 * - GET /api/v1/eligibility - Check eligibility
 */
@RestController
@RequestMapping("/api/v1/eligibility")
public class EligibilityController {

    private static final Logger log = LoggerFactory.getLogger(EligibilityController.class);

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Check document eligibility
     *
     * GET /api/v1/eligibility?customerId=xxx&accountId=yyy&arrangementId=zzz
     *
     * @param customerId Customer ID
     * @param accountId Account ID
     * @param arrangementId Arrangement ID
     * @return Mono of EligibilityResponse
     */
    @GetMapping
    public Mono<ResponseEntity<EligibilityResponse>> checkEligibility(
            @RequestParam String customerId,
            @RequestParam String accountId,
            @RequestParam String arrangementId
    ) {
        log.info("Received eligibility check request: customerId={}, accountId={}, arrangementId={}",
                customerId, accountId, arrangementId);

        EligibilityRequest request = new EligibilityRequest(customerId, accountId, arrangementId);

        return eligibilityService.checkEligibility(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error processing eligibility request: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Health check endpoint
     *
     * @return OK status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("Eligibility Service is running"));
    }
}
