package com.example.droolspoc.controller;

import com.example.droolspoc.service.ConfigDrivenEligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * Configuration-Driven Eligibility Controller
 *
 * REST API endpoints for configuration-driven document eligibility.
 *
 * Example Request:
 * GET /api/v1/config-driven/eligibility?customerId=CUST123&accountId=ACC456&arrangementId=ARR789
 *
 * This will:
 * 1. Fetch data from ALL configured external APIs (chained and parallel)
 * 2. Evaluate ALL configured rules
 * 3. Return eligible document IDs
 *
 * NO CODE CHANGES needed to add new APIs or rules!
 */
@RestController
@RequestMapping("/api/v1/config-driven")
public class ConfigDrivenEligibilityController {

    private static final Logger log = LoggerFactory.getLogger(ConfigDrivenEligibilityController.class);

    private final ConfigDrivenEligibilityService eligibilityService;

    @Autowired
    public ConfigDrivenEligibilityController(ConfigDrivenEligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Get eligible documents using configuration-driven approach
     *
     * Example:
     * GET /api/v1/config-driven/eligibility?customerId=CUST123&accountId=ACC456&arrangementId=ARR789
     *
     * Response:
     * {
     *   "eligibleDocuments": [
     *     "DOC-TNC-GOLD-2024-BENEFITS",
     *     "DOC-HIGH-BALANCE-GOLD-EXCLUSIVE",
     *     "DOC-2024-TNC-UPDATE"
     *   ]
     * }
     */
    @GetMapping("/eligibility")
    public Mono<EligibilityResponse> getEligibleDocuments(
        @RequestParam String customerId,
        @RequestParam String accountId,
        @RequestParam String arrangementId
    ) {
        log.info("Config-driven eligibility request: customer={}, account={}, arrangement={}",
            customerId, accountId, arrangementId);

        return eligibilityService.getEligibleDocuments(customerId, accountId, arrangementId)
            .map(documents -> new EligibilityResponse(documents, documents.size()));
    }

    /**
     * Alternative endpoint (if pricingId is already known)
     *
     * Example:
     * GET /api/v1/config-driven/eligibility-by-pricing?customerId=CUST123&accountId=ACC456&pricingId=PRICING456
     */
    @GetMapping("/eligibility-by-pricing")
    public Mono<EligibilityResponse> getEligibleDocumentsByPricing(
        @RequestParam String customerId,
        @RequestParam String accountId,
        @RequestParam String pricingId
    ) {
        log.info("Config-driven eligibility request (by pricing): customer={}, account={}, pricing={}",
            customerId, accountId, pricingId);

        return eligibilityService.getEligibleDocumentsWithPricingId(customerId, accountId, pricingId)
            .map(documents -> new EligibilityResponse(documents, documents.size()));
    }

    /**
     * Debug endpoint: Get all data fetched from external APIs
     *
     * Example:
     * GET /api/v1/config-driven/debug/data-context?customerId=CUST123&accountId=ACC456&arrangementId=ARR789
     *
     * Response:
     * {
     *   "arrangements_api": {
     *     "pricingId": "PRICING456",
     *     "productCode": "GOLD_CARD",
     *     "arrangementStatus": "ACTIVE"
     *   },
     *   "cardholder_agreements_api": {
     *     "cardholderAgreementsTNCCode": "TNC_GOLD_2024",
     *     "tncEffectiveDate": "2024-01-01"
     *   },
     *   "account_service_api": {
     *     "accountBalance": 75000,
     *     "accountStatus": "ACTIVE",
     *     "accountType": "CREDIT_CARD"
     *   },
     *   "customer_service_api": {
     *     "customerTier": "GOLD",
     *     "creditScore": 780
     *   }
     * }
     */
    @GetMapping("/debug/data-context")
    public Mono<Map<String, Map<String, Object>>> getDataContext(
        @RequestParam String customerId,
        @RequestParam String accountId,
        @RequestParam String arrangementId
    ) {
        log.info("Debug: Fetching data context for customer={}, account={}, arrangement={}",
            customerId, accountId, arrangementId);

        return eligibilityService.getDataContext(customerId, accountId, arrangementId);
    }

    // ========================================================================
    // Response DTOs
    // ========================================================================

    public static class EligibilityResponse {
        private Set<String> eligibleDocuments;
        private int count;

        public EligibilityResponse(Set<String> eligibleDocuments, int count) {
            this.eligibleDocuments = eligibleDocuments;
            this.count = count;
        }

        public Set<String> getEligibleDocuments() {
            return eligibleDocuments;
        }

        public void setEligibleDocuments(Set<String> eligibleDocuments) {
            this.eligibleDocuments = eligibleDocuments;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
