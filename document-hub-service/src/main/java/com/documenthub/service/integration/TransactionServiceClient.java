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
 * Client for interacting with Transaction Service API.
 * Provides methods to fetch transaction history and patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final WebClient webClient;

    @Value("${integration.transaction-service.base-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${integration.transaction-service.timeout:3000}")
    private int timeoutMs;

    /**
     * Get transaction summary for an account.
     */
    @Cacheable(value = "transactionSummary", key = "#accountId + '_' + #period", unless = "#result == null")
    public Mono<Map<String, Object>> getTransactionSummary(UUID accountId, String period) {
        log.debug("Fetching transaction summary for accountId: {} with period: {}", accountId, period);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/transaction-service/accounts/{accountId}/transactions/summary")
                        .queryParam("period", period)
                        .build(accountId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched transaction summary for: {}", accountId))
                .doOnError(error -> log.error("Error fetching transaction summary for {}: {}", accountId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch transaction summary, returning empty", error);
                    return Mono.empty();
                });
    }

    /**
     * Get international transaction history.
     */
    public Mono<Map<String, Object>> getInternationalTransactions(UUID accountId) {
        log.debug("Fetching international transactions for accountId: {}", accountId);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/transaction-service/accounts/{accountId}/transactions/summary")
                        .queryParam("period", "last_90_days")
                        .queryParam("type", "international")
                        .build(accountId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched international transactions for: {}", accountId))
                .doOnError(error -> log.error("Error fetching international transactions for {}: {}", accountId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch international transactions, returning empty", error);
                    return Mono.empty();
                });
    }
}
