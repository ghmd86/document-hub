package com.documenthub.service;

import com.documenthub.model.DocumentListRequest;
import com.documenthub.model.extraction.DataExtractionConfig;
import com.documenthub.model.extraction.ExecutionStrategy;
import com.documenthub.service.extraction.ApiCallExecutor;
import com.documenthub.service.extraction.ExtractionPlan;
import com.documenthub.service.extraction.ExtractionPlanBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurableDataExtractionServiceTest {

    @Mock
    private ExtractionPlanBuilder planBuilder;

    @Mock
    private ApiCallExecutor apiCallExecutor;

    private ObjectMapper objectMapper;
    private ConfigurableDataExtractionService extractionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        extractionService = new ConfigurableDataExtractionService(
                objectMapper, planBuilder, apiCallExecutor);
    }

    @Nested
    @DisplayName("extractData Tests - Null Config")
    class NullConfigTests {

        @Test
        @DisplayName("Should return empty map when config is null")
        void shouldReturnEmptyMap_whenConfigNull() {
            // Given
            DocumentListRequest request = new DocumentListRequest();

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(null, request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(Map::isEmpty)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("extractData Tests - Empty Plan")
    class EmptyPlanTests {

        @Test
        @DisplayName("Should return initial context when plan is empty")
        void shouldReturnInitialContext_whenPlanEmpty() {
            // Given - dataSources is a Map, not an Array
            String configJson = "{\"fieldsToExtract\":[],\"dataSources\":{}}";
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Arrays.asList("acc-123"));
            request.setCustomerId(UUID.randomUUID());

            ExtractionPlan emptyPlan = mock(ExtractionPlan.class);
            when(emptyPlan.isEmpty()).thenReturn(true);
            when(planBuilder.buildPlan(any(), any())).thenReturn(emptyPlan);

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context -> {
                        assertTrue(context.containsKey("accountId"));
                        assertTrue(context.containsKey("customerId"));
                        assertTrue(context.containsKey("correlationId"));
                        return true;
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("extractData Tests - Initial Context")
    class InitialContextTests {

        @Test
        @DisplayName("Should populate accountId from request")
        void shouldPopulateAccountId() {
            // Given
            String configJson = "{\"fieldsToExtract\":[],\"dataSources\":{}}";
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Arrays.asList("acc-123", "acc-456"));

            ExtractionPlan emptyPlan = mock(ExtractionPlan.class);
            when(emptyPlan.isEmpty()).thenReturn(true);
            when(planBuilder.buildPlan(any(), any())).thenReturn(emptyPlan);

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context ->
                            "acc-123".equals(context.get("accountId")))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should populate customerId from request")
        void shouldPopulateCustomerId() {
            // Given
            String configJson = "{\"fieldsToExtract\":[],\"dataSources\":{}}";
            UUID customerId = UUID.randomUUID();
            DocumentListRequest request = new DocumentListRequest();
            request.setCustomerId(customerId);

            ExtractionPlan emptyPlan = mock(ExtractionPlan.class);
            when(emptyPlan.isEmpty()).thenReturn(true);
            when(planBuilder.buildPlan(any(), any())).thenReturn(emptyPlan);

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context ->
                            customerId.toString().equals(context.get("customerId")))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should populate referenceKey and referenceKeyType from request")
        void shouldPopulateReferenceKey() {
            // Given
            String configJson = "{\"fieldsToExtract\":[],\"dataSources\":{}}";
            DocumentListRequest request = new DocumentListRequest();
            request.setReferenceKey("REF-001");
            request.setReferenceKeyType("DISCLOSURE");

            ExtractionPlan emptyPlan = mock(ExtractionPlan.class);
            when(emptyPlan.isEmpty()).thenReturn(true);
            when(planBuilder.buildPlan(any(), any())).thenReturn(emptyPlan);

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context ->
                            "REF-001".equals(context.get("referenceKey")) &&
                                    "DISCLOSURE".equals(context.get("referenceKeyType")))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should add system variables")
        void shouldAddSystemVariables() {
            // Given
            String configJson = "{\"fieldsToExtract\":[],\"dataSources\":{}}";
            DocumentListRequest request = new DocumentListRequest();

            ExtractionPlan emptyPlan = mock(ExtractionPlan.class);
            when(emptyPlan.isEmpty()).thenReturn(true);
            when(planBuilder.buildPlan(any(), any())).thenReturn(emptyPlan);

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context ->
                            context.containsKey("correlationId") &&
                                    context.containsKey("auth.token"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle empty accountId list")
        void shouldHandleEmptyAccountIdList() {
            // Given
            String configJson = "{\"fieldsToExtract\":[],\"dataSources\":{}}";
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Collections.emptyList());

            ExtractionPlan emptyPlan = mock(ExtractionPlan.class);
            when(emptyPlan.isEmpty()).thenReturn(true);
            when(planBuilder.buildPlan(any(), any())).thenReturn(emptyPlan);

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context -> !context.containsKey("accountId"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("extractData Tests - Execution")
    class ExecutionTests {

        @Test
        @DisplayName("Should execute plan sequentially by default")
        void shouldExecuteSequentially_byDefault() {
            // Given - fieldsToExtract is List<String>, dataSources is Map
            String configJson = "{\"fieldsToExtract\":[\"creditScore\"],\"dataSources\":{}}";
            DocumentListRequest request = new DocumentListRequest();

            ExtractionPlan plan = mock(ExtractionPlan.class);
            when(plan.isEmpty()).thenReturn(false);
            when(plan.size()).thenReturn(1);
            when(planBuilder.buildPlan(any(), any())).thenReturn(plan);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 750);
            when(apiCallExecutor.executeSequential(any(), any()))
                    .thenReturn(Mono.just(extractedData));

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context -> context.containsKey("creditScore"))
                    .verifyComplete();

            verify(apiCallExecutor).executeSequential(any(), any());
            verify(apiCallExecutor, never()).executeParallel(any(), any());
        }

        @Test
        @DisplayName("Should execute plan in parallel when configured")
        void shouldExecuteInParallel_whenConfigured() {
            // Given
            String configJson = "{\"fieldsToExtract\":[\"creditScore\"]," +
                    "\"dataSources\":{},\"executionStrategy\":{\"mode\":\"parallel\"}}";
            DocumentListRequest request = new DocumentListRequest();

            ExtractionPlan plan = mock(ExtractionPlan.class);
            when(plan.isEmpty()).thenReturn(false);
            when(plan.size()).thenReturn(1);
            when(planBuilder.buildPlan(any(), any())).thenReturn(plan);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 750);
            when(apiCallExecutor.executeParallel(any(), any()))
                    .thenReturn(Mono.just(extractedData));

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context -> context.containsKey("creditScore"))
                    .verifyComplete();

            verify(apiCallExecutor).executeParallel(any(), any());
            verify(apiCallExecutor, never()).executeSequential(any(), any());
        }

        @Test
        @DisplayName("Should execute sequentially when mode is sequential")
        void shouldExecuteSequentially_whenModeIsSequential() {
            // Given
            String configJson = "{\"fieldsToExtract\":[\"creditScore\"]," +
                    "\"dataSources\":{},\"executionStrategy\":{\"mode\":\"sequential\"}}";
            DocumentListRequest request = new DocumentListRequest();

            ExtractionPlan plan = mock(ExtractionPlan.class);
            when(plan.isEmpty()).thenReturn(false);
            when(plan.size()).thenReturn(1);
            when(planBuilder.buildPlan(any(), any())).thenReturn(plan);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 750);
            when(apiCallExecutor.executeSequential(any(), any()))
                    .thenReturn(Mono.just(extractedData));

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(configJson), request);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(context -> context.containsKey("creditScore"))
                    .verifyComplete();

            verify(apiCallExecutor).executeSequential(any(), any());
        }
    }

    @Nested
    @DisplayName("extractData Tests - Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error when config parsing fails")
        void shouldReturnError_whenConfigParsingFails() {
            // Given
            String invalidJson = "not valid json";
            DocumentListRequest request = new DocumentListRequest();

            // When
            Mono<Map<String, Object>> result = extractionService.extractData(
                    Json.of(invalidJson), request);

            // Then
            StepVerifier.create(result)
                    .expectError()
                    .verify();
        }
    }
}
