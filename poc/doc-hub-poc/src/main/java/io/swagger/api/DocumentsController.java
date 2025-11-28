package io.swagger.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.model.*;
import io.swagger.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Document Management
 * Manually created controller following company standards
 * Uses generated model classes from OpenAPI spec
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@Validated
@RequiredArgsConstructor
public class DocumentsController {

    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    /**
     * Upload a document
     * POST /documents
     *
     * @param xVersion         API version
     * @param xCorrelationId   Correlation ID for request tracing
     * @param xRequestorId     ID of the requestor
     * @param xRequestorType   Type of the requestor (CUSTOMER, AGENT, SYSTEM)
     * @param documentType     Type of document being uploaded
     * @param createdBy        UUID of user/system creating the document
     * @param content          The file content as reactive FilePart
     * @param metadataJson     Metadata as JSON string
     * @param templateId       Optional template ID
     * @param referenceKey     Optional reference key
     * @param referenceKeyType Optional reference key type
     * @param accountKey       Optional account key
     * @param customerKey      Optional customer key
     * @param category         Optional category
     * @param fileName         Optional file name
     * @param activeStartDate  Optional active start date (epoch seconds)
     * @param activeEndDate    Optional active end date (epoch seconds)
     * @param threadId         Optional thread ID
     * @param correlationId    Optional correlation ID
     * @return ResponseEntity with document ID
     */
    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<InlineResponse200>> uploadDocument(
        // Required Headers
        @NotNull
        @RequestHeader(value = "X-version", required = true)
        Integer xVersion,

        @NotNull
        @Size(max = 36)
        @RequestHeader(value = "X-correlation-id", required = true)
        String xCorrelationId,

        @NotNull
        @RequestHeader(value = "X-requestor-id", required = true)
        UUID xRequestorId,

        @NotNull
        @RequestHeader(value = "X-requestor-type", required = true)
        XRequestorType xRequestorType,

        // Required Form Parts
        @NotNull
        @RequestPart(value = "documentType", required = true)
        String documentType,

        @NotNull
        @RequestPart(value = "createdBy", required = true)
        UUID createdBy,

        @NotNull
        @RequestPart(value = "content", required = true)
        Mono<FilePart> content,

        @NotNull
        @RequestPart(value = "metadata", required = true)
        String metadataJson,

        // Optional Form Parts
        @RequestPart(value = "templateId", required = false)
        UUID templateId,

        @RequestPart(value = "referenceKey", required = false)
        String referenceKey,

        @RequestPart(value = "referenceKeyType", required = false)
        String referenceKeyType,

        @RequestPart(value = "accountKey", required = false)
        UUID accountKey,

        @RequestPart(value = "customerKey", required = false)
        UUID customerKey,

        @RequestPart(value = "category", required = false)
        String category,

        @RequestPart(value = "fileName", required = false)
        String fileName,

        @RequestPart(value = "activeStartDate", required = false)
        Long activeStartDate,

        @RequestPart(value = "activeEndDate", required = false)
        Long activeEndDate,

        @RequestPart(value = "threadId", required = false)
        UUID threadId,

        @RequestPart(value = "correlationId", required = false)
        UUID correlationId
    ) {
        log.info("Document upload request received. CorrelationId: {}, DocumentType: {}",
                 xCorrelationId, documentType);

        try {
            // Parse metadata JSON to List<MetadataNode>
            List<MetadataNode> metadata = parseMetadata(metadataJson);

            // Build DocumentUploadRequest object
            DocumentUploadRequest request = new DocumentUploadRequest();
            request.setDocumentType(documentType);
            request.setCreatedBy(createdBy);
            request.setTemplateId(templateId);
            request.setReferenceKey(referenceKey);
            request.setReferenceKeyType(referenceKeyType);
            request.setAccountKey(accountKey);
            request.setCustomerKey(customerKey);
            request.setCategory(category);
            request.setFileName(fileName);
            request.setActiveStartDate(activeStartDate);
            request.setActiveEndDate(activeEndDate);
            request.setThreadId(threadId);
            request.setCorrelationId(correlationId);

            // Set metadata (Metadata extends ArrayList<MetadataNode>)
            Metadata metadataList = new Metadata();
            metadataList.addAll(metadata);
            request.setMetadata(metadataList);

            // Delegate to service layer
            return documentService.uploadDocument(
                xCorrelationId,
                xRequestorId,
                xRequestorType,
                request,
                content
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to parse metadata JSON. CorrelationId: {}", xCorrelationId, e);
            return Mono.error(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid metadata JSON format: " + e.getMessage()
            ));
        }
    }

