package com.documenthub.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for reactive controllers.
 * Handles various exceptions and returns appropriate error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
        log.error("Validation error: {}", ex.getMessage());

        List<String> errorMessages = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errorMessages.add(String.format("%s: %s", error.getField(), error.getDefaultMessage()))
        );

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(errorMessages)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(List.of(ex.getMessage()))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(List.of(ex.getMessage()))
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Unauthorized access: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(List.of(ex.getMessage()))
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleForbiddenException(ForbiddenException ex) {
        log.error("Forbidden access: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(List.of(ex.getMessage()))
                .statusCode(HttpStatus.FORBIDDEN.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailableException(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(List.of(ex.getMessage()))
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .errorMsg(List.of("An unexpected error occurred. Please try again later."))
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(String.valueOf(Instant.now().getEpochSecond()))
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
