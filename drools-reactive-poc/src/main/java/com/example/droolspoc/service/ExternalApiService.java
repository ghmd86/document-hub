package com.example.droolspoc.service;

import com.example.droolspoc.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * External API Service
 *
 * Handles all external API calls reactively:
 * 1. Fetch arrangement (to get pricingId)
 * 2. Fetch cardholder agreement (using pricingId to get TNC code)
 * 3. Fetch account details
 * 4. Fetch customer details
 *
 * All calls are non-blocking and use reactive streams.
 */
@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    private final WebClient arrangementsWebClient;
    private final WebClient cardholderAgreementsWebClient;
    private final WebClient accountServiceWebClient;
    private final WebClient customerServiceWebClient;

    @Autowired
    public ExternalApiService(
        @Qualifier("arrangementsWebClient") WebClient arrangementsWebClient,
        @Qualifier("cardholderAgreementsWebClient") WebClient cardholderAgreementsWebClient,
        @Qualifier("accountServiceWebClient") WebClient accountServiceWebClient,
        @Qualifier("customerServiceWebClient") WebClient customerServiceWebClient
    ) {
        this.arrangementsWebClient = arrangementsWebClient;
        this.cardholderAgreementsWebClient = cardholderAgreementsWebClient;
        this.accountServiceWebClient = accountServiceWebClient;
        this.customerServiceWebClient = customerServiceWebClient;
    }

    /**
     * Fetch arrangement details to get pricingId
     *
     * API: GET /api/v1/arrangements/{arrangementId}
     *
     * @param arrangementId Arrangement identifier
     * @return Mono of arrangement response containing pricingId
     */
    public Mono<ArrangementResponse> getArrangement(String arrangementId) {
        log.debug("Fetching arrangement: {}", arrangementId);

        return arrangementsWebClient
            .get()
            .uri("/api/v1/arrangements/{arrangementId}", arrangementId)
            .retrieve()
            .bodyToMono(ArrangementResponse.class)
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(response ->
                log.debug("Arrangement fetched: pricingId={}", response.getPricingId())
            )
            .doOnError(error ->
                log.error("Failed to fetch arrangement: {}", arrangementId, error)
            );
    }

    /**
     * Fetch cardholder agreement using pricingId
     *
     * API: GET /api/v1/cardholder-agreements/{pricingId}
     *
     * This is called AFTER getArrangement() to use the pricingId.
     *
     * @param pricingId Pricing identifier from arrangement
     * @return Mono of cardholder agreement response containing TNC code
     */
    public Mono<CardholderAgreementResponse> getCardholderAgreement(String pricingId) {
        log.debug("Fetching cardholder agreement for pricingId: {}", pricingId);

        return cardholderAgreementsWebClient
            .get()
            .uri("/api/v1/cardholder-agreements/{pricingId}", pricingId)
            .retrieve()
            .bodyToMono(CardholderAgreementResponse.class)
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(response ->
                log.debug("Cardholder agreement fetched: TNCCode={}",
                    response.getCardholderAgreementsTNCCode())
            )
            .doOnError(error ->
                log.error("Failed to fetch cardholder agreement for pricingId: {}", pricingId, error)
            );
    }

    /**
     * ⭐ CHAINED API CALLS: Fetch arrangement THEN cardholder agreement
     *
     * This demonstrates how to chain dependent API calls:
     * 1. Call arrangements API to get pricingId
     * 2. Use that pricingId to call cardholder agreements API
     *
     * Pattern: Use flatMap to chain dependent reactive calls
     *
     * @param arrangementId Arrangement identifier
     * @return Mono of cardholder agreement (after fetching arrangement first)
     */
    public Mono<CardholderAgreementResponse> getCardholderAgreementByArrangement(String arrangementId) {
        log.debug("Fetching cardholder agreement via arrangement: {}", arrangementId);

        return getArrangement(arrangementId)               // Step 1: Get arrangement
            .flatMap(arrangement -> {                      // Step 2: Use pricingId from arrangement
                String pricingId = arrangement.getPricingId();
                log.debug("Got pricingId from arrangement: {}", pricingId);
                return getCardholderAgreement(pricingId);  // Step 3: Get cardholder agreement
            })
            .doOnSuccess(agreement ->
                log.debug("Chained call completed: TNCCode={}",
                    agreement.getCardholderAgreementsTNCCode())
            );
    }

    /**
     * Fetch account details
     *
     * API: GET /api/v1/accounts/{accountId}
     *
     * @param accountId Account identifier
     * @return Mono of account fact
     */
    public Mono<AccountFact> getAccount(String accountId) {
        log.debug("Fetching account: {}", accountId);

        return accountServiceWebClient
            .get()
            .uri("/api/v1/accounts/{accountId}", accountId)
            .retrieve()
            .bodyToMono(AccountFact.class)
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(account ->
                log.debug("Account fetched: balance={}", account.getBalance())
            )
            .doOnError(error ->
                log.error("Failed to fetch account: {}", accountId, error)
            );
    }

    /**
     * Fetch customer details
     *
     * API: GET /api/v1/customers/{customerId}
     *
     * @param customerId Customer identifier
     * @return Mono of customer fact
     */
    public Mono<CustomerFact> getCustomer(String customerId) {
        log.debug("Fetching customer: {}", customerId);

        return customerServiceWebClient
            .get()
            .uri("/api/v1/customers/{customerId}", customerId)
            .retrieve()
            .bodyToMono(CustomerFact.class)
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(customer ->
                log.debug("Customer fetched: tier={}", customer.getTier())
            )
            .doOnError(error ->
                log.error("Failed to fetch customer: {}", customerId, error)
            );
    }

    /**
     * ⭐ COMPLETE DATA ASSEMBLY: Fetch all data needed for Drools rules
     *
     * This demonstrates fetching multiple pieces of data in parallel AND sequentially:
     * 1. SEQUENTIAL: Fetch arrangement → Use pricingId → Fetch cardholder agreement
     * 2. PARALLEL: Fetch account AND customer (independent calls)
     * 3. Combine all data into a single object
     *
     * @param customerId Customer ID
     * @param accountId Account ID
     * @param arrangementId Arrangement ID
     * @return Mono of complete data needed for Drools
     */
    public Mono<CompleteEligibilityData> assembleCompleteData(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        log.debug("Assembling complete data for customer={}, account={}, arrangement={}",
            customerId, accountId, arrangementId);

        // Step 1: Fetch arrangement → cardholder agreement (SEQUENTIAL)
        Mono<CardholderAgreementResponse> agreementMono =
            getCardholderAgreementByArrangement(arrangementId);

        // Step 2: Fetch account and customer (PARALLEL)
        Mono<AccountFact> accountMono = getAccount(accountId);
        Mono<CustomerFact> customerMono = getCustomer(customerId);

        // Step 3: Combine all results
        return Mono.zip(accountMono, customerMono, agreementMono)
            .map(tuple -> CompleteEligibilityData.builder()
                .account(tuple.getT1())
                .customer(tuple.getT2())
                .cardholderAgreementsTNCCode(tuple.getT3().getCardholderAgreementsTNCCode())
                .pricingId(tuple.getT3().getPricingId())
                .build()
            )
            .doOnSuccess(data ->
                log.debug("Complete data assembled: TNCCode={}",
                    data.getCardholderAgreementsTNCCode())
            );
    }
}
