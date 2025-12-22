package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.*;
import com.documenthub.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentEnquiryProcessor.
 * Tests the optional accountId functionality and document retrieval scenarios.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentEnquiryProcessorTest {

    @Mock
    private MasterTemplateDao masterTemplateDao;

    @Mock
    private AccountMetadataService accountMetadataService;

    @Mock
    private RuleEvaluationService ruleEvaluationService;

    @Mock
    private ConfigurableDataExtractionService dataExtractionService;

    @Mock
    private DocumentMatchingService documentMatchingService;

    @Mock
    private DocumentResponseBuilder responseBuilder;

    private DocumentEnquiryProcessor documentEnquiryProcessor;

    // Test data
    private static final UUID CUSTOMER_ID = UUID.fromString("cccc0000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_1 = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_2 = UUID.fromString("aaaa0000-0000-0000-0000-000000000002");
    private static final String REQUESTOR_TYPE = "CUSTOMER";

    @BeforeEach
    void setUp() {
        documentEnquiryProcessor = new DocumentEnquiryProcessor(
                masterTemplateDao,
                accountMetadataService,
                ruleEvaluationService,
                dataExtractionService,
                documentMatchingService,
                responseBuilder
        );
    }

    // ========================================================================
    // Scenario 1: accountId is provided (existing behavior)
    // ========================================================================
    @Nested
    @DisplayName("Scenario 1: accountId provided")
    class AccountIdProvidedTests {

        @Test
        @DisplayName("Should use provided accountId when present")
        void shouldUseProvidedAccountId_whenPresent() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(List.of(ACCOUNT_1.toString()));
            request.setCustomerId(CUSTOMER_ID);

            setupMocksForSuccessfulQuery();

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> response != null)
                    .verifyComplete();

            // Verify accountMetadataService.getAccountsByCustomerId was NOT called
            verify(accountMetadataService, never()).getAccountsByCustomerId(any());
            // Verify getAccountMetadata was called for the provided accountId (at least once, may be called multiple times in flow)
            verify(accountMetadataService, atLeast(1)).getAccountMetadata(ACCOUNT_1);
        }

        @Test
        @DisplayName("Should process multiple provided accountIds")
        void shouldProcessMultipleAccountIds_whenProvided() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(List.of(ACCOUNT_1.toString(), ACCOUNT_2.toString()));
            request.setCustomerId(CUSTOMER_ID);

            setupMocksForSuccessfulQuery();

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> response != null)
                    .verifyComplete();

            // Verify both accounts were processed (at least once each)
            verify(accountMetadataService, atLeast(1)).getAccountMetadata(ACCOUNT_1);
            verify(accountMetadataService, atLeast(1)).getAccountMetadata(ACCOUNT_2);
        }
    }

    // ========================================================================
    // Scenario 2: accountId is empty but customerId is provided
    // ========================================================================
    @Nested
    @DisplayName("Scenario 2: accountId empty, customerId provided")
    class AccountIdEmptyCustomerIdProvidedTests {

        @Test
        @DisplayName("Should fetch accounts by customerId when accountId is empty")
        void shouldFetchAccountsByCustomerId_whenAccountIdEmpty() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Collections.emptyList()); // Empty list
            request.setCustomerId(CUSTOMER_ID);

            AccountMetadata account1Meta = createAccountMetadata(ACCOUNT_1, CUSTOMER_ID);
            AccountMetadata account2Meta = createAccountMetadata(ACCOUNT_2, CUSTOMER_ID);

            when(accountMetadataService.getAccountsByCustomerId(CUSTOMER_ID))
                    .thenReturn(Flux.just(account1Meta, account2Meta));

            setupMocksForSuccessfulQuery();

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> response != null)
                    .verifyComplete();

            // Verify getAccountsByCustomerId was called
            verify(accountMetadataService).getAccountsByCustomerId(CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should fetch accounts by customerId when accountId is null")
        void shouldFetchAccountsByCustomerId_whenAccountIdNull() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(null); // Null
            request.setCustomerId(CUSTOMER_ID);

            AccountMetadata account1Meta = createAccountMetadata(ACCOUNT_1, CUSTOMER_ID);

            when(accountMetadataService.getAccountsByCustomerId(CUSTOMER_ID))
                    .thenReturn(Flux.just(account1Meta));

            setupMocksForSuccessfulQuery();

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> response != null)
                    .verifyComplete();

            verify(accountMetadataService).getAccountsByCustomerId(CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should process all fetched accounts for the customer")
        void shouldProcessAllFetchedAccounts() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Collections.emptyList());
            request.setCustomerId(CUSTOMER_ID);

            AccountMetadata account1Meta = createAccountMetadata(ACCOUNT_1, CUSTOMER_ID);
            AccountMetadata account2Meta = createAccountMetadata(ACCOUNT_2, CUSTOMER_ID);

            when(accountMetadataService.getAccountsByCustomerId(CUSTOMER_ID))
                    .thenReturn(Flux.just(account1Meta, account2Meta));

            setupMocksForSuccessfulQuery();

            // When
            documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE).block();

            // Then - verify both fetched accounts were processed (at least once each)
            verify(accountMetadataService, atLeast(1)).getAccountMetadata(ACCOUNT_1);
            verify(accountMetadataService, atLeast(1)).getAccountMetadata(ACCOUNT_2);
        }

        @Test
        @DisplayName("Should return empty response when no accounts found for customer")
        void shouldReturnEmptyResponse_whenNoAccountsFoundForCustomer() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Collections.emptyList());
            request.setCustomerId(CUSTOMER_ID);

            DocumentRetrievalResponse emptyResponse = createEmptyResponse();

            when(accountMetadataService.getAccountsByCustomerId(CUSTOMER_ID))
                    .thenReturn(Flux.empty()); // No accounts
            when(responseBuilder.buildEmptyResponse()).thenReturn(emptyResponse);

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals(0, response.getDocumentList().size());
                        return true;
                    })
                    .verifyComplete();

            verify(responseBuilder).buildEmptyResponse();
        }
    }

    // ========================================================================
    // Scenario 3: Neither accountId nor customerId provided
    // ========================================================================
    @Nested
    @DisplayName("Scenario 3: Neither accountId nor customerId provided")
    class NeitherProvidedTests {

        @Test
        @DisplayName("Should return empty response when both accountId and customerId are null")
        void shouldReturnEmptyResponse_whenBothNull() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(null);
            request.setCustomerId(null);

            DocumentRetrievalResponse emptyResponse = createEmptyResponse();
            when(responseBuilder.buildEmptyResponse()).thenReturn(emptyResponse);

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals(0, response.getDocumentList().size());
                        return true;
                    })
                    .verifyComplete();

            verify(responseBuilder).buildEmptyResponse();
            verify(accountMetadataService, never()).getAccountsByCustomerId(any());
            verify(masterTemplateDao, never()).findActiveTemplatesWithFilters(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return empty response when accountId is empty and customerId is null")
        void shouldReturnEmptyResponse_whenAccountIdEmptyAndCustomerIdNull() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(Collections.emptyList());
            request.setCustomerId(null);

            DocumentRetrievalResponse emptyResponse = createEmptyResponse();
            when(responseBuilder.buildEmptyResponse()).thenReturn(emptyResponse);

            // When
            Mono<DocumentRetrievalResponse> result = documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> response.getDocumentList().isEmpty())
                    .verifyComplete();

            verify(responseBuilder).buildEmptyResponse();
        }
    }

    // ========================================================================
    // Scenario 4: accountId provided takes precedence over customerId lookup
    // ========================================================================
    @Nested
    @DisplayName("Scenario 4: accountId takes precedence")
    class AccountIdPrecedenceTests {

        @Test
        @DisplayName("Should use accountId and NOT fetch by customerId when both provided")
        void shouldUseAccountId_notFetchByCustomerId_whenBothProvided() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(List.of(ACCOUNT_1.toString())); // Specific account
            request.setCustomerId(CUSTOMER_ID); // Also has customerId

            setupMocksForSuccessfulQuery();

            // When
            documentEnquiryProcessor.processEnquiry(request, REQUESTOR_TYPE).block();

            // Then - should NOT call getAccountsByCustomerId
            verify(accountMetadataService, never()).getAccountsByCustomerId(any());
            // Should use the provided accountId (at least once)
            verify(accountMetadataService, atLeast(1)).getAccountMetadata(ACCOUNT_1);
        }
    }

    // ========================================================================
    // Scenario 5: Default requestor type
    // ========================================================================
    @Nested
    @DisplayName("Scenario 5: Default requestor type")
    class DefaultRequestorTypeTests {

        @Test
        @DisplayName("Should use CUSTOMER as default requestor type")
        void shouldUseCustomerAsDefaultRequestorType() {
            // Given
            DocumentListRequest request = new DocumentListRequest();
            request.setAccountId(List.of(ACCOUNT_1.toString()));

            setupMocksForSuccessfulQuery();

            // When - call without requestorType
            documentEnquiryProcessor.processEnquiry(request).block();

            // Then - should still work with default CUSTOMER requestor type
            verify(masterTemplateDao).findActiveTemplatesWithFilters(
                    anyString(), anyBoolean(), any(), anyLong()
            );
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void setupMocksForSuccessfulQuery() {
        // Account metadata
        AccountMetadata accountMetadata = createAccountMetadata(ACCOUNT_1, CUSTOMER_ID);
        when(accountMetadataService.getAccountMetadata(any(UUID.class)))
                .thenReturn(Mono.just(accountMetadata));

        // Template DAO
        MasterTemplateDefinitionEntity template = createTemplate();
        when(masterTemplateDao.findActiveTemplatesWithFilters(anyString(), anyBoolean(), any(), anyLong()))
                .thenReturn(Flux.just(template));

        // Data extraction
        when(dataExtractionService.extractData(any(), any()))
                .thenReturn(Mono.just(Collections.emptyMap()));

        // Document matching
        when(documentMatchingService.queryDocuments(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(Collections.emptyList()));

        // Response builder
        DocumentRetrievalResponse response = createSuccessResponse();
        when(responseBuilder.convertToNodes(any(), any(), anyString()))
                .thenReturn(Collections.emptyList());
        when(responseBuilder.determinePageSize(any())).thenReturn(20);
        when(responseBuilder.determinePageNumber(any())).thenReturn(0);
        when(responseBuilder.paginate(any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(responseBuilder.buildResponse(any(), anyInt(), anyInt(), anyInt(), anyLong()))
                .thenReturn(response);
    }

    private AccountMetadata createAccountMetadata(UUID accountId, UUID customerId) {
        return AccountMetadata.builder()
                .accountId(accountId)
                .customerId(customerId)
                .accountType("credit_card")
                .region("US_WEST")
                .state("CA")
                .customerSegment("STANDARD")
                .lineOfBusiness("CREDIT_CARD")
                .isActive(true)
                .build();
    }

    private MasterTemplateDefinitionEntity createTemplate() {
        MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
        template.setTemplateType("TEST_TEMPLATE");
        template.setLineOfBusiness("CREDIT_CARD");
        template.setTemplateCategory("TEST");
        template.setActiveFlag(true);
        template.setMessageCenterDocFlag(true);
        return template;
    }

    private DocumentRetrievalResponse createEmptyResponse() {
        DocumentRetrievalResponse response = new DocumentRetrievalResponse();
        response.setDocumentList(Collections.emptyList());
        response.setPagination(new PaginationResponse());
        response.setLinks(new DocumentRetrievalResponseLinks());
        return response;
    }

    private DocumentRetrievalResponse createSuccessResponse() {
        DocumentRetrievalResponse response = new DocumentRetrievalResponse();
        response.setDocumentList(Collections.emptyList());
        PaginationResponse pagination = new PaginationResponse();
        pagination.setTotalItems(0);
        pagination.setPageNumber(0);
        pagination.setPageSize(20);
        pagination.setTotalPages(0);
        response.setPagination(pagination);
        response.setLinks(new DocumentRetrievalResponseLinks());
        return response;
    }
}
