package com.example.droolspoc.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration for External API Calls
 *
 * Configures WebClient beans for calling external APIs reactively:
 * - Arrangements API (to fetch pricing ID)
 * - Cardholder Agreements API (to fetch TNC code using pricing ID)
 * - Account Service API
 * - Customer Service API
 *
 * All calls are non-blocking and use reactive streams.
 */
@Configuration
public class WebClientConfig {

    @Value("${external-api.arrangements.base-url:http://localhost:8081}")
    private String arrangementsBaseUrl;

    @Value("${external-api.cardholder-agreements.base-url:http://localhost:8082}")
    private String cardholderAgreementsBaseUrl;

    @Value("${external-api.account-service.base-url:http://localhost:8083}")
    private String accountServiceBaseUrl;

    @Value("${external-api.customer-service.base-url:http://localhost:8084}")
    private String customerServiceBaseUrl;

    @Value("${external-api.timeout.connection-ms:5000}")
    private int connectionTimeoutMs;

    @Value("${external-api.timeout.read-ms:10000}")
    private int readTimeoutMs;

    @Value("${external-api.timeout.write-ms:10000}")
    private int writeTimeoutMs;

    /**
     * Create base HttpClient with timeouts
     */
    private HttpClient createHttpClient() {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))
            )
            .responseTimeout(Duration.ofMillis(readTimeoutMs));
    }

    /**
     * WebClient for Arrangements API
     *
     * Used to fetch pricingId from arrangements
     * Example: GET /api/v1/arrangements/{arrangementId}
     */
    @Bean(name = "arrangementsWebClient")
    public WebClient arrangementsWebClient() {
        return WebClient.builder()
            .baseUrl(arrangementsBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build();
    }

    /**
     * WebClient for Cardholder Agreements API
     *
     * Used to fetch cardholderAgreementsTNCCode using pricingId
     * Example: GET /api/v1/cardholder-agreements/{pricingId}
     */
    @Bean(name = "cardholderAgreementsWebClient")
    public WebClient cardholderAgreementsWebClient() {
        return WebClient.builder()
            .baseUrl(cardholderAgreementsBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build();
    }

    /**
     * WebClient for Account Service API
     *
     * Used to fetch account details
     * Example: GET /api/v1/accounts/{accountId}
     */
    @Bean(name = "accountServiceWebClient")
    public WebClient accountServiceWebClient() {
        return WebClient.builder()
            .baseUrl(accountServiceBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build();
    }

    /**
     * WebClient for Customer Service API
     *
     * Used to fetch customer details
     * Example: GET /api/v1/customers/{customerId}
     */
    @Bean(name = "customerServiceWebClient")
    public WebClient customerServiceWebClient() {
        return WebClient.builder()
            .baseUrl(customerServiceBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build();
    }
}
