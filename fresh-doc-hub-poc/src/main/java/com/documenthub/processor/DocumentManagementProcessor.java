package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.dao.StorageIndexDao;
import com.documenthub.dto.DocumentUploadRequest;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Processor for document management operations.
 * Handles upload, download, delete, and metadata retrieval.
 *
 * <h2>Upload Flow Overview</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                         DOCUMENT UPLOAD FLOW                                │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │  Step 1: TEMPLATE LOOKUP                                                    │
 * │  ├── Query master_template_definition by documentType                       │
 * │  ├── Uses efficient indexed query (findLatestActiveTemplateByType)          │
 * │  └── Returns latest active version within date range                        │
 * │                                                                             │
 * │  Step 2: ACCESS CONTROL CHECK                                               │
 * │  ├── Validate requestorType has Upload permission                           │
 * │  ├── Check template's role_access configuration                             │
 * │  └── Reject with SecurityException if not permitted                         │
 * │                                                                             │
 * │  Step 3: REQUEST VALIDATION                                                 │
 * │  ├── Extract file bytes from MultipartFile                                  │
 * │  ├── Parse metadata JSON into MetadataNode list                             │
 * │  └── Validate required fields present                                       │
 * │                                                                             │
 * │  Step 4: SINGLE DOCUMENT ENFORCEMENT (conditional)                          │
 * │  ├── Check if template.single_document_flag = true                          │
 * │  ├── If true: close existing docs by setting end_date                       │
 * │  └── Uses referenceKey + referenceKeyType to find existing docs             │
 * │                                                                             │
 * │  Step 5: ECMS UPLOAD                                                        │
 * │  ├── Build ECMS request with metadata                                       │
 * │  ├── Call EcmsClient.uploadDocument()                                       │
 * │  └── Receive storage_document_key (ECMS document ID)                        │
 * │                                                                             │
 * │  Step 6: STORAGE INDEX CREATION                                             │
 * │  ├── Create StorageIndexEntity with all document metadata                   │
 * │  ├── Set shared_flag from template.shared_document_flag                     │
 * │  ├── Save to storage_index table                                            │
 * │  └── Return storage_index_id as document ID                                 │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Key Components</h3>
 * <ul>
 *   <li>{@link MasterTemplateDao} - Template lookup with caching</li>
 *   <li>{@link DocumentAccessControlService} - Role-based access control</li>
 *   <li>{@link EcmsClient} - External content management system integration</li>
 *   <li>{@link StorageIndexDao} - Document index persistence</li>
 * </ul>
 *
 * @see #uploadDocument(DocumentUploadRequest, String)
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
     * Upload a document according to the API spec.
     *
     * <p><b>Flow Steps:</b></p>
     * <ol>
     *   <li><b>Step 1 - Template Lookup:</b> Find active template by documentType
     *       via {@link #findTemplateByDocumentType(String)}</li>
     *   <li><b>Step 2-6:</b> Delegated to {@link #processUpload(MasterTemplateDefinitionEntity,
     *       DocumentUploadRequest, String)}</li>
     * </ol>
     *
     * @param request The upload request containing file, metadata, and identifiers
     * @param requestorType The type of requestor (e.g., "CUSTOMER", "EMPLOYEE")
     * @return Mono containing the upload response with document ID
     * @throws IllegalArgumentException if template not found for documentType
     * @throws SecurityException if requestorType lacks upload permission
     */
    public Mono<InlineResponse200> uploadDocument(DocumentUploadRequest request, String requestorType) {
        // Step 1: Log and lookup template
        logUploadRequest(request, requestorType);

        return findTemplateByDocumentType(request.getDocumentType())  // Step 1: Template lookup
            .flatMap(template -> processUpload(template, request, requestorType))  // Steps 2-6
            .doOnSuccess(resp -> log.info("Document upload completed: id={}", resp.getId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    private void logUploadRequest(DocumentUploadRequest request, String requestorType) {
        log.info("Processing document upload: documentType={}, fileName={}, createdBy={}, requestorType={}",
            request.getDocumentType(), request.getFileName(), request.getCreatedBy(), requestorType);
    }

    /**
     * Process the upload after template is found.
     *
     * <p><b>Steps 2-6 of Upload Flow:</b></p>
     * <ul>
     *   <li><b>Step 2 - Access Control:</b> Check if requestorType can upload to this template</li>
     *   <li><b>Step 3 - Request Validation:</b> Extract file bytes and parse metadata</li>
     *   <li><b>Step 4 - Single Doc Enforcement:</b> Close existing docs if single_document_flag=true</li>
     *   <li><b>Step 5 - ECMS Upload:</b> Upload file to external storage</li>
     *   <li><b>Step 6 - Storage Index:</b> Create index entry and return document ID</li>
     * </ul>
     */
    private Mono<InlineResponse200> processUpload(
            MasterTemplateDefinitionEntity template, DocumentUploadRequest request, String requestorType) {
        // Step 2: Access control check
        if (!accessControlService.canUpload(template, requestorType)) {
            return handleUploadPermissionDenied(request.getDocumentType(), requestorType);
        }

        // Step 3: Request validation - extract file bytes
        byte[] fileBytes = extractFileBytes(request);
        if (fileBytes == null) {
            return Mono.error(new IllegalArgumentException("Failed to read file content"));
        }

        // Step 3: Request validation - parse metadata
        List<MetadataNode> metadata = parseMetadata(request.getMetadataJson());

        // Step 4: Single document enforcement (if applicable)
        // Step 5 & 6: Upload to ECMS and save storage index
        return closeExistingDocsIfSingleDoc(template, request)
            .then(uploadToEcmsAndSave(template, request, metadata, fileBytes));
    }

    /**
     * Step 4: Single Document Enforcement.
     * If template has single_document_flag=true, close existing documents
     * by setting their end_date to the new document's start_date.
     */
    private Mono<Void> closeExistingDocsIfSingleDoc(
            MasterTemplateDefinitionEntity template, DocumentUploadRequest request) {
        if (!shouldCloseExistingDocs(template, request)) {
            return Mono.empty();
        }

        Long newDocStartDate = getStartDateForNewDoc(request);
        log.info("Single document flag is true - closing existing documents for refKey={}, newEndDate={}",
            request.getReferenceKey(), newDocStartDate);
        return storageIndexDao.updateEndDateByReferenceKey(
                request.getReferenceKey(),
                request.getReferenceKeyType(),
                template.getTemplateType(),
                newDocStartDate)
            .then();
    }

    private boolean shouldCloseExistingDocs(
            MasterTemplateDefinitionEntity template, DocumentUploadRequest request) {
        return Boolean.TRUE.equals(template.getSingleDocumentFlag())
            && request.getReferenceKey() != null
            && request.getReferenceKeyType() != null;
    }

    private Long getStartDateForNewDoc(DocumentUploadRequest request) {
        return request.getActiveStartDate() != null
            ? request.getActiveStartDate()
            : System.currentTimeMillis();
    }

    private Mono<InlineResponse200> handleUploadPermissionDenied(String docType, String requestorType) {
        log.warn("Upload permission denied: documentType={}, requestorType={}", docType, requestorType);
        return Mono.error(new SecurityException("Upload not permitted for requestor type: " + requestorType));
    }

    private byte[] extractFileBytes(DocumentUploadRequest request) {
        try {
            return request.getContent().getBytes();
        } catch (java.io.IOException e) {
            log.error("Failed to read file content: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Step 5: Upload to ECMS.
     * Sends file bytes to external content management system.
     */
    private Mono<InlineResponse200> uploadToEcmsAndSave(
            MasterTemplateDefinitionEntity template, DocumentUploadRequest request,
            List<MetadataNode> metadata, byte[] fileBytes) {
        return ecmsClient.uploadDocument(fileBytes, buildEcmsRequest(request, metadata))
            .flatMap(ecmsResponse -> saveStorageIndex(template, request, metadata, ecmsResponse));
    }

    /**
     * Step 6: Create Storage Index Entry.
     * Persists document metadata to storage_index table and returns the document ID.
     */
    private Mono<InlineResponse200> saveStorageIndex(
            MasterTemplateDefinitionEntity template, DocumentUploadRequest request,
            List<MetadataNode> metadata, EcmsDocumentResponse ecmsResponse) {
        StorageIndexEntity entity = createStorageEntity(template, request, ecmsResponse);
        setEntityMetadata(entity, metadata);
        return storageIndexDao.save(entity).map(this::buildUploadResponse);
    }

    private StorageIndexEntity createStorageEntity(
            MasterTemplateDefinitionEntity template, DocumentUploadRequest request,
            EcmsDocumentResponse ecmsResponse) {
        return StorageIndexEntity.builder()
            .storageIndexId(UUID.randomUUID())
            .masterTemplateId(template.getMasterTemplateId())
            .templateVersion(template.getTemplateVersion())
            .templateType(request.getDocumentType())
            .storageVendor(STORAGE_VENDOR_ECMS)
            .storageDocumentKey(ecmsResponse.getId())
            .fileName(request.getFileName())
            .referenceKey(request.getReferenceKey())
            .referenceKeyType(request.getReferenceKeyType())
            .accountKey(request.getAccountKey())
            .customerKey(request.getCustomerKey())
            .docCreationDate(System.currentTimeMillis())
            .accessibleFlag(true)
            .sharedFlag(Boolean.TRUE.equals(template.getSharedDocumentFlag()))
            .startDate(request.getActiveStartDate())
            .endDate(request.getActiveEndDate())
            .createdBy(request.getCreatedBy())
            .createdTimestamp(LocalDateTime.now())
            .archiveIndicator(false)
            .versionNumber(1L)
            .recordStatus("ACTIVE")
            .build();
    }

    private void setEntityMetadata(StorageIndexEntity entity, List<MetadataNode> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        try {
            entity.setDocMetadata(Json.of(objectMapper.writeValueAsString(metadata)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata: {}", e.getMessage());
        }
    }

    private InlineResponse200 buildUploadResponse(StorageIndexEntity saved) {
        InlineResponse200 response = new InlineResponse200();
        response.setId(saved.getStorageIndexId());
        return response;
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
     * Step 1: Template Lookup.
     * Finds the latest active template for the given documentType using an efficient
     * indexed database query. Returns the highest version template that is:
     * <ul>
     *   <li>active_flag = true</li>
     *   <li>Within valid date range (start_date <= now <= end_date)</li>
     * </ul>
     *
     * @param documentType The document type (maps to template_type in database)
     * @return Mono with the template, or error if not found
     */
    private Mono<MasterTemplateDefinitionEntity> findTemplateByDocumentType(String documentType) {
        long currentDate = System.currentTimeMillis();
        return masterTemplateDao.findLatestActiveTemplateByType(documentType, currentDate)
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

    private com.documenthub.dto.upload.DocumentUploadRequest buildEcmsRequest(
            DocumentUploadRequest request, List<MetadataNode> metadata) {
        return com.documenthub.dto.upload.DocumentUploadRequest.builder()
            .templateType(request.getDocumentType())
            .templateVersion(1)
            .fileName(request.getFileName())
            .accountId(request.getAccountKey())
            .customerId(request.getCustomerKey())
            .referenceKey(request.getReferenceKey())
            .referenceKeyType(request.getReferenceKeyType())
            .metadata(convertMetadataToMap(metadata))
            .build();
    }

    private Map<String, Object> convertMetadataToMap(List<MetadataNode> metadata) {
        Map<String, Object> metadataMap = new HashMap<>();
        if (metadata != null) {
            metadata.forEach(node -> metadataMap.put(node.getKey(), node.getValue()));
        }
        return metadataMap;
    }

    private DocumentDetailsNode buildDocumentDetailsNode(
            StorageIndexEntity storageIndex, MasterTemplateDefinitionEntity template,
            String requestorType, boolean includeDownloadUrl) {
        DocumentDetailsNode node = createBaseDocumentNode(storageIndex, template);
        setDocumentMetadata(node, storageIndex);
        node.setLinks(buildDocumentLinks(storageIndex, template, requestorType));
        return node;
    }

    private DocumentDetailsNode createBaseDocumentNode(
            StorageIndexEntity storageIndex, MasterTemplateDefinitionEntity template) {
        DocumentDetailsNode node = new DocumentDetailsNode();
        node.setDocumentId(storageIndex.getStorageIndexId().toString());
        node.setDisplayName(storageIndex.getFileName());
        node.setDocumentType(storageIndex.getTemplateType());
        node.setCategory(template.getTemplateCategory());
        node.setLineOfBusiness(template.getLineOfBusiness());
        node.setMimeType(determineMimeType(storageIndex.getFileName()));
        node.setDatePosted(storageIndex.getDocCreationDate());
        return node;
    }

    private void setDocumentMetadata(DocumentDetailsNode node, StorageIndexEntity storageIndex) {
        if (storageIndex.getDocMetadata() == null) {
            return;
        }
        try {
            List<MetadataNode> metadata = objectMapper.readValue(
                storageIndex.getDocMetadata().asString(),
                new TypeReference<List<MetadataNode>>() {});
            node.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse document metadata: {}", e.getMessage());
        }
    }

    private Links buildDocumentLinks(
            StorageIndexEntity storageIndex, MasterTemplateDefinitionEntity template, String requestorType) {
        Links links = new Links();
        addDownloadLink(links, storageIndex, template, requestorType);
        addDeleteLink(links, storageIndex, template, requestorType);
        return links;
    }

    private void addDownloadLink(
            Links links, StorageIndexEntity storageIndex,
            MasterTemplateDefinitionEntity template, String requestorType) {
        if (!accessControlService.hasAccess(template, requestorType, "Download")) {
            return;
        }
        LinksDownload downloadLink = new LinksDownload();
        downloadLink.setHref("/documents/" + storageIndex.getStorageIndexId());
        downloadLink.setType("GET");
        downloadLink.setRel("download");
        downloadLink.setTitle("Download this document");
        downloadLink.setResponseTypes(Arrays.asList("application/pdf", "application/octet-stream"));
        links.setDownload(downloadLink);
    }

    private void addDeleteLink(
            Links links, StorageIndexEntity storageIndex,
            MasterTemplateDefinitionEntity template, String requestorType) {
        if (!accessControlService.hasAccess(template, requestorType, "Delete")) {
            return;
        }
        LinksDelete deleteLink = new LinksDelete();
        deleteLink.setHref("/documents/" + storageIndex.getStorageIndexId());
        deleteLink.setType("DELETE");
        deleteLink.setRel("delete");
        deleteLink.setTitle("Delete this document");
        links.setDelete(deleteLink);
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
