package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.dao.StorageIndexDao;
import com.documenthub.dto.DocumentUploadRequest;
import com.documenthub.dto.MasterTemplateDto;
import com.documenthub.dto.StorageIndexDto;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
import com.documenthub.model.InlineResponse200;
import com.documenthub.service.DocumentAccessControlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentManagementProcessor.
 * Tests the single_document_flag upload behavior.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentManagementProcessorTest {

    @Mock
    private MasterTemplateDao masterTemplateDao;

    @Mock
    private StorageIndexDao storageIndexDao;

    @Mock
    private EcmsClient ecmsClient;

    @Mock
    private DocumentAccessControlService accessControlService;

    private ObjectMapper objectMapper;
    private DocumentManagementProcessor processor;

    private static final String DOC_TYPE = "INVOICE";
    private static final String REF_KEY = "REF-123";
    private static final String REF_KEY_TYPE = "ORDER";
    private static final String REQUESTOR_TYPE = "CUSTOMER";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new DocumentManagementProcessor(
            masterTemplateDao, storageIndexDao, ecmsClient,
            accessControlService, objectMapper);
    }

    @Nested
    @DisplayName("Single Document Flag - Upload Tests")
    class SingleDocumentFlagUploadTests {

        @Test
        @DisplayName("Should close existing docs when single_document_flag is true")
        void shouldCloseExistingDocsWhenSingleDocFlagTrue() {
            // Given
            MasterTemplateDto template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest();

            setupMocksForSuccessfulUpload(template);
            when(storageIndexDao.updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE), anyLong()))
                .thenReturn(Mono.just(2L));

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(storageIndexDao).updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE), anyLong());
        }

        @Test
        @DisplayName("Should NOT close existing docs when single_document_flag is false")
        void shouldNotCloseExistingDocsWhenSingleDocFlagFalse() {
            // Given
            MasterTemplateDto template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest();

            setupMocksForSuccessfulUpload(template);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(storageIndexDao, never()).updateEndDateByReferenceKey(
                anyString(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Should NOT close existing docs when single_document_flag is null")
        void shouldNotCloseExistingDocsWhenSingleDocFlagNull() {
            // Given
            MasterTemplateDto template = createTemplate(null);
            DocumentUploadRequest request = createUploadRequest();

            setupMocksForSuccessfulUpload(template);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(storageIndexDao, never()).updateEndDateByReferenceKey(
                anyString(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Should NOT close when reference_key is null")
        void shouldNotCloseWhenReferenceKeyIsNull() {
            // Given
            MasterTemplateDto template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest();
            request.setReferenceKey(null);

            setupMocksForSuccessfulUpload(template);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(storageIndexDao, never()).updateEndDateByReferenceKey(
                anyString(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Should NOT close when reference_key_type is null")
        void shouldNotCloseWhenReferenceKeyTypeIsNull() {
            // Given
            MasterTemplateDto template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest();
            request.setReferenceKeyType(null);

            setupMocksForSuccessfulUpload(template);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(storageIndexDao, never()).updateEndDateByReferenceKey(
                anyString(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Should use activeStartDate as new end_date for existing docs")
        void shouldUseActiveStartDateAsNewEndDate() {
            // Given
            MasterTemplateDto template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest();
            Long startDate = 1234567890L;
            request.setActiveStartDate(startDate);

            setupMocksForSuccessfulUpload(template);
            when(storageIndexDao.updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE), eq(startDate)))
                .thenReturn(Mono.just(1L));

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(storageIndexDao).updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE), eq(startDate));
        }

        @Test
        @DisplayName("Should use current time when activeStartDate is null")
        void shouldUseCurrentTimeWhenActiveStartDateIsNull() {
            // Given
            MasterTemplateDto template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest();
            request.setActiveStartDate(null);

            setupMocksForSuccessfulUpload(template);
            when(storageIndexDao.updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE), anyLong()))
                .thenReturn(Mono.just(1L));

            long beforeCall = System.currentTimeMillis();

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            long afterCall = System.currentTimeMillis();

            verify(storageIndexDao).updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE),
                longThat(time -> time >= beforeCall && time <= afterCall));
        }

        @Test
        @DisplayName("Should proceed with upload even when no docs to close")
        void shouldProceedWithUploadWhenNoDocsToClose() {
            // Given
            MasterTemplateDto template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest();

            setupMocksForSuccessfulUpload(template);
            when(storageIndexDao.updateEndDateByReferenceKey(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(DOC_TYPE), anyLong()))
                .thenReturn(Mono.just(0L)); // No docs closed

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();

            verify(ecmsClient).uploadDocument(any(byte[].class), any());
            verify(storageIndexDao).save(any(StorageIndexDto.class));
        }
    }

    private void setupMocksForSuccessfulUpload(MasterTemplateDto template) {
        when(masterTemplateDao.findLatestActiveTemplateByType(eq(DOC_TYPE), anyLong()))
            .thenReturn(Mono.just(template));
        when(accessControlService.canUpload(any(), eq(REQUESTOR_TYPE)))
            .thenReturn(true);

        EcmsDocumentResponse ecmsResponse = new EcmsDocumentResponse();
        ecmsResponse.setId(UUID.randomUUID());
        when(ecmsClient.uploadDocument(any(byte[].class), any()))
            .thenReturn(Mono.just(ecmsResponse));

        when(storageIndexDao.save(any(StorageIndexDto.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    private MasterTemplateDto createTemplate(Boolean singleDocFlag) {
        MasterTemplateDto template = new MasterTemplateDto();
        template.setMasterTemplateId(UUID.randomUUID());
        template.setTemplateType(DOC_TYPE);
        template.setTemplateVersion(1);
        template.setSingleDocumentFlag(singleDocFlag);
        template.setActiveFlag(true);
        return template;
    }

    private DocumentUploadRequest createUploadRequest() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "test content".getBytes());

        return DocumentUploadRequest.builder()
            .content(file)
            .documentType(DOC_TYPE)
            .fileName("test.pdf")
            .referenceKey(REF_KEY)
            .referenceKeyType(REF_KEY_TYPE)
            .createdBy("testUser")
            .build();
    }

    @Nested
    @DisplayName("ReferenceKeyType Validation Tests")
    class ReferenceKeyTypeValidationTests {

        @Test
        @DisplayName("Should succeed when referenceKeyType matches template config")
        void shouldSucceedWhenReferenceKeyTypeMatches() {
            // Given
            MasterTemplateDto template = createTemplateWithMatchingConfig("ORDER");
            DocumentUploadRequest request = createUploadRequest();

            setupMocksForSuccessfulUpload(template);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail when referenceKeyType does not match template config")
        void shouldFailWhenReferenceKeyTypeMismatch() {
            // Given
            MasterTemplateDto template = createTemplateWithMatchingConfig("PROMO_CODE");
            DocumentUploadRequest request = createUploadRequest(); // Has REF_KEY_TYPE = "ORDER"

            when(masterTemplateDao.findLatestActiveTemplateByType(eq(DOC_TYPE), anyLong()))
                .thenReturn(Mono.just(template));
            when(accessControlService.canUpload(any(), eq(REQUESTOR_TYPE)))
                .thenReturn(true);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                    && e.getMessage().contains("referenceKeyType mismatch"))
                .verify();
        }

        @Test
        @DisplayName("Should fail when template requires referenceKeyType but request has none")
        void shouldFailWhenReferenceKeyTypeRequiredButMissing() {
            // Given
            MasterTemplateDto template = createTemplateWithMatchingConfig("ORDER");
            DocumentUploadRequest request = createUploadRequest();
            request.setReferenceKeyType(null);

            when(masterTemplateDao.findLatestActiveTemplateByType(eq(DOC_TYPE), anyLong()))
                .thenReturn(Mono.just(template));
            when(accessControlService.canUpload(any(), eq(REQUESTOR_TYPE)))
                .thenReturn(true);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                    && e.getMessage().contains("requires referenceKeyType"))
                .verify();
        }

        @Test
        @DisplayName("Should succeed when template has no document_matching_config")
        void shouldSucceedWhenNoMatchingConfig() {
            // Given
            MasterTemplateDto template = createTemplate(false); // false to skip single_document_flag logic
            // template has no documentMatchingConfig
            DocumentUploadRequest request = createUploadRequest();

            setupMocksForSuccessfulUpload(template);

            // When
            Mono<InlineResponse200> result = processor.uploadDocument(request, REQUESTOR_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getId() != null)
                .verifyComplete();
        }

        private MasterTemplateDto createTemplateWithMatchingConfig(String referenceKeyType) {
            MasterTemplateDto template = createTemplate(false); // false to skip single_document_flag logic
            String configJson = String.format(
                "{\"matchBy\":\"reference_key\",\"referenceKeyType\":\"%s\"}", referenceKeyType);
            template.setDocumentMatchingConfig(configJson);
            return template;
        }
    }
}
