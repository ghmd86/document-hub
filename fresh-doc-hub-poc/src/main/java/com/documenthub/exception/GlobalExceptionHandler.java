package com.documenthub.exception;

import com.documenthub.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across all endpoints.
 * Converts exceptions to standardized ErrorResponse format matching API specification.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_PREFIX_VALIDATION = "VAL";
    private static final String ERROR_PREFIX_SECURITY = "SEC";
    private static final String ERROR_PREFIX_NOT_FOUND = "NTF";
    private static final String ERROR_PREFIX_CONFLICT = "CNF";
    private static final String ERROR_PREFIX_INTERNAL = "INT";
    private static final String ERROR_PREFIX_EXTERNAL = "EXT";

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s-001 - %s: %s",
                        ERROR_PREFIX_VALIDATION,
                        error.getField(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(errors, HttpStatus.BAD_REQUEST.value())));
    }

    /**
     * Handle input parsing errors
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInputException(ServerWebInputException ex) {
        String errorMessage = String.format("%s-002 - Invalid request input: %s",
                ERROR_PREFIX_VALIDATION, ex.getReason());

        log.warn("Input error: {}", errorMessage);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(errorMessage, HttpStatus.BAD_REQUEST.value())));
    }

    /**
     * Handle illegal argument exceptions (business validation failures)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String errorMessage = String.format("%s-003 - %s",
                ERROR_PREFIX_VALIDATION, ex.getMessage());

        log.warn("Validation error: {}", errorMessage);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(errorMessage, HttpStatus.BAD_REQUEST.value())));
    }

    /**
     * Handle security exceptions (permission denied)
     */
    @ExceptionHandler(SecurityException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSecurityException(SecurityException ex) {
        String errorMessage = String.format("%s-001 - Access denied: %s",
                ERROR_PREFIX_SECURITY, ex.getMessage());

        log.warn("Security violation: {}", errorMessage);

        return Mono.just(ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(errorMessage, HttpStatus.FORBIDDEN.value())));
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        String errorMessage = String.format("%s-001 - %s",
                ERROR_PREFIX_NOT_FOUND, ex.getMessage());

        log.warn("Resource not found: {}", errorMessage);

        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(errorMessage, HttpStatus.NOT_FOUND.value())));
    }

    /**
     * Handle conflict exceptions (e.g., duplicate entries, date overlaps)
     */
    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflictException(ConflictException ex) {
        String errorMessage = String.format("%s-001 - %s",
                ERROR_PREFIX_CONFLICT, ex.getMessage());

        log.warn("Conflict detected: {}", errorMessage);

        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(errorMessage, HttpStatus.CONFLICT.value())));
    }

    /**
     * Handle external service exceptions (ECMS, etc.)
     */
    @ExceptionHandler(ExternalServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleExternalServiceException(ExternalServiceException ex) {
        String errorMessage = String.format("%s-001 - External service error: %s",
                ERROR_PREFIX_EXTERNAL, ex.getMessage());

        log.error("External service failure: {}", errorMessage, ex);

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(errorMessage, HttpStatus.SERVICE_UNAVAILABLE.value())));
    }

    /**
     * Handle all other unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        String errorMessage = String.format("%s-001 - An unexpected error occurred",
                ERROR_PREFIX_INTERNAL);

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    /**
     * Build ErrorResponse with single message
     */
    private ErrorResponse buildErrorResponse(String message, int statusCode) {
        return buildErrorResponse(Collections.singletonList(message), statusCode);
    }

    /**
     * Build ErrorResponse with multiple messages
     */
    private ErrorResponse buildErrorResponse(List<String> messages, int statusCode) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorMsg(messages);
        response.setStatusCode(statusCode);
        response.setTimestamp(String.valueOf(Instant.now().getEpochSecond()));
        return response;
    }
}
