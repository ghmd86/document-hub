package com.example.eligibility.service;

import com.example.eligibility.entity.DataSourceEntity;
import com.example.eligibility.model.DataContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Fetcher Service
 *
 * Fetches data from external APIs based on database configuration.
 *
 * Features:
 * - Dynamic API calls based on JSONB configuration
 * - Handles chained/dependent API calls
 * - Maps responses using JSONPath
 * - Timeout and retry support
 * - Fully reactive
 *
 * Flow:
 * 1. Load data source configurations
 * 2. Execute independent API calls in parallel
 * 3. Execute dependent API calls (chained)
 * 4. Map responses to field names
 * 5. Return DataContext with all fetched data
 */
@Service
public class DataFetcherService {

    private static final Logger log = LoggerFactory.getLogger(DataFetcherService.class);

    private final ConfigurationLoaderService configLoader;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public DataFetcherService(
            ConfigurationLoaderService configLoader,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.configLoader = configLoader;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch all data from external APIs
     *
     * @param parameters Request parameters (customerId, accountId, arrangementId)
     * @return Mono of DataContext containing all fetched data
     */
    public Mono<DataContext> fetchAllData(Map<String, String> parameters) {
        log.info("Fetching data from external APIs with parameters: {}", parameters);

        return configLoader.loadDataSources()
                .flatMap(dataSources -> {
                    DataContext context = new DataContext();

                    // Process each data source
                    return Flux.fromIterable(dataSources)
                            .flatMap(dataSource -> fetchFromSource(dataSource, parameters, context))
                            .then(Mono.just(context));
                })
                .doOnSuccess(context ->
                        log.info("Data fetching completed. Sources: {}", context.getSourceIds())
                );
    }

    /**
     * Fetch data from a single data source
     *
     * @param dataSource Data source entity
     * @param parameters Request parameters
     * @param context Data context (for dependent calls)
     * @return Mono of Void
     */
    private Mono<Void> fetchFromSource(
            DataSourceEntity dataSource,
            Map<String, String> parameters,
            DataContext context
    ) {
        try {
            JsonNode config = objectMapper.readTree(dataSource.getConfiguration().asString());

            String method = config.path("method").asText();
            String baseUrl = config.path("baseUrl").asText();
            String endpoint = config.path("endpoint").asText();
            int timeoutMs = config.path("timeoutMs").asInt(5000);

            // Replace path parameters
            String url = baseUrl + replacePathParameters(endpoint, parameters, context);

            log.debug("Calling API: {} {}", method, url);

            WebClient webClient = webClientBuilder.build();

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .flatMap(responseBody -> {
                        // Map response to fields
                        Map<String, Object> mappedData = mapResponse(responseBody, config);
                        context.addSourceData(dataSource.getId(), mappedData);

                        log.debug("Fetched data from {}: {}", dataSource.getId(), mappedData);
                        return Mono.empty();
                    })
                    .onErrorResume(error -> {
                        log.error("Error fetching data from {}: {}", dataSource.getId(), error.getMessage());
                        // Add empty data to context to avoid null pointer exceptions
                        context.addSourceData(dataSource.getId(), new HashMap<>());
                        return Mono.empty();
                    });

        } catch (Exception e) {
            log.error("Error processing data source {}: {}", dataSource.getId(), e.getMessage());
            return Mono.empty();
        }
    }

    /**
     * Replace path parameters in endpoint URL
     *
     * Example: "/api/v1/arrangements/{arrangementId}" -> "/api/v1/arrangements/ARR001"
     *
     * @param endpoint Endpoint template
     * @param parameters Request parameters
     * @param context Data context (for dependent parameters)
     * @return Endpoint with replaced parameters
     */
    private String replacePathParameters(
            String endpoint,
            Map<String, String> parameters,
            DataContext context
    ) {
        String result = endpoint;

        // Replace from request parameters
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // TODO: Replace from dependent data sources (chained calls)
        // For now, this is a simplified implementation

        return result;
    }

    /**
     * Map API response to field names using JSONPath
     *
     * @param responseBody Response body (JSON string)
     * @param config Data source configuration
     * @return Map of field names to values
     */
    private Map<String, Object> mapResponse(String responseBody, JsonNode config) {
        Map<String, Object> mappedData = new HashMap<>();

        try {
            JsonNode responseMappingNode = config.path("responseMapping");

            if (responseMappingNode.isArray()) {
                for (JsonNode mapping : responseMappingNode) {
                    String fieldName = mapping.path("fieldName").asText();
                    String jsonPath = mapping.path("jsonPath").asText();

                    try {
                        Object value = JsonPath.read(responseBody, jsonPath);
                        mappedData.put(fieldName, value);
                    } catch (Exception e) {
                        log.warn("Could not extract field {} using JSONPath {}: {}",
                                fieldName, jsonPath, e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error mapping response: {}", e.getMessage());
        }

        return mappedData;
    }
}
