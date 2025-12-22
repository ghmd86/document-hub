package com.documenthub.controller;

import com.documenthub.model.*;
import com.documenthub.processor.DocumentManagementProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Document Management API
 * Handles document upload, download, delete, and metadata operations.
 * Implements the doc-hub.yaml OpenAPI specification.
 */
@RestController
@RequestMapping("/documents")
@Tag(name = "Document Management", description = "Upload, download, delete, and manage documents")
@Slf4j
@Validated
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentManagementProcessor documentManagementProcessor;

    /**
     * Upload a document (POST /documents)
     * Accepts multipart/form-data with document content and metadata
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document",
               description = "Uploads a document for storage. The document is stored in ECMS and indexed in Document Hub.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
                     content = @Content(schema = @Schema(implementation = InlineResponse200.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Cannot be processed. Antivirus scan failed.",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Service Unavailable",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<InlineResponse200>> uploadDocument(
        @Parameter(description = "API version", required = true)
        @RequestHeader(value = "X-version", required = true) Integer xVersion,

        @Parameter(description = "Correlation ID for request tracing", required = true)
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,

        @Parameter(description = "ID of the requestor", required = true)
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,

        @Parameter(description = "Type of the requestor", required = true)
        @RequestHeader(value = "X-requestor-type", required = true) XRequestorType xRequestorType,

        @Parameter(description = "Document type", required = true)
        @RequestPart("documentType") String documentType,

        @Parameter(description = "User or system that created the document", required = true)
        @RequestPart("createdBy") String createdBy,

        @Parameter(description = "The document content", required = true)
        @RequestPart("content") MultipartFile content,

        @Parameter(description = "Document metadata as JSON array")
        @RequestPart(value = "metadata", required = false) String metadataJson,

        @Parameter(description = "Template ID reference")
        @RequestPart(value = "templateId", required = false) String templateId,

        @Parameter(description = "External reference key")
        @RequestPart(value = "referenceKey", required = false) String referenceKey,

        @Parameter(description = "Reference key type")
        @RequestPart(value = "referenceKeyType", required = false) String referenceKeyType,

        @Parameter(description = "Account key")
        @RequestPart(value = "accountKey", required = false) String accountKey,

        @Parameter(description = "Customer key")
        @RequestPart(value = "customerKey", required = false) String customerKey,

        @Parameter(description = "Document category")
        @RequestPart(value = "category", required = false) String category,

        @Parameter(description = "File name")
        @RequestPart(value = "fileName", required = false) String fileName,

        @Parameter(description = "Active start date (epoch)")
        @RequestPart(value = "activeStartDate", required = false) String activeStartDate,

        @Parameter(description = "Active end date (epoch)")
        @RequestPart(value = "activeEndDate", required = false) String activeEndDate,

        @Parameter(description = "Thread ID for tracing")
        @RequestPart(value = "threadId", required = false) String threadId,

        @Parameter(description = "Correlation ID")
        @RequestPart(value = "correlationId", required = false) String correlationId
    ) {
        String originalFilename = content.getOriginalFilename();
        log.info("Received document upload request - correlationId: {}, requestorId: {}, documentType: {}, fileName: {}",
            xCorrelationId, xRequestorId, documentType, originalFilename);

        return documentManagementProcessor.uploadDocument(
                content, documentType, createdBy, metadataJson,
                parseUUID(templateId), referenceKey, referenceKeyType,
                parseUUID(accountKey), parseUUID(customerKey),
                category, fileName != null ? fileName : originalFilename,
                parseLong(activeStartDate), parseLong(activeEndDate),
                parseUUID(threadId), parseUUID(correlationId),
                xRequestorType != null ? xRequestorType.name() : "SYSTEM")
            .map(response -> ResponseEntity.ok(response))
            .doOnError(e -> log.error("Error processing document upload - correlationId: {}", xCorrelationId, e))
            .onErrorResume(SecurityException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.badRequest().build()))
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * Download a document (GET /documents/{documentId})
     * Returns the binary content of the document
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "Download a document",
               description = "Downloads the document identified by documentId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Service Unavailable",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadDocument(
        @Parameter(description = "API version", required = true)
        @RequestHeader(value = "X-version", required = true) Integer xVersion,

        @Parameter(description = "Correlation ID for request tracing", required = true)
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,

        @Parameter(description = "ID of the requestor", required = true)
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,

        @Parameter(description = "Type of the requestor", required = true)
        @RequestHeader(value = "X-requestor-type", required = true) XRequestorType xRequestorType,

        @Parameter(description = "Document ID", required = true)
        @PathVariable String documentId
    ) {
        log.info("Received document download request - correlationId: {}, documentId: {}", xCorrelationId, documentId);

        return documentManagementProcessor.downloadDocument(documentId, xRequestorType.getValue())
            .map(downloadResult -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(downloadResult.getMimeType()));
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.inline()
                        .filename(downloadResult.getFileName())
                        .build());
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(downloadResult.getContent());
            })
            .doOnError(e -> log.error("Error downloading document - correlationId: {}, documentId: {}",
                xCorrelationId, documentId, e))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(SecurityException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * Delete a document (DELETE /documents/{documentId})
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document",
               description = "Deletes the document identified by documentId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successful operation"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Service Unavailable",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<Void>> deleteDocument(
        @Parameter(description = "API version", required = true)
        @RequestHeader(value = "X-version", required = true) Integer xVersion,

        @Parameter(description = "Correlation ID for request tracing", required = true)
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,

        @Parameter(description = "ID of the requestor", required = true)
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,

        @Parameter(description = "Type of the requestor", required = true)
        @RequestHeader(value = "X-requestorType", required = true) XRequestorType xRequestorType,

        @Parameter(description = "Document ID", required = true)
        @PathVariable String documentId
    ) {
        log.info("Received document delete request - correlationId: {}, documentId: {}", xCorrelationId, documentId);

        return documentManagementProcessor.deleteDocument(documentId, xRequestorType.getValue())
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .doOnError(e -> log.error("Error deleting document - correlationId: {}, documentId: {}",
                xCorrelationId, documentId, e))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(SecurityException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * Get document metadata (GET /documents/{documentId}/metadata)
     */
    @GetMapping("/{documentId}/metadata")
    @Operation(summary = "Get document metadata",
               description = "Retrieves the metadata for the document identified by documentId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
                     content = @Content(schema = @Schema(implementation = DocumentDetailsNode.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Service Unavailable",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<DocumentDetailsNode>> getDocumentMetadata(
        @Parameter(description = "API version", required = true)
        @RequestHeader(value = "X-version", required = true) Integer xVersion,

        @Parameter(description = "Correlation ID for request tracing", required = true)
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,

        @Parameter(description = "ID of the requestor", required = true)
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,

        @Parameter(description = "Type of the requestor", required = true)
        @RequestHeader(value = "X-requestor-type", required = true) XRequestorType xRequestorType,

        @Parameter(description = "Document ID", required = true)
        @PathVariable String documentId,

        @Parameter(description = "Include download URL in response")
        @RequestParam(value = "includeDownloadUrl", required = false, defaultValue = "false") Boolean includeDownloadUrl
    ) {
        log.info("Received document metadata request - correlationId: {}, documentId: {}, includeDownloadUrl: {}",
            xCorrelationId, documentId, includeDownloadUrl);

        return documentManagementProcessor.getDocumentMetadata(documentId, xRequestorType.getValue(), includeDownloadUrl)
            .map(metadata -> ResponseEntity.ok(metadata))
            .doOnError(e -> log.error("Error getting document metadata - correlationId: {}, documentId: {}",
                xCorrelationId, documentId, e))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(SecurityException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    private UUID parseUUID(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
