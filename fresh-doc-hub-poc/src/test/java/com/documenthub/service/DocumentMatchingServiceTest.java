package com.documenthub.service;

import com.documenthub.config.ReferenceKeyConfig;
import com.documenthub.dao.StorageIndexDao;
import com.documenthub.dto.DocumentQueryParamsDto;
import com.documenthub.dto.MasterTemplateDto;
import com.documenthub.dto.StorageIndexDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class DocumentMatchingServiceTest {

    @Mock
    private StorageIndexDao storageIndexDao;

    @Mock
    private DocumentValidityService validityService;

    @Mock
    private ReferenceKeyConfig referenceKeyConfig;

    private ObjectMapper objectMapper;
    private DocumentMatchingService documentMatchingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        documentMatchingService = new DocumentMatchingService(
                storageIndexDao, validityService, referenceKeyConfig, objectMapper);
        // Default: allow all reference key types in tests
        when(referenceKeyConfig.isValid(anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("queryDocuments Tests - Standard Query")
    class StandardQueryTests {

        @Test
        @DisplayName("Should query account documents when no document matching config")
        void shouldQueryAccountDocuments_whenNoDocumentMatchingConfig() {
            // Given
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(null);
            template.setSharedDocumentFlag(false);

            UUID accountId = UUID.randomUUID();
            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findAccountSpecificDocumentsWithDateRange(
                    any(), anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, accountId, null));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();

            verify(storageIndexDao).findAccountSpecificDocumentsWithDateRange(
                    eq(accountId), eq("TestTemplate"), any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("Should query shared documents when shared flag is true")
        void shouldQuerySharedDocuments_whenSharedFlagTrue() {
            // Given
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(null);
            template.setSharedDocumentFlag(true);

            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findSharedDocumentsWithDateRange(
                    anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), null));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();

            verify(storageIndexDao).findSharedDocumentsWithDateRange(
                    eq("TestTemplate"), any(), any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("queryDocuments Tests - Reference Key Matching")
    class ReferenceKeyMatchingTests {

        @Test
        @DisplayName("Should query by reference key when config specifies reference_key match")
        void shouldQueryByReferenceKey_whenConfigured() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"reference_key\"," +
                    "\"referenceKeyField\":\"disclosureCode\",\"referenceKeyType\":\"DISCLOSURE\"}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("disclosureCode", "DISC-001");

            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                    anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();

            verify(storageIndexDao).findByReferenceKeyAndTemplateWithDateRange(
                    eq("DISC-001"), eq("DISCLOSURE"), eq("TestTemplate"),
                    any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("Should return empty when reference key not found in extracted data")
        void shouldReturnEmpty_whenReferenceKeyNotFound() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"reference_key\"," +
                    "\"referenceKeyField\":\"disclosureCode\",\"referenceKeyType\":\"DISCLOSURE\"}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            // Note: disclosureCode is NOT in extractedData

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("queryDocuments Tests - Conditional Matching")
    class ConditionalMatchingTests {

        @Test
        @DisplayName("Should match condition with numeric >= operator")
        void shouldMatchCondition_numericGreaterThanOrEqual() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\"," +
                    "\"referenceKeyType\":\"TIER\",\"conditions\":[" +
                    "{\"field\":\"creditScore\",\"operator\":\">=\",\"value\":750,\"referenceKey\":\"PREMIUM\"}," +
                    "{\"field\":\"creditScore\",\"operator\":\">=\",\"value\":650,\"referenceKey\":\"STANDARD\"}" +
                    "]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 800);

            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                    anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();

            verify(storageIndexDao).findByReferenceKeyAndTemplateWithDateRange(
                    eq("PREMIUM"), eq("TIER"), anyString(), any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("Should match second condition when first fails")
        void shouldMatchSecondCondition_whenFirstFails() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\"," +
                    "\"referenceKeyType\":\"TIER\",\"conditions\":[" +
                    "{\"field\":\"creditScore\",\"operator\":\">=\",\"value\":750,\"referenceKey\":\"PREMIUM\"}," +
                    "{\"field\":\"creditScore\",\"operator\":\">=\",\"value\":650,\"referenceKey\":\"STANDARD\"}" +
                    "]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 700); // Between 650 and 750

            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                    anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();

            verify(storageIndexDao).findByReferenceKeyAndTemplateWithDateRange(
                    eq("STANDARD"), eq("TIER"), anyString(), any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("Should return empty when no condition matches")
        void shouldReturnEmpty_whenNoConditionMatches() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\"," +
                    "\"referenceKeyType\":\"TIER\"," +
                    "\"conditions\":[" +
                    "{\"field\":\"creditScore\",\"operator\":\">=\",\"value\":750,\"referenceKey\":\"PREMIUM\"}" +
                    "]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 600); // Below 750

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle string equality condition")
        void shouldHandleStringEqualityCondition() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\"," +
                    "\"referenceKeyType\":\"STATE_DOC\",\"conditions\":[" +
                    "{\"field\":\"state\",\"operator\":\"==\",\"value\":\"CA\",\"referenceKey\":\"CA-DOC\"}" +
                    "]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("state", "CA");

            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                    anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();

            verify(storageIndexDao).findByReferenceKeyAndTemplateWithDateRange(
                    eq("CA-DOC"), eq("STATE_DOC"), anyString(), any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("Should handle boolean condition")
        void shouldHandleBooleanCondition() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\"," +
                    "\"referenceKeyType\":\"VIP\",\"conditions\":[" +
                    "{\"field\":\"isVip\",\"operator\":\"==\",\"value\":true,\"referenceKey\":\"VIP-DOC\"}" +
                    "]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("isVip", true);

            List<StorageIndexDto> documents = Arrays.asList(createStorageEntity());

            when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                    anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                    .thenReturn(Flux.fromIterable(documents));
            when(validityService.filterByValidity(any()))
                    .thenReturn(documents);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle all numeric operators")
        void shouldHandleAllNumericOperators() {
            // Test each operator
            testNumericOperator(">", 10, 5, true);
            testNumericOperator(">", 5, 10, false);
            testNumericOperator("<", 5, 10, true);
            testNumericOperator("<", 10, 5, false);
            testNumericOperator("<=", 10, 10, true);
            testNumericOperator("<=", 5, 10, true);
            testNumericOperator("==", 10, 10, true);
            testNumericOperator("!=", 10, 5, true);
        }

        private void testNumericOperator(String operator, int value, int threshold, boolean shouldMatch) {
            String documentMatchingConfig = String.format(
                    "{\"matchBy\":\"conditional\"," +
                            "\"referenceKeyType\":\"TEST_TYPE\"," +
                            "\"conditions\":[{\"field\":\"score\",\"operator\":\"%s\",\"value\":%d,\"referenceKey\":\"REF\"}]}",
                    operator, threshold);
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("score", value);

            if (shouldMatch) {
                when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                        anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                        .thenReturn(Flux.just(createStorageEntity()));
                when(validityService.filterByValidity(any()))
                        .thenReturn(Arrays.asList(createStorageEntity()));
            }

            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            StepVerifier.create(result)
                    .expectNextMatches(list -> shouldMatch ? !list.isEmpty() : list.isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle string operators - contains, startsWith, endsWith")
        void shouldHandleStringOperators() {
            testStringCondition("contains", "hello world", "world", true);
            testStringCondition("startsWith", "hello world", "hello", true);
            testStringCondition("endsWith", "hello world", "world", true);
            testStringCondition("!=", "hello", "world", true);
        }

        private void testStringCondition(String operator, String value, String threshold, boolean shouldMatch) {
            String documentMatchingConfig = String.format(
                    "{\"matchBy\":\"conditional\"," +
                            "\"referenceKeyType\":\"TEST_TYPE\"," +
                            "\"conditions\":[{\"field\":\"text\",\"operator\":\"%s\",\"value\":\"%s\",\"referenceKey\":\"REF\"}]}",
                    operator, threshold);
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("text", value);

            if (shouldMatch) {
                when(storageIndexDao.findByReferenceKeyAndTemplateWithDateRange(
                        anyString(), anyString(), anyString(), any(), any(), any(), anyLong()))
                        .thenReturn(Flux.just(createStorageEntity()));
                when(validityService.filterByValidity(any()))
                        .thenReturn(Arrays.asList(createStorageEntity()));
            }

            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            StepVerifier.create(result)
                    .expectNextMatches(list -> shouldMatch ? !list.isEmpty() : list.isEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("queryDocuments Tests - Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return empty list when unknown matchBy type")
        void shouldReturnEmpty_whenUnknownMatchByType() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"unknown_type\"}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty list when invalid JSON config")
        void shouldReturnEmpty_whenInvalidJsonConfig() {
            // Given
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig("invalid json");

            Map<String, Object> extractedData = new HashMap<>();

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return error when referenceKeyType is missing for conditional matching")
        void shouldReturnError_whenReferenceKeyTypeMissing() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\",\"conditions\":[]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 800);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                            e.getMessage().contains("referenceKeyType is required"))
                    .verify();
        }

        @Test
        @DisplayName("Should return empty when conditions array is empty")
        void shouldReturnEmpty_whenConditionsArrayEmpty() {
            // Given
            String documentMatchingConfig = "{\"matchBy\":\"conditional\",\"referenceKeyType\":\"TIER\",\"conditions\":[]}";
            MasterTemplateDto template = createTemplate();
            template.setDocumentMatchingConfig(documentMatchingConfig);

            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("creditScore", 800);

            // When
            Mono<List<StorageIndexDto>> result = documentMatchingService.queryDocuments(
                    buildParams(template, UUID.randomUUID(), extractedData));

            // Then - No condition matches, should return empty
            StepVerifier.create(result)
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }
    }

    // Helper methods
    private MasterTemplateDto createTemplate() {
        MasterTemplateDto template = new MasterTemplateDto();
        template.setTemplateType("TestTemplate");
        template.setTemplateVersion(1);
        template.setSharedDocumentFlag(false);
        return template;
    }

    private StorageIndexDto createStorageEntity() {
        StorageIndexDto entity = new StorageIndexDto();
        entity.setStorageIndexId(UUID.randomUUID());
        entity.setStorageDocumentKey(UUID.randomUUID());
        entity.setFileName("test-file.pdf");
        return entity;
    }

    private DocumentQueryParamsDto buildParams(
            MasterTemplateDto template,
            UUID accountId,
            Map<String, Object> extractedData) {
        return DocumentQueryParamsDto.builder()
                .template(template)
                .accountId(accountId)
                .extractedData(extractedData)
                .postedFromDate(null)
                .postedToDate(null)
                .build();
    }
}
