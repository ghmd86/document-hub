package com.documenthub.service;

import com.documenthub.dto.upload.DocumentUploadRequest;
import com.documenthub.dto.upload.DocumentUploadResponse;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for document upload operations
 * Orchestrates upload to ECMS and creates storage index entry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadService {

    private static final String STORAGE_VENDOR_ECMS = "ECMS";

    private final EcmsClient ecmsClient;
    private final StorageIndexRepository storageIndexRepository;
    private final TemplateCacheService templateCacheService;
    private final ObjectMapper objectMapper;

    /**
     * Upload a document with file part (multipart upload)
     *
     * @param filePart The file to upload
     * @param request  Upload request metadata
     * @param userId   ID of the user performing the upload
     * @return Upload response with storage index and ECMS document IDs
     */
    public Mono<DocumentUploadResponse> uploadDocument(FilePart filePart,
                                                        DocumentUploadRequest request,
                                                        String userId) {
        log.info("Processing document upload: templateType={}, fileName={}, userId={}",
            request.getTemplateType(), request.getFileName(), userId);

        // First validate the template exists
        return validateTemplate(request.getTemplateType(), request.getTemplateVersion())
            .flatMap(template ->
                // Upload to ECMS
                ecmsClient.uploadDocument(filePart, request)
                    .flatMap(ecmsResponse ->
                        // Create storage index entry
                        createStorageIndexEntry(request, ecmsResponse, template, userId)
                            .map(storageIndex -> buildUploadResponse(storageIndex, ecmsResponse))
                    )
            )
            .doOnSuccess(resp -> log.info("Document upload completed: storageIndexId={}, ecmsId={}",
                resp.getStorageIndexId(), resp.getEcmsDocumentId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    /**
     * Upload a document from byte array
     *
     * @param fileContent File content as bytes
     * @param request     Upload request metadata
     * @param userId      ID of the user performing the upload
     * @return Upload response
     */
    public Mono<DocumentUploadResponse> uploadDocument(byte[] fileContent,
                                                        DocumentUploadRequest request,
                                                        String userId) {
        log.info("Processing document upload from bytes: templateType={}, fileName={}, size={}, userId={}",
            request.getTemplateType(), request.getFileName(), fileContent.length, userId);

        return validateTemplate(request.getTemplateType(), request.getTemplateVersion())
            .flatMap(template ->
                ecmsClient.uploadDocument(fileContent, request)
                    .flatMap(ecmsResponse ->
                        createStorageIndexEntry(request, ecmsResponse, template, userId)
                            .map(storageIndex -> buildUploadResponse(storageIndex, ecmsResponse))
                    )
            )
            .doOnSuccess(resp -> log.info("Document upload completed: storageIndexId={}, ecmsId={}",
                resp.getStorageIndexId(), resp.getEcmsDocumentId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    /**
     * Validate that the template exists and is active (uses cache)
     */
    private Mono<MasterTemplateDefinitionEntity> validateTemplate(String templateType, Integer templateVersion) {
        return templateCacheService.getTemplate(templateType, templateVersion)
            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                "Template not found: type=" + templateType + ", version=" + templateVersion)));
    }

    /**
     * Create a storage index entry for the uploaded document
     */
    private Mono<StorageIndexEntity> createStorageIndexEntry(DocumentUploadRequest request,
                                                              EcmsDocumentResponse ecmsResponse,
                                                              MasterTemplateDefinitionEntity template,
                                                              String userId) {
        UUID storageIndexId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        long currentTimeMs = System.currentTimeMillis();

        // Determine sharedFlag: inherit from template's sharedDocumentFlag
        // Request can override to true, but cannot override template's true to false
        boolean sharedFlag = Boolean.TRUE.equals(template.getSharedDocumentFlag())
            || Boolean.TRUE.equals(request.getSharedFlag());

        StorageIndexEntity entity = StorageIndexEntity.builder()
            .storageIndexId(storageIndexId)
            .masterTemplateId(template.getMasterTemplateId())
            .templateVersion(request.getTemplateVersion())
            .templateType(request.getTemplateType())
            .storageVendor(STORAGE_VENDOR_ECMS)
            .storageDocumentKey(ecmsResponse.getId())
            .fileName(request.getFileName())
            .referenceKey(request.getReferenceKey())
            .referenceKeyType(request.getReferenceKeyType())
            .accountKey(request.getAccountId())
            .customerKey(request.getCustomerId())
            .docCreationDate(currentTimeMs)
            .accessibleFlag(true)
            .sharedFlag(sharedFlag)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .createdBy(userId)
            .createdTimestamp(now)
            .archiveIndicator(false)
            .versionNumber(1L)
            .recordStatus("ACTIVE")
            .build();

        // Set metadata if provided
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            try {
                String metadataJson = objectMapper.writeValueAsString(request.getMetadata());
                entity.setDocMetadata(Json.of(metadataJson));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize metadata, skipping: {}", e.getMessage());
            }
        }

        log.debug("Creating storage index entry: id={}, ecmsDocId={}, templateType={}, sharedFlag={}",
            storageIndexId, ecmsResponse.getId(), request.getTemplateType(), sharedFlag);

        return storageIndexRepository.save(entity);
    }

    /**
     * Build the upload response from storage index and ECMS response
     */
    private DocumentUploadResponse buildUploadResponse(StorageIndexEntity storageIndex,
                                                        EcmsDocumentResponse ecmsResponse) {
        DocumentUploadResponse.FileSize fileSize = null;
        if (ecmsResponse.getFileSize() != null) {
            fileSize = DocumentUploadResponse.FileSize.builder()
                .value(ecmsResponse.getFileSize().getValue())
                .unit(ecmsResponse.getFileSize().getUnit())
                .build();
        }

        return DocumentUploadResponse.builder()
            .storageIndexId(storageIndex.getStorageIndexId())
            .ecmsDocumentId(ecmsResponse.getId())
            .fileName(storageIndex.getFileName())
            .displayName(ecmsResponse.getName())
            .templateType(storageIndex.getTemplateType())
            .templateVersion(storageIndex.getTemplateVersion())
            .accountId(storageIndex.getAccountKey())
            .customerId(storageIndex.getCustomerKey())
            .referenceKey(storageIndex.getReferenceKey())
            .referenceKeyType(storageIndex.getReferenceKeyType())
            .documentLink(ecmsResponse.getLink())
            .fileSize(fileSize)
            .createdAt(storageIndex.getCreatedTimestamp())
            .status("SUCCESS")
            .message("Document uploaded successfully")
            .build();
    }
}
