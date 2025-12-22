package com.documenthub.service;

import com.documenthub.config.ReferenceKeyConfig;
import com.documenthub.dto.upload.DocumentUploadRequest;
import com.documenthub.dto.upload.DocumentUploadResponse;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final DocumentAccessControlService accessControlService;
    private final ReferenceKeyConfig referenceKeyConfig;
    private final ObjectMapper objectMapper;

    /**
     * Upload a document with file part (multipart upload)
     *
     * @param filePart      The file to upload
     * @param request       Upload request metadata
     * @param userId        ID of the user performing the upload
     * @param requestorType Type of requestor (CUSTOMER, AGENT, SYSTEM)
     * @return Upload response with storage index and ECMS document IDs
     */
    public Mono<DocumentUploadResponse> uploadDocument(FilePart filePart,
                                                        DocumentUploadRequest request,
                                                        String userId,
                                                        String requestorType) {
        log.info("Processing document upload: templateType={}, fileName={}, userId={}, requestorType={}",
            request.getTemplateType(), request.getFileName(), userId, requestorType);

        // First validate the template exists and check upload permission
        return validateTemplate(request.getTemplateType(), request.getTemplateVersion())
            .flatMap(template -> {
                // Check upload permission
                if (!accessControlService.canUpload(template, requestorType)) {
                    log.warn("Upload permission denied: templateType={}, requestorType={}",
                        request.getTemplateType(), requestorType);
                    return Mono.error(new SecurityException(
                        "Upload not permitted for requestor type: " + requestorType));
                }

                // Validate reference_key and reference_key_type based on template's document_matching_config
                try {
                    validateReferenceKey(request, template);
                } catch (IllegalArgumentException e) {
                    log.warn("Reference key validation failed: {}", e.getMessage());
                    return Mono.error(e);
                }

                // Upload to ECMS
                return ecmsClient.uploadDocument(filePart, request)
                    .flatMap(ecmsResponse ->
                        // Create storage index entry
                        createStorageIndexEntry(request, ecmsResponse, template, userId)
                            .map(storageIndex -> buildUploadResponse(storageIndex, ecmsResponse))
                    );
            })
            .doOnSuccess(resp -> log.info("Document upload completed: storageIndexId={}, ecmsId={}",
                resp.getStorageIndexId(), resp.getEcmsDocumentId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    /**
     * Upload a document from byte array
     *
     * @param fileContent   File content as bytes
     * @param request       Upload request metadata
     * @param userId        ID of the user performing the upload
     * @param requestorType Type of requestor (CUSTOMER, AGENT, SYSTEM)
     * @return Upload response
     */
    public Mono<DocumentUploadResponse> uploadDocument(byte[] fileContent,
                                                        DocumentUploadRequest request,
                                                        String userId,
                                                        String requestorType) {
        log.info("Processing document upload from bytes: templateType={}, fileName={}, size={}, userId={}, requestorType={}",
            request.getTemplateType(), request.getFileName(), fileContent.length, userId, requestorType);

        return validateTemplate(request.getTemplateType(), request.getTemplateVersion())
            .flatMap(template -> {
                // Check upload permission
                if (!accessControlService.canUpload(template, requestorType)) {
                    log.warn("Upload permission denied: templateType={}, requestorType={}",
                        request.getTemplateType(), requestorType);
                    return Mono.error(new SecurityException(
                        "Upload not permitted for requestor type: " + requestorType));
                }

                // Validate reference_key and reference_key_type based on template's document_matching_config
                try {
                    validateReferenceKey(request, template);
                } catch (IllegalArgumentException e) {
                    log.warn("Reference key validation failed: {}", e.getMessage());
                    return Mono.error(e);
                }

                return ecmsClient.uploadDocument(fileContent, request)
                    .flatMap(ecmsResponse ->
                        createStorageIndexEntry(request, ecmsResponse, template, userId)
                            .map(storageIndex -> buildUploadResponse(storageIndex, ecmsResponse))
                    );
            })
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
     * Validate reference_key and reference_key_type based on template's document_matching_config.
     * If the template has document_matching_config, both fields are required.
     *
     * @param request  The upload request
     * @param template The template definition
     * @throws IllegalArgumentException if validation fails
     */
    private void validateReferenceKey(DocumentUploadRequest request, MasterTemplateDefinitionEntity template) {
        Json documentMatchingConfig = template.getDocumentMatchingConfig();

        // If template has document_matching_config, reference_key and reference_key_type are required
        if (documentMatchingConfig != null) {
            try {
                JsonNode configNode = objectMapper.readTree(documentMatchingConfig.asString());

                // Check if config has matchBy field (indicates active matching configuration)
                if (configNode.has("matchBy")) {
                    String expectedKeyType = configNode.has("referenceKeyType")
                        ? configNode.get("referenceKeyType").asText()
                        : null;

                    // Validate reference_key is provided
                    if (request.getReferenceKey() == null || request.getReferenceKey().isBlank()) {
                        throw new IllegalArgumentException(
                            "reference_key is required for template '" + template.getTemplateType() +
                            "' which has document_matching_config with referenceKeyType: " + expectedKeyType);
                    }

                    // Validate reference_key_type is provided
                    if (request.getReferenceKeyType() == null || request.getReferenceKeyType().isBlank()) {
                        throw new IllegalArgumentException(
                            "reference_key_type is required for template '" + template.getTemplateType() +
                            "'. Expected type: " + expectedKeyType);
                    }

                    // Validate reference_key_type is a valid configured value
                    if (!referenceKeyConfig.isValid(request.getReferenceKeyType())) {
                        throw new IllegalArgumentException(
                            "Invalid reference_key_type: '" + request.getReferenceKeyType() +
                            "'. Allowed values: " + referenceKeyConfig.getAllowedTypesString());
                    }

                    // Validate reference_key_type matches template's expected type
                    if (expectedKeyType != null && !expectedKeyType.equals(request.getReferenceKeyType())) {
                        throw new IllegalArgumentException(
                            "reference_key_type mismatch for template '" + template.getTemplateType() +
                            "'. Expected: " + expectedKeyType + ", Provided: " + request.getReferenceKeyType());
                    }

                    log.debug("Reference key validation passed: referenceKey={}, referenceKeyType={}",
                        request.getReferenceKey(), request.getReferenceKeyType());
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse document_matching_config for template {}: {}",
                    template.getTemplateType(), e.getMessage());
                // Don't fail on parse error, just log warning
            }
        } else {
            // Template has no document_matching_config
            // If reference_key is provided, reference_key_type must also be provided
            if (request.getReferenceKey() != null && !request.getReferenceKey().isBlank()) {
                if (request.getReferenceKeyType() == null || request.getReferenceKeyType().isBlank()) {
                    throw new IllegalArgumentException(
                        "reference_key_type is required when reference_key is provided");
                }
                if (!referenceKeyConfig.isValid(request.getReferenceKeyType())) {
                    throw new IllegalArgumentException(
                        "Invalid reference_key_type: '" + request.getReferenceKeyType() +
                        "'. Allowed values: " + referenceKeyConfig.getAllowedTypesString());
                }
            }
        }
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
