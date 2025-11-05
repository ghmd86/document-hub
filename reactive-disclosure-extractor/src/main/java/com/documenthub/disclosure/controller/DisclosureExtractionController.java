package com.documenthub.disclosure.controller;

import com.documenthub.disclosure.model.ExtractionConfig;
import com.documenthub.disclosure.service.DisclosureExtractionService;
import com.documenthub.disclosure.service.DisclosureExtractionService.ExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for disclosure code extraction
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/disclosure")
public class DisclosureExtractionController {

    private final DisclosureExtractionService extractionService;
    private final ObjectMapper objectMapper;
    private final ExtractionConfig defaultConfig;

    public DisclosureExtractionController(
            DisclosureExtractionService extractionService,
            ObjectMapper objectMapper) throws IOException {

        this.extractionService = extractionService;
        this.objectMapper = objectMapper;

        // Load default configuration from classpath
        this.defaultConfig = loadDefaultConfig();
    }

    /**
     * Extract disclosure code for an account
     */
    @PostMapping("/extract")
    public Mono<ResponseEntity<ExtractionResult>> extractDisclosureCode(
            @RequestBody ExtractionRequest request,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId) {

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        log.info("[{}] Extracting disclosure code for account: {}",
                correlationId, request.getAccountId());

        // Build input context
        Map<String, Object> inputContext = Map.of(
                "accountId", request.getAccountId()
        );

        final String finalCorrelationId = correlationId;

        return extractionService.extract(defaultConfig, inputContext, correlationId)
                .map(result -> {
                    if (result.isSuccess()) {
                        log.info("[{}] Disclosure code extracted successfully: {}",
                                finalCorrelationId, result.getData().get("disclosureData"));
                        return ResponseEntity.ok(result);
                    } else {
                        log.error("[{}] Disclosure code extraction failed: {}",
                                finalCorrelationId, result.getError());
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
                    }
                })
                .onErrorResume(error -> {
                    log.error("[{}] Unexpected error during extraction", finalCorrelationId, error);
                    ExtractionResult errorResult = new ExtractionResult();
                    errorResult.setSuccess(false);
                    errorResult.setError(new ExtractionResult.ErrorDetail(
                            "INTERNAL_ERROR",
                            error.getMessage()
                    ));
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResult));
                });
    }

    /**
     * Extract disclosure code with custom configuration
     */
    @PostMapping("/extract/custom")
    public Mono<ResponseEntity<ExtractionResult>> extractWithCustomConfig(
            @RequestBody CustomExtractionRequest request,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId) {

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        log.info("[{}] Extracting with custom config", correlationId);

        final String finalCorrelationId = correlationId;

        return extractionService.extract(request.getConfig(), request.getInput(), correlationId)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("[{}] Extraction failed", finalCorrelationId, error);
                    ExtractionResult errorResult = new ExtractionResult();
                    errorResult.setSuccess(false);
                    errorResult.setError(new ExtractionResult.ErrorDetail(
                            "EXTRACTION_ERROR",
                            error.getMessage()
                    ));
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResult));
                });
    }

    /**
     * Get the current extraction configuration
     */
    @GetMapping("/config")
    public Mono<ResponseEntity<ExtractionConfig>> getConfig() {
        return Mono.just(ResponseEntity.ok(defaultConfig));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "disclosure-extraction"
        )));
    }

    /**
     * Load default configuration from JSON file
     */
    private ExtractionConfig loadDefaultConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("enhanced_disclosure_extraction.json");
        return objectMapper.readValue(resource.getInputStream(), ExtractionConfig.class);
    }

    // Request DTOs
    @Data
    public static class ExtractionRequest {
        private String accountId;
    }

    @Data
    public static class CustomExtractionRequest {
        private ExtractionConfig config;
        private Map<String, Object> input;
    }
}
