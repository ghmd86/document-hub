package com.documenthub.controller;

import com.documenthub.dto.upload.DocumentUploadRequest;
import com.documenthub.dto.upload.DocumentUploadResponse;
import com.documenthub.integration.ecms.EcmsClientException;
import com.documenthub.model.ErrorResponse;
import com.documenthub.processor.DocumentUploadProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST Controller for Document Upload API
 * Handles document uploads to ECMS and creates storage index entries
 */
@RestController
@RequestMapping("/documents")
@Tag(name = "Document Upload", description = "Upload documents to ECMS and track in Document Hub")
@Slf4j
@Validated
@RequiredArgsConstructor
public class DocumentUploadController {

    private final DocumentUploadProcessor documentUploadProcessor;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document",
               description = "Uploads a document to ECMS and creates a storage index entry for tracking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document uploaded successfully",
                     content = @Content(schema = @Schema(implementation = DocumentUploadResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<DocumentUploadResponse>> uploadDocument(
        @Parameter(description = "API version", required = true)
        @RequestHeader(value = "X-version", required = true) Integer xVersion,

        @Parameter(description = "Correlation ID for request tracing", required = true)
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,

        @Parameter(description = "ID of the requestor (user ID)", required = true)
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,

        @Parameter(description = "The file to upload", required = true)
        @RequestPart("file") FilePart file,

        @Parameter(description = "Template type for the document", required = true)
        @RequestPart("templateType") String templateType,

        @Parameter(description = "Template version", required = true)
        @RequestPart("templateVersion") String templateVersion,

        @Parameter(description = "Display name for the document")
        @RequestPart(value = "displayName", required = false) String displayName,

        @Parameter(description = "Account ID to associate with the document")
        @RequestPart(value = "accountId", required = false) String accountId,

        @Parameter(description = "Customer ID to associate with the document")
        @RequestPart(value = "customerId", required = false) String customerId,

        @Parameter(description = "Reference key for document lookup")
        @RequestPart(value = "referenceKey", required = false) String referenceKey,

        @Parameter(description = "Reference key type")
        @RequestPart(value = "referenceKeyType", required = false) String referenceKeyType,

        @Parameter(description = "Whether this is a shared document")
        @RequestPart(value = "sharedFlag", required = false) String sharedFlag
    ) {
        log.info("Received document upload request - correlationId: {}, requestorId: {}, fileName: {}",
            xCorrelationId, xRequestorId, file.filename());

        // Build upload request from multipart parts
        DocumentUploadRequest request = DocumentUploadRequest.builder()
            .templateType(templateType)
            .templateVersion(parseInteger(templateVersion, "templateVersion"))
            .fileName(file.filename())
            .displayName(displayName)
            .accountId(parseUUID(accountId))
            .customerId(parseUUID(customerId))
            .referenceKey(referenceKey)
            .referenceKeyType(referenceKeyType)
            .sharedFlag(parseBoolean(sharedFlag))
            .build();

        // Validate required fields
        if (request.getTemplateType() == null || request.getTemplateType().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(
                buildErrorResponse("Template type is required")));
        }

        if (request.getTemplateVersion() == null) {
            return Mono.just(ResponseEntity.badRequest().body(
                buildErrorResponse("Template version is required")));
        }

        return documentUploadProcessor.processUpload(file, request, xRequestorId.toString())
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
            .doOnError(e -> log.error("Error processing document upload - correlationId: {}",
                xCorrelationId, e))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()))))
            .onErrorResume(EcmsClientException.class, e ->
                Mono.just(ResponseEntity.status(e.getStatusCode())
                    .body(buildErrorResponse("ECMS error: " + e.getMessage()))))
            .onErrorResume(e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Internal server error"))));
    }

    private Integer parseInteger(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
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

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private DocumentUploadResponse buildErrorResponse(String message) {
        return DocumentUploadResponse.builder()
            .status("ERROR")
            .message(message)
            .build();
    }
}
