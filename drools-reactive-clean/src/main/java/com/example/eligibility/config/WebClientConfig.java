package com.example.eligibility.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient Configuration
 *
 * Configures WebClient for making reactive HTTP calls to external APIs.
 *
 * Features:
 * - Connection timeout
 * - Read timeout
 * - Response timeout
 * - Connection pooling
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient Bean
     *
     * Configured for calling external APIs with proper timeouts.
     *
     * @return WebClient.Builder instance
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .compress(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
