package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.dao.StorageIndexDao;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.model.*;
import com.documenthub.service.DocumentAccessControlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Processor for document management operations.
 * Handles upload, download, delete, and metadata retrieval.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentManagementProcessor {

    private static final String STORAGE_VENDOR_ECMS = "ECMS";

    private final MasterTemplateDao masterTemplateDao;
    private final StorageIndexDao storageIndexDao;
    private final EcmsClient ecmsClient;
    private final DocumentAccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    /**
     * Upload a document according to the API spec
     */
    public Mono<InlineResponse200> uploadDocument(
            FilePart content,
            String documentType,
            String createdBy,
            String metadataJson,
            UUID templateId,
            String referenceKey,
            String referenceKeyType,
            UUID accountKey,
            UUID customerKey,
            String category,
            String fileName,
            Long activeStartDate,
            Long activeEndDate,
            UUID threadId,
            UUID correlationId) {

        log.info("Processing document upload: documentType={}, fileName={}, createdBy={}",
            documentType, fileName, createdBy);

        // Find the template by document type
        return findTemplateByDocumentType(documentType)
            .flatMap(template -> {
                // Parse metadata
                List<MetadataNode> metadata = parseMetadata(metadataJson);

                // Upload to ECMS
                return ecmsClient.uploadDocument(content, buildUploadRequest(
                        documentType, fileName, accountKey, customerKey,
                        referenceKey, referenceKeyType, metadata))
                    .flatMap(ecmsResponse -> {
                        // Create storage index entry
                        UUID storageIndexId = UUID.randomUUID();
                        LocalDateTime now = LocalDateTime.now();
                        long currentTimeMs = System.currentTimeMillis();

                        StorageIndexEntity entity = StorageIndexEntity.builder()
                            .storageIndexId(storageIndexId)
                            .masterTemplateId(template.getMasterTemplateId())
                            .templateVersion(template.getTemplateVersion())
                            .templateType(documentType)
                            .storageVendor(STORAGE_VENDOR_ECMS)
                            .storageDocumentKey(ecmsResponse.getId())
                            .fileName(fileName)
                            .referenceKey(referenceKey)
                            .referenceKeyType(referenceKeyType)
                            .accountKey(accountKey)
                            .customerKey(customerKey)
                            .docCreationDate(currentTimeMs)
                            .accessibleFlag(true)
                            .sharedFlag(Boolean.TRUE.equals(template.getSharedDocumentFlag()))
                            .startDate(activeStartDate)
                            .endDate(activeEndDate)
                            .createdBy(createdBy)
                            .createdTimestamp(now)
                            .archiveIndicator(false)
                            .versionNumber(1L)
                            .recordStatus("ACTIVE")
                            .build();

                        // Set metadata
                        if (metadata != null && !metadata.isEmpty()) {
                            try {
                                String metadataStr = objectMapper.writeValueAsString(metadata);
                                entity.setDocMetadata(Json.of(metadataStr));
                            } catch (JsonProcessingException e) {
                                log.warn("Failed to serialize metadata: {}", e.getMessage());
                            }
                        }

                        return storageIndexDao.save(entity)
                            .map(saved -> {
                                InlineResponse200 response = new InlineResponse200();
                                response.setId(saved.getStorageIndexId());
                                return response;
                            });
                    });
            })
            .doOnSuccess(resp -> log.info("Document upload completed: id={}", resp.getId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    /**
     * Download a document
     */
    public Mono<DocumentDownloadResult> downloadDocument(String documentId, String requestorType) {
        log.info("Processing document download: documentId={}, requestorType={}", documentId, requestorType);

        return findStorageIndex(documentId)
            .flatMap(storageIndex -> {
                // Check access control
                return masterTemplateDao.findByTypeAndVersion(
                        storageIndex.getTemplateType(), storageIndex.getTemplateVersion())
                    .flatMap(template -> {
                        if (!accessControlService.hasAccess(template, requestorType, "Download")) {
                            return Mono.error(new SecurityException("Access denied for download"));
                        }

                        // Download from ECMS (convert UUID to String)
                        String ecmsDocId = storageIndex.getStorageDocumentKey().toString();
                        return ecmsClient.downloadDocument(ecmsDocId)
                            .<DocumentDownloadResult>map(dataBufferFlux -> DocumentDownloadResult.builder()
                                .content(dataBufferFlux)
                                .fileName(storageIndex.getFileName())
                                .mimeType(determineMimeType(storageIndex.getFileName()))
                                .build());
                    });
            })
            .doOnSuccess(result -> log.info("Document download completed: documentId={}", documentId))
            .doOnError(e -> log.error("Document download failed: documentId={}", documentId, e));
    }

    /**
     * Delete a document (soft delete)
     */
    public Mono<Void> deleteDocument(String documentId, String requestorType) {
        log.info("Processing document delete: documentId={}, requestorType={}", documentId, requestorType);

        return findStorageIndex(documentId)
            .flatMap(storageIndex -> {
                // Check access control
                return masterTemplateDao.findByTypeAndVersion(
                        storageIndex.getTemplateType(), storageIndex.getTemplateVersion())
                    .flatMap(template -> {
                        if (!accessControlService.hasAccess(template, requestorType, "Delete")) {
                            return Mono.error(new SecurityException("Access denied for delete"));
                        }

                        // Soft delete the storage index entry
                        return storageIndexDao.softDelete(storageIndex)
                            .then();
                    });
            })
            .doOnSuccess(v -> log.info("Document delete completed: documentId={}", documentId))
            .doOnError(e -> log.error("Document delete failed: documentId={}", documentId, e));
    }

    /**
     * Get document metadata
     */
    public Mono<DocumentDetailsNode> getDocumentMetadata(String documentId, String requestorType, boolean includeDownloadUrl) {
        log.info("Processing document metadata request: documentId={}, requestorType={}, includeDownloadUrl={}",
            documentId, requestorType, includeDownloadUrl);

        return findStorageIndex(documentId)
            .flatMap(storageIndex -> {
                // Check access control
                return masterTemplateDao.findByTypeAndVersion(
                        storageIndex.getTemplateType(), storageIndex.getTemplateVersion())
                    .map(template -> {
                        if (!accessControlService.hasAccess(template, requestorType, "View")) {
                            throw new SecurityException("Access denied for view");
                        }

                        return buildDocumentDetailsNode(storageIndex, template, requestorType, includeDownloadUrl);
                    });
            })
            .doOnSuccess(result -> log.info("Document metadata retrieved: documentId={}", documentId))
            .doOnError(e -> log.error("Document metadata retrieval failed: documentId={}", documentId, e));
    }

    /**
     * Find storage index by document ID (which is the storage_index_id or encoded reference)
     */
    private Mono<StorageIndexEntity> findStorageIndex(String documentId) {
        // First try to parse as UUID (storage_index_id)
        try {
            UUID storageIndexId = UUID.fromString(documentId);
            return storageIndexDao.findById(storageIndexId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found: " + documentId)));
        } catch (IllegalArgumentException e) {
            // Not a UUID, might be a base64-encoded reference
            // For now, return error - can be extended to support encoded references
            return Mono.error(new IllegalArgumentException("Invalid document ID format: " + documentId));
        }
    }

    /**
     * Find template by document type
     */
    private Mono<MasterTemplateDefinitionEntity> findTemplateByDocumentType(String documentType) {
        long currentDate = System.currentTimeMillis();
        return masterTemplateDao.findActiveTemplatesByLineOfBusiness(null, currentDate)
            .filter(t -> documentType.equals(t.getTemplateType()))
            .next()
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Template not found for document type: " + documentType)));
    }

    /**
     * Parse metadata JSON into list of MetadataNode
     */
    private List<MetadataNode> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<List<MetadataNode>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Build upload request for ECMS
     */
    private com.documenthub.dto.upload.DocumentUploadRequest buildUploadRequest(
            String documentType,
            String fileName,
            UUID accountKey,
            UUID customerKey,
            String referenceKey,
            String referenceKeyType,
            List<MetadataNode> metadata) {

        Map<String, Object> metadataMap = new HashMap<>();
        if (metadata != null) {
            for (MetadataNode node : metadata) {
                metadataMap.put(node.getKey(), node.getValue());
            }
        }

        return com.documenthub.dto.upload.DocumentUploadRequest.builder()
            .templateType(documentType)
            .templateVersion(1) // Default version
            .fileName(fileName)
            .accountId(accountKey)
            .customerId(customerKey)
            .referenceKey(referenceKey)
            .referenceKeyType(referenceKeyType)
            .metadata(metadataMap)
            .build();
    }

    /**
     * Build DocumentDetailsNode from storage index and template
     */
    private DocumentDetailsNode buildDocumentDetailsNode(
            StorageIndexEntity storageIndex,
            MasterTemplateDefinitionEntity template,
            String requestorType,
            boolean includeDownloadUrl) {

        DocumentDetailsNode node = new DocumentDetailsNode();
        node.setDocumentId(storageIndex.getStorageIndexId().toString());
        node.setDisplayName(storageIndex.getFileName());
        node.setDocumentType(storageIndex.getTemplateType());
        node.setCategory(template.getTemplateCategory());
        node.setLineOfBusiness(template.getLineOfBusiness());
        node.setMimeType(determineMimeType(storageIndex.getFileName()));
        node.setDatePosted(storageIndex.getDocCreationDate());

        // Parse and set metadata
        if (storageIndex.getDocMetadata() != null) {
            try {
                List<MetadataNode> metadata = objectMapper.readValue(
                    storageIndex.getDocMetadata().asString(),
                    new TypeReference<List<MetadataNode>>() {});
                node.setMetadata(metadata);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse document metadata: {}", e.getMessage());
            }
        }

        // Build links
        Links links = new Links();

        // Download link
        if (accessControlService.hasAccess(template, requestorType, "Download")) {
            LinksDownload downloadLink = new LinksDownload();
            downloadLink.setHref("/documents/" + storageIndex.getStorageIndexId());
            downloadLink.setType("GET");
            downloadLink.setRel("download");
            downloadLink.setTitle("Download this document");
            downloadLink.setResponseTypes(Arrays.asList("application/pdf", "application/octet-stream"));

            if (includeDownloadUrl) {
                // Set expiration time (10 minutes from now)
                long expiresAt = Instant.now().plusSeconds(600).getEpochSecond();
                downloadLink.setExpiresAt(expiresAt);
            }

            links.setDownload(downloadLink);
        }

        // Delete link
        if (accessControlService.hasAccess(template, requestorType, "Delete")) {
            LinksDelete deleteLink = new LinksDelete();
            deleteLink.setHref("/documents/" + storageIndex.getStorageIndexId());
            deleteLink.setType("DELETE");
            deleteLink.setRel("delete");
            deleteLink.setTitle("Delete this document");
            links.setDelete(deleteLink);
        }

        node.setLinks(links);

        return node;
    }

    /**
     * Determine MIME type from file name
     */
    private String determineMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".html")) {
            return "text/html";
        } else if (lowerName.endsWith(".xml")) {
            return "application/xml";
        } else if (lowerName.endsWith(".json")) {
            return "application/json";
        }
        return "application/octet-stream";
    }

    /**
     * Result of document download operation
     */
    @Data
    @Builder
    public static class DocumentDownloadResult {
        private Flux<DataBuffer> content;
        private String fileName;
        private String mimeType;
    }
}