    /**
     * Download a document by ID
     * GET /documents/{documentId}
     *
     * @param xVersion       API version
     * @param xCorrelationId Correlation ID for request tracing
     * @param xRequestorId   ID of the requestor
     * @param xRequestorType Type of the requestor
     * @param documentId     Document ID to download
     * @return ResponseEntity with file bytes
     */
    @GetMapping(
        value = "/{documentId}",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public Mono<ResponseEntity<byte[]>> downloadDocument(
        @NotNull @RequestHeader(value = "X-version") Integer xVersion,
        @NotNull @RequestHeader(value = "X-correlation-id") String xCorrelationId,
        @NotNull @RequestHeader(value = "X-requestor-id") UUID xRequestorId,
        @NotNull @RequestHeader(value = "X-requestor-type") XRequestorType xRequestorType,
        @PathVariable @Size(max = 500) String documentId
    ) {
        log.info("Document download request. CorrelationId: {}, DocumentId: {}",
                 xCorrelationId, documentId);

        return documentService.downloadDocument(documentId, xCorrelationId);
    }

    /**
     * Delete a document by ID
     * DELETE /documents/{documentId}
     *
     * @param xVersion       API version
     * @param xCorrelationId Correlation ID for request tracing
     * @param xRequestorId   ID of the requestor
     * @param xRequestorType Type of the requestor
     * @param documentId     Document ID to delete
     * @return ResponseEntity with 204 No Content on success
     */
    @DeleteMapping("/{documentId}")
    public Mono<ResponseEntity<Void>> deleteDocument(
        @NotNull @RequestHeader(value = "X-version") Integer xVersion,
        @NotNull @RequestHeader(value = "X-correlation-id") String xCorrelationId,
        @NotNull @RequestHeader(value = "X-requestor-id") UUID xRequestorId,
        @NotNull @RequestHeader(value = "X-requestorType") XRequestorType xRequestorType,
        @PathVariable @Size(max = 500) String documentId
    ) {
        log.info("Document delete request. CorrelationId: {}, DocumentId: {}",
                 xCorrelationId, documentId);

        return documentService.deleteDocument(documentId, xCorrelationId);
    }

    /**
     * Get document metadata by ID
     * GET /documents/{documentId}/metadata
     *
     * @param xVersion           API version
     * @param xCorrelationId     Correlation ID for request tracing
     * @param xRequestorId       ID of the requestor
     * @param xRequestorType     Type of the requestor
     * @param documentId         Document ID
     * @param includeDownloadUrl Whether to include download URL
     * @return ResponseEntity with DocumentDetailsNode
     */
    @GetMapping(
        value = "/{documentId}/metadata",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<DocumentDetailsNode>> getDocumentMetadata(
        @NotNull @RequestHeader(value = "X-version") Integer xVersion,
        @NotNull @RequestHeader(value = "X-correlation-id") String xCorrelationId,
        @NotNull @RequestHeader(value = "X-requestor-id") UUID xRequestorId,
        @NotNull @RequestHeader(value = "X-requestor-type") XRequestorType xRequestorType,
        @PathVariable @Size(max = 500) String documentId,
        @RequestParam(value = "includeDownloadUrl", required = false, defaultValue = "false") Boolean includeDownloadUrl
    ) {
        log.info("Document metadata request. CorrelationId: {}, DocumentId: {}, IncludeDownloadUrl: {}",
                 xCorrelationId, documentId, includeDownloadUrl);

        return documentService.getDocumentMetadata(documentId, includeDownloadUrl, xCorrelationId);
    }

    /**
     * Parse metadata JSON string to List<MetadataNode>
     *
     * @param metadataJson JSON string containing metadata array
     * @return List of MetadataNode objects
     * @throws JsonProcessingException if JSON parsing fails
     */
    private List<MetadataNode> parseMetadata(String metadataJson) throws JsonProcessingException {
        return objectMapper.readValue(
            metadataJson,
            new TypeReference<List<MetadataNode>>() {}
        );
    }
}
