package com.documenthub.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Client for interacting with Account Service API.
 * Provides methods to fetch account details, balance, and arrangements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceClient {

    private final WebClient webClient;

    @Value("${integration.account-service.base-url:http://localhost:8082}")
    private String baseUrl;

    @Value("${integration.account-service.timeout:3000}")
    private int timeoutMs;

    /**
     * Get account details including line of business, product type, etc.
     */
    @Cacheable(value = "accountDetails", key = "#accountId", unless = "#result == null")
    public Mono<Map<String, Object>> getAccountDetails(UUID accountId) {
        log.debug("Fetching account details for accountId: {}", accountId);

        return webClient
                .get()
                .uri(baseUrl + "/accounts-service/accounts/{accountId}", accountId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched account details for: {}", accountId))
                .doOnError(error -> log.error("Error fetching account details for {}: {}", accountId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch account details, returning empty", error);
                    return Mono.empty();
                });
    }

    /**
     * Get account balance information.
     */
    @Cacheable(value = "accountBalance", key = "#accountId", unless = "#result == null")
    public Mono<Map<String, Object>> getAccountBalance(UUID accountId) {
        log.debug("Fetching account balance for accountId: {}", accountId);

        return webClient
                .get()
                .uri(baseUrl + "/accounts-service/accounts/{accountId}/balance", accountId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched account balance for: {}", accountId))
                .doOnError(error -> log.error("Error fetching account balance for {}: {}", accountId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch account balance, returning empty", error);
                    return Mono.empty();
                });
    }

    /**
     * Get account arrangements (pricing, product arrangements).
     */
    @Cacheable(value = "accountArrangements", key = "#accountId", unless = "#result == null")
    public Mono<Map<String, Object>> getAccountArrangements(UUID accountId) {
        log.debug("Fetching account arrangements for accountId: {}", accountId);

        return webClient
                .get()
                .uri(baseUrl + "/creditcard/accounts/{accountId}/arrangements", accountId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched account arrangements for: {}", accountId))
                .doOnError(error -> log.error("Error fetching account arrangements for {}: {}", accountId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch account arrangements, returning empty", error);
                    return Mono.empty();
                });
    }

    /**
     * Get account product information.
     */
    @Cacheable(value = "accountProduct", key = "#accountId", unless = "#result == null")
    public Mono<Map<String, Object>> getAccountProduct(UUID accountId) {
        log.debug("Fetching account product for accountId: {}", accountId);

        return webClient
                .get()
                .uri(baseUrl + "/creditcard/accounts/{accountId}/product", accountId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched account product for: {}", accountId))
                .doOnError(error -> log.error("Error fetching account product for {}: {}", accountId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch account product, returning empty", error);
                    return Mono.empty();
                });
    }
}
