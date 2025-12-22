package com.documenthub.service;

import com.documenthub.dto.upload.DocumentUploadRequest;
import com.documenthub.dto.upload.DocumentUploadResponse;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.integration.ecms.EcmsClientException;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentUploadService.
 * Tests document upload, template validation, and shared flag handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentUploadServiceTest {

    @Mock
    private EcmsClient ecmsClient;

    @Mock
    private StorageIndexRepository storageIndexRepository;

    @Mock
    private TemplateCacheService templateCacheService;

    @Mock
    private DocumentAccessControlService accessControlService;

    @Mock
    private FilePart filePart;

    private ObjectMapper objectMapper;
    private DocumentUploadService uploadService;

    // Test data
    private static final UUID TEMPLATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ECMS_DOC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACCOUNT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String USER_ID = "test-user";
    private static final String REQUESTOR_TYPE_SYSTEM = "SYSTEM";
    private static final String REQUESTOR_TYPE_CUSTOMER = "CUSTOMER";
    private static final String TEMPLATE_TYPE = "WELCOME_LETTER";
    private static final Integer TEMPLATE_VERSION = 1;
    private static final String FILE_NAME = "test-document.pdf";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        uploadService = new DocumentUploadService(
            ecmsClient,
            storageIndexRepository,
            templateCacheService,
            accessControlService,
            objectMapper
        );
        // Default: allow upload for SYSTEM
        when(accessControlService.canUpload(any(), eq(REQUESTOR_TYPE_SYSTEM))).thenReturn(true);
    }

    private MasterTemplateDefinitionEntity createTemplate(boolean sharedDocumentFlag) {
        return MasterTemplateDefinitionEntity.builder()
            .masterTemplateId(TEMPLATE_ID)
            .templateType(TEMPLATE_TYPE)
            .templateVersion(TEMPLATE_VERSION)
            .activeFlag(true)
            .sharedDocumentFlag(sharedDocumentFlag)
            .build();
    }

    private DocumentUploadRequest createUploadRequest(Boolean sharedFlag) {
        return DocumentUploadRequest.builder()
            .templateType(TEMPLATE_TYPE)
            .templateVersion(TEMPLATE_VERSION)
            .fileName(FILE_NAME)
            .displayName("Test Document")
            .accountId(ACCOUNT_ID)
            .customerId(CUSTOMER_ID)
            .sharedFlag(sharedFlag)
            .build();
    }

    private EcmsDocumentResponse createEcmsResponse() {
        EcmsDocumentResponse response = new EcmsDocumentResponse();
        response.setId(ECMS_DOC_ID);
        response.setName("Test Document");
        response.setLink("https://ecms.example.com/documents/" + ECMS_DOC_ID);
        return response;
    }

    // ========================================================================
    // Template Validation Tests
    // ========================================================================
    @Nested
    @DisplayName("Template Validation")
    class TemplateValidationTests {

        @Test
        @DisplayName("Should fail when template not found")
        void shouldFailWhenTemplateNotFound() {
            // Given
            DocumentUploadRequest request = createUploadRequest(null);
            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.empty());

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                    e.getMessage().contains("Template not found"))
                .verify();

            // Verify ECMS was never called
            verify(ecmsClient, never()).uploadDocument(any(byte[].class), any());
        }

        @Test
        @DisplayName("Should proceed when template exists")
        void shouldProceedWhenTemplateExists() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextMatches(response ->
                    response.getStatus().equals("SUCCESS") &&
                    response.getEcmsDocumentId().equals(ECMS_DOC_ID))
                .verifyComplete();
        }
    }

    // ========================================================================
    // Upload Permission Tests
    // ========================================================================
    @Nested
    @DisplayName("Upload Permission")
    class UploadPermissionTests {

        @Test
        @DisplayName("Should deny upload when requestor lacks permission")
        void shouldDenyUploadWhenNoPermission() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(accessControlService.canUpload(any(), eq(REQUESTOR_TYPE_CUSTOMER)))
                .thenReturn(false);

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_CUSTOMER))
                .expectErrorMatches(e -> e instanceof SecurityException &&
                    e.getMessage().contains("Upload not permitted"))
                .verify();

            // Verify ECMS was never called
            verify(ecmsClient, never()).uploadDocument(any(byte[].class), any());
        }

        @Test
        @DisplayName("Should allow upload when requestor has permission")
        void shouldAllowUploadWhenHasPermission() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(accessControlService.canUpload(any(), eq("AGENT"))).thenReturn(true);
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, "AGENT"))
                .expectNextMatches(response -> response.getStatus().equals("SUCCESS"))
                .verifyComplete();
        }
    }

    // ========================================================================
    // Shared Flag Tests
    // ========================================================================
    @Nested
    @DisplayName("Shared Flag Handling")
    class SharedFlagTests {

        @Test
        @DisplayName("Should inherit sharedFlag=true from template")
        void shouldInheritSharedFlagFromTemplate() {
            // Given: Template has sharedDocumentFlag=true
            MasterTemplateDefinitionEntity template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest(null); // Request doesn't set sharedFlag
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify sharedFlag is set to true
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            assertTrue(captor.getValue().getSharedFlag(),
                "sharedFlag should be true when template has sharedDocumentFlag=true");
        }

        @Test
        @DisplayName("Should enforce sharedFlag=true from template even when request says false")
        void shouldEnforceTemplateSahredFlagOverRequest() {
            // Given: Template has sharedDocumentFlag=true, request has sharedFlag=false
            MasterTemplateDefinitionEntity template = createTemplate(true);
            DocumentUploadRequest request = createUploadRequest(false);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify sharedFlag is still true (template enforces it)
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            assertTrue(captor.getValue().getSharedFlag(),
                "sharedFlag should be true when template has sharedDocumentFlag=true, regardless of request");
        }

        @Test
        @DisplayName("Should allow request to set sharedFlag=true for non-shared template")
        void shouldAllowRequestToSetSharedFlagTrue() {
            // Given: Template has sharedDocumentFlag=false, request has sharedFlag=true
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(true);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify sharedFlag is true from request
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            assertTrue(captor.getValue().getSharedFlag(),
                "sharedFlag should be true when request sets sharedFlag=true");
        }

        @Test
        @DisplayName("Should default sharedFlag to false when template and request are both false/null")
        void shouldDefaultSharedFlagToFalse() {
            // Given: Template has sharedDocumentFlag=false, request has sharedFlag=null
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify sharedFlag is false
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            assertFalse(captor.getValue().getSharedFlag(),
                "sharedFlag should be false when both template and request don't specify shared");
        }

        @Test
        @DisplayName("Should handle null sharedDocumentFlag in template as false")
        void shouldHandleNullTemplateSharedFlag() {
            // Given: Template has sharedDocumentFlag=null
            MasterTemplateDefinitionEntity template = MasterTemplateDefinitionEntity.builder()
                .masterTemplateId(TEMPLATE_ID)
                .templateType(TEMPLATE_TYPE)
                .templateVersion(TEMPLATE_VERSION)
                .activeFlag(true)
                .sharedDocumentFlag(null) // Explicitly null
                .build();
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify sharedFlag is false
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            assertFalse(captor.getValue().getSharedFlag(),
                "sharedFlag should be false when template sharedDocumentFlag is null");
        }
    }

    // ========================================================================
    // Storage Index Creation Tests
    // ========================================================================
    @Nested
    @DisplayName("Storage Index Creation")
    class StorageIndexCreationTests {

        @Test
        @DisplayName("Should create storage index with correct fields")
        void shouldCreateStorageIndexWithCorrectFields() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                .templateType(TEMPLATE_TYPE)
                .templateVersion(TEMPLATE_VERSION)
                .fileName(FILE_NAME)
                .displayName("Test Document")
                .accountId(ACCOUNT_ID)
                .customerId(CUSTOMER_ID)
                .referenceKey("REF-123")
                .referenceKeyType("ORDER_ID")
                .sharedFlag(false)
                .startDate(1700000000000L)
                .endDate(1800000000000L)
                .build();
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify all fields are set correctly
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            StorageIndexEntity saved = captor.getValue();

            assertNotNull(saved.getStorageIndexId());
            assertEquals(TEMPLATE_ID, saved.getMasterTemplateId());
            assertEquals(TEMPLATE_VERSION, saved.getTemplateVersion());
            assertEquals(TEMPLATE_TYPE, saved.getTemplateType());
            assertEquals("ECMS", saved.getStorageVendor());
            assertEquals(ECMS_DOC_ID, saved.getStorageDocumentKey());
            assertEquals(FILE_NAME, saved.getFileName());
            assertEquals("REF-123", saved.getReferenceKey());
            assertEquals("ORDER_ID", saved.getReferenceKeyType());
            assertEquals(ACCOUNT_ID, saved.getAccountKey());
            assertEquals(CUSTOMER_ID, saved.getCustomerKey());
            assertTrue(saved.getAccessibleFlag());
            assertFalse(saved.getArchiveIndicator());
            assertEquals(1L, saved.getVersionNumber());
            assertEquals("ACTIVE", saved.getRecordStatus());
            assertEquals(USER_ID, saved.getCreatedBy());
            assertNotNull(saved.getCreatedTimestamp());
            assertNotNull(saved.getDocCreationDate());
            assertEquals(1700000000000L, saved.getStartDate());
            assertEquals(1800000000000L, saved.getEndDate());
        }

        @Test
        @DisplayName("Should store metadata as JSON")
        void shouldStoreMetadataAsJson() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                .templateType(TEMPLATE_TYPE)
                .templateVersion(TEMPLATE_VERSION)
                .fileName(FILE_NAME)
                .metadata(Map.of("key1", "value1", "key2", 123))
                .build();
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then: Verify metadata is set
            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(storageIndexRepository).save(captor.capture());
            assertNotNull(captor.getValue().getDocMetadata());
        }
    }

    // ========================================================================
    // ECMS Integration Tests
    // ========================================================================
    @Nested
    @DisplayName("ECMS Integration")
    class EcmsIntegrationTests {

        @Test
        @DisplayName("Should propagate ECMS errors")
        void shouldPropagateEcmsErrors() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.error(new EcmsClientException(500, "ECMS server error")));

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectError(EcmsClientException.class)
                .verify();

            // Verify storage was never saved
            verify(storageIndexRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should pass correct data to ECMS client")
        void shouldPassCorrectDataToEcmsClient() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();
            byte[] fileContent = new byte[]{1, 2, 3, 4, 5};

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            StepVerifier.create(uploadService.uploadDocument(fileContent, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .expectNextCount(1)
                .verifyComplete();

            // Then
            ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<DocumentUploadRequest> requestCaptor = ArgumentCaptor.forClass(DocumentUploadRequest.class);
            verify(ecmsClient).uploadDocument(contentCaptor.capture(), requestCaptor.capture());

            assertArrayEquals(fileContent, contentCaptor.getValue());
            assertEquals(TEMPLATE_TYPE, requestCaptor.getValue().getTemplateType());
            assertEquals(FILE_NAME, requestCaptor.getValue().getFileName());
        }
    }

    // ========================================================================
    // Response Building Tests
    // ========================================================================
    @Nested
    @DisplayName("Response Building")
    class ResponseBuildingTests {

        @Test
        @DisplayName("Should build complete success response")
        void shouldBuildCompleteSuccessResponse() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();
            EcmsDocumentResponse.FileSize fileSize = new EcmsDocumentResponse.FileSize();
            fileSize.setValue(1024);
            fileSize.setUnit("bytes");
            ecmsResponse.setFileSize(fileSize);

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .assertNext(response -> {
                    assertEquals("SUCCESS", response.getStatus());
                    assertEquals("Document uploaded successfully", response.getMessage());
                    assertNotNull(response.getStorageIndexId());
                    assertEquals(ECMS_DOC_ID, response.getEcmsDocumentId());
                    assertEquals(FILE_NAME, response.getFileName());
                    assertEquals(TEMPLATE_TYPE, response.getTemplateType());
                    assertEquals(TEMPLATE_VERSION, response.getTemplateVersion());
                    assertEquals(ACCOUNT_ID, response.getAccountId());
                    assertEquals(CUSTOMER_ID, response.getCustomerId());
                    assertNotNull(response.getDocumentLink());
                    assertNotNull(response.getFileSize());
                    assertEquals(Integer.valueOf(1024), response.getFileSize().getValue());
                    assertEquals("bytes", response.getFileSize().getUnit());
                    assertNotNull(response.getCreatedAt());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle null fileSize from ECMS")
        void shouldHandleNullFileSizeFromEcms() {
            // Given
            MasterTemplateDefinitionEntity template = createTemplate(false);
            DocumentUploadRequest request = createUploadRequest(null);
            EcmsDocumentResponse ecmsResponse = createEcmsResponse();
            ecmsResponse.setFileSize(null);

            when(templateCacheService.getTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION))
                .thenReturn(Mono.just(template));
            when(ecmsClient.uploadDocument(any(byte[].class), any()))
                .thenReturn(Mono.just(ecmsResponse));
            when(storageIndexRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When/Then
            StepVerifier.create(uploadService.uploadDocument(new byte[]{1, 2, 3}, request, USER_ID, REQUESTOR_TYPE_SYSTEM))
                .assertNext(response -> {
                    assertEquals("SUCCESS", response.getStatus());
                    assertNull(response.getFileSize());
                })
                .verifyComplete();
        }
    }
}
