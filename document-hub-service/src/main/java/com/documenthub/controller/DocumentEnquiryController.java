package com.documenthub.controller;

import com.documenthub.model.request.DocumentListRequest;
import com.documenthub.model.response.DocumentRetrievalResponse;
import com.documenthub.service.DocumentEnquiryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for document enquiry operations.
 * Provides endpoints to retrieve documents based on various criteria.
 */
@Slf4j
@RestController
@RequestMapping("/documents-enquiry")
@RequiredArgsConstructor
public class DocumentEnquiryController {

    private final DocumentEnquiryService documentEnquiryService;

    /**
     * POST /documents-enquiry
     * Retrieve documents based on request criteria.
     *
     * @param version API version
     * @param correlationId Correlation ID for request tracing
     * @param requestorId ID of the requestor
     * @param requestorType Type of requestor (CUSTOMER, AGENT, SYSTEM)
     * @param request Document list request body
     * @return DocumentRetrievalResponse with documents and pagination
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<DocumentRetrievalResponse> getDocuments(
            @RequestHeader(name = "X-version") @Max(100) Integer version,
            @RequestHeader(name = "X-correlation-id") String correlationId,
            @RequestHeader(name = "X-requestor-id") UUID requestorId,
            @RequestHeader(name = "X-requestor-type") String requestorType,
            @Valid @RequestBody DocumentListRequest request
    ) {
        log.info("Received document enquiry request. CorrelationId: {}, RequestorId: {}, RequestorType: {}",
                correlationId, requestorId, requestorType);

        log.debug("Request details - CustomerId: {}, AccountIds: {}, PageNumber: {}, PageSize: {}",
                request.getCustomerId(), request.getAccountId(), request.getPageNumber(), request.getPageSize());

        return documentEnquiryService.getDocuments(request)
                .doOnSuccess(response -> log.info("Successfully retrieved {} documents for correlationId: {}",
                        response.getDocumentList().size(), correlationId))
                .doOnError(error -> log.error("Error processing document enquiry for correlationId: {}: {}",
                        correlationId, error.getMessage(), error));
    }
}
