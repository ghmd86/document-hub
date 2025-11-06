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
 * Client for interacting with Customer Service API.
 * Provides methods to fetch customer profile and related data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceClient {

    private final WebClient webClient;

    @Value("${integration.customer-service.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${integration.customer-service.timeout:3000}")
    private int timeoutMs;

    /**
     * Get customer profile including address, segment, customer type, etc.
     */
    @Cacheable(value = "customerProfile", key = "#customerId", unless = "#result == null")
    public Mono<Map<String, Object>> getCustomerProfile(UUID customerId) {
        log.debug("Fetching customer profile for customerId: {}", customerId);

        return webClient
                .get()
                .uri(baseUrl + "/customer-service/customers/{customerId}/profile", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched customer profile for: {}", customerId))
                .doOnError(error -> log.error("Error fetching customer profile for {}: {}", customerId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch customer profile, returning empty", error);
                    return Mono.empty();
                });
    }

    /**
     * Get customer segment information.
     */
    @Cacheable(value = "customerSegment", key = "#customerId", unless = "#result == null")
    public Mono<Map<String, Object>> getCustomerSegment(UUID customerId) {
        log.debug("Fetching customer segment for customerId: {}", customerId);

        return webClient
                .get()
                .uri(baseUrl + "/customer-service/customers/{customerId}/segment", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched customer segment for: {}", customerId))
                .doOnError(error -> log.error("Error fetching customer segment for {}: {}", customerId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch customer segment, returning empty", error);
                    return Mono.empty();
                });
    }

    /**
     * Get customer address details.
     */
    public Mono<Map<String, Object>> getCustomerAddress(UUID customerId) {
        log.debug("Fetching customer address for customerId: {}", customerId);

        return webClient
                .get()
                .uri(baseUrl + "/customer-service/customers/{customerId}/address", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Successfully fetched customer address for: {}", customerId))
                .doOnError(error -> log.error("Error fetching customer address for {}: {}", customerId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to fetch customer address, returning empty", error);
                    return Mono.empty();
                });
    }
}
