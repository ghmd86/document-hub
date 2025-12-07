package com.documenthub.controller;

import com.documenthub.model.DocumentListRequest;
import com.documenthub.model.DocumentRetrievalResponse;
import com.documenthub.model.ErrorResponse;
import com.documenthub.model.XRequestorType;
import com.documenthub.service.DocumentEnquiryService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

/**
 * REST Controller for Document Enquiry API
 */
@RestController
@RequestMapping("/documents-enquiry")
@Tag(name = "Document List Retrieval", description = "Retrieve document lists based on account and sharing rules")
@Slf4j
@Validated
@RequiredArgsConstructor
public class DocumentEnquiryController {

    private final DocumentEnquiryService documentEnquiryService;

    @PostMapping
    @Operation(summary = "Retrieve document list",
               description = "Returns a list of documents based on customer/account and sharing rules")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful Retrieval",
                     content = @Content(schema = @Schema(implementation = DocumentRetrievalResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<DocumentRetrievalResponse>> getDocuments(
        @Parameter(description = "API version", required = true)
        @RequestHeader(value = "X-version", required = true) Integer xVersion,

        @Parameter(description = "Correlation ID for request tracing", required = true)
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,

        @Parameter(description = "ID of the requestor", required = true)
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,

        @Parameter(description = "Type of the requestor", required = true)
        @RequestHeader(value = "X-requestor-type", required = true) XRequestorType xRequestorType,

        @Parameter(description = "Document list request", required = true)
        @Valid @RequestBody DocumentListRequest body
    ) {
        log.info("Received document enquiry request - correlationId: {}, requestorId: {}, requestorType: {}",
            xCorrelationId, xRequestorId, xRequestorType);

        // Validate request
        if (body == null || body.getCustomerId() == null) {
            log.warn("Customer ID is required");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (body.getAccountId() == null || body.getAccountId().isEmpty()) {
            log.warn("At least one account ID is required");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Call service
        return documentEnquiryService.getDocuments(body)
            .map(response -> {
                log.info("Successfully retrieved {} documents",
                    response.getDocumentList() != null ? response.getDocumentList().size() : 0);
                return ResponseEntity.ok(response);
            })
            .doOnError(e -> log.error("Error processing document enquiry", e))
            .onErrorResume(e -> Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            ));
    }
}
