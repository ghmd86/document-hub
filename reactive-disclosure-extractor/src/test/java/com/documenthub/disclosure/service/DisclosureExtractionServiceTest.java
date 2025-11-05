package com.documenthub.disclosure.service;

import com.documenthub.disclosure.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DisclosureExtractionService
 */
class DisclosureExtractionServiceTest {

    private MockWebServer mockWebServer;
    private DisclosureExtractionService service;
    private ReactiveRedisTemplate<String, Object> mockRedisTemplate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();

        // Mock Redis template
        mockRedisTemplate = mock(ReactiveRedisTemplate.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(mock());

        // Create service with mocked dependencies
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        service = new DisclosureExtractionService(
                () -> webClient,
                mockRedisTemplate,
                objectMapper,
                new SimpleMeterRegistry()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testSuccessfulDisclosureExtraction() {
        // Arrange
        String arrangementsResponse = """
            {
              "content": [
                {
                  "domain": "PRICING",
                  "domainId": "PRICING_123",
                  "status": "ACTIVE"
                }
              ]
            }
            """;

        String pricingResponse = """
            {
              "cardholderAgreementsTncCode": "DISC_CC_STD_001",
              "version": "2.0",
              "effectiveDate": "2024-01-01T00:00:00Z",
              "expirationDate": "2025-12-31T23:59:59Z"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(arrangementsResponse)
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setBody(pricingResponse)
                .addHeader("Content-Type", "application/json"));

        ExtractionConfig config = createTestConfig();
        Map<String, Object> input = Map.of("accountId", "ACC-123");

        // Act & Assert
        StepVerifier.create(service.extract(config, input, "test-correlation-id"))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertNotNull(result.getData());
                    assertNotNull(result.getData().get("disclosureData"));

                    Map<String, Object> disclosureData =
                            (Map<String, Object>) result.getData().get("disclosureData");
                    assertEquals("DISC_CC_STD_001", disclosureData.get("disclosureCode"));

                    assertEquals(2, result.getMetadata().getApiCalls());
                })
                .verifyComplete();
    }

    @Test
    void testExtractionWithMissingPricingId() {
        // Arrange
        String arrangementsResponse = """
            {
              "content": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(arrangementsResponse)
                .addHeader("Content-Type", "application/json"));

        ExtractionConfig config = createTestConfig();
        Map<String, Object> input = Map.of("accountId", "ACC-123");

        // Act & Assert
        StepVerifier.create(service.extract(config, input, "test-correlation-id"))
                .assertNext(result -> {
                    // Should fail validation or return default
                    assertFalse(result.isSuccess());
                })
                .verifyComplete();
    }

    @Test
    void testAPIRetryOn5xxError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"content\": []}")
                .addHeader("Content-Type", "application/json"));

        ExtractionConfig config = createTestConfig();
        Map<String, Object> input = Map.of("accountId", "ACC-123");

        // Act & Assert
        StepVerifier.create(service.extract(config, input, "test-correlation-id"))
                .assertNext(result -> {
                    // Should eventually succeed after retries
                    assertNotNull(result);
                })
                .verifyComplete();
    }

    private ExtractionConfig createTestConfig() {
        ExtractionConfig config = new ExtractionConfig();

        // Data source 1: Get arrangements
        DataSource ds1 = new DataSource();
        ds1.setId("getAccountArrangements");
        ds1.setDescription("Get account arrangements");

        EndpointConfig endpoint1 = new EndpointConfig();
        endpoint1.setUrl(mockWebServer.url("/arrangements").toString());
        endpoint1.setMethod("GET");
        endpoint1.setTimeout(5000);

        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryPolicy.setBackoffStrategy("exponential");
        retryPolicy.setRetryOn(List.of(500, 502, 503, 504));
        endpoint1.setRetryPolicy(retryPolicy);

        ds1.setEndpoint(endpoint1);

        CacheConfig cache1 = new CacheConfig();
        cache1.setEnabled(false); // Disable for testing
        ds1.setCache(cache1);

        ResponseMapping mapping1 = new ResponseMapping();
        mapping1.setExtract(Map.of("pricingId", "$.content[0].domainId"));

        Validation validation1 = new Validation();
        validation1.setType("string");
        validation1.setRequired(true);
        mapping1.setValidate(Map.of("pricingId", validation1));

        ds1.setResponseMapping(mapping1);

        NextCall nextCall = new NextCall();
        nextCall.setTargetDataSource("getPricingData");
        Condition condition = new Condition();
        condition.setField("pricingId");
        condition.setOperator("notNull");
        nextCall.setCondition(condition);
        ds1.setNextCalls(List.of(nextCall));

        // Data source 2: Get pricing data
        DataSource ds2 = new DataSource();
        ds2.setId("getPricingData");
        ds2.setDescription("Get pricing data");

        EndpointConfig endpoint2 = new EndpointConfig();
        endpoint2.setUrl(mockWebServer.url("/pricing").toString());
        endpoint2.setMethod("GET");
        endpoint2.setTimeout(5000);
        ds2.setEndpoint(endpoint2);

        CacheConfig cache2 = new CacheConfig();
        cache2.setEnabled(false); // Disable for testing
        ds2.setCache(cache2);

        ResponseMapping mapping2 = new ResponseMapping();
        mapping2.setExtract(Map.of(
                "disclosureCode", "$.cardholderAgreementsTncCode",
                "pricingVersion", "$.version"
        ));
        ds2.setResponseMapping(mapping2);
        ds2.setStoreAs("disclosureData");

        config.setExtractionStrategy(List.of(ds1, ds2));

        ExecutionRules executionRules = new ExecutionRules();
        executionRules.setStartFrom("getAccountArrangements");
        executionRules.setStopOnError(true);

        ExecutionErrorHandling errorHandling = new ExecutionErrorHandling();
        errorHandling.setStrategy("fail-fast");
        executionRules.setErrorHandling(errorHandling);

        config.setExecutionRules(executionRules);

        return config;
    }
}
