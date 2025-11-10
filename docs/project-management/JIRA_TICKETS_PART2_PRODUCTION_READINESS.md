# Document Hub API - JIRA Tickets Part 2: Production Readiness

**Continuation of:** JIRA_TICKETS_FOR_COMPANY.md
**Created:** 2025-11-09

---

## STORY-010: Implement Rule Evaluation Engine

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 8-10 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 9

### User Story
```
As a system
I want to evaluate conditional logic on extracted data
So that I can determine document eligibility based on business rules
```

### Description
Implement the rule evaluation engine that:
1. Applies transformations to extracted data
2. Evaluates conditions using various operators
3. Combines conditions with AND/OR logic
4. Returns eligibility result

### Technical Details

**RuleEvaluationService.java**
```java
package com.company.documenthub.extraction;

import com.company.documenthub.extraction.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    private final TransformationService transformationService;

    public Mono<Boolean> evaluateRule(
            ExtractionConfig config,
            Map<String, Map<String, Object>> extractedData) {

        return Mono.fromCallable(() -> {
            // Step 1: Apply transformations
            Map<String, Object> transformedData =
                transformationService.applyTransformations(
                    extractedData,
                    config.getTransformations()
                );

            // Step 2: Evaluate conditions
            boolean result = evaluateConditions(
                transformedData,
                config.getConditions()
            );

            log.info("Rule evaluation result: {} for rule: {}",
                result, config.getRuleName());

            return result;
        });
    }

    private boolean evaluateConditions(
            Map<String, Object> data,
            List<Condition> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means always eligible
        }

        Boolean result = null;

        for (Condition condition : conditions) {
            boolean conditionResult = evaluateCondition(data, condition);

            if (result == null) {
                result = conditionResult;
            } else {
                String logicalOp = condition.getLogicalOperator();
                if ("AND".equalsIgnoreCase(logicalOp)) {
                    result = result && conditionResult;
                } else if ("OR".equalsIgnoreCase(logicalOp)) {
                    result = result || conditionResult;
                }
            }

            // Short-circuit evaluation for AND
            if ("AND".equalsIgnoreCase(condition.getLogicalOperator()) && !result) {
                return false;
            }
        }

        return result != null ? result : true;
    }

    private boolean evaluateCondition(Map<String, Object> data, Condition condition) {
        String field = condition.getField();
        Object actualValue = getNestedValue(data, field);
        Object expectedValue = condition.getValue();
        String operator = condition.getOperator();

        if (actualValue == null) {
            log.warn("Field {} not found in extracted data", field);
            return false;
        }

        return switch (operator) {
            case "EQUALS" -> equals(actualValue, expectedValue);
            case "NOT_EQUALS" -> !equals(actualValue, expectedValue);
            case "GREATER_THAN" -> greaterThan(actualValue, expectedValue);
            case "LESS_THAN" -> lessThan(actualValue, expectedValue);
            case "GREATER_THAN_OR_EQUALS" -> greaterThanOrEquals(actualValue, expectedValue);
            case "LESS_THAN_OR_EQUALS" -> lessThanOrEquals(actualValue, expectedValue);
            case "CONTAINS" -> contains(actualValue, expectedValue);
            case "NOT_CONTAINS" -> !contains(actualValue, expectedValue);
            case "IN" -> in(actualValue, expectedValue);
            case "NOT_IN" -> !in(actualValue, expectedValue);
            default -> {
                log.error("Unsupported operator: {}", operator);
                yield false;
            }
        };
    }

    private Object getNestedValue(Map<String, Object> data, String field) {
        // Support dot notation for nested fields (e.g., "account.balance")
        String[] parts = field.split("\\.");

        Object current = data;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    private boolean equals(Object actual, Object expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        return actual.toString().equals(expected.toString());
    }

    private boolean greaterThan(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum > expectedNum;
        } catch (Exception e) {
            log.error("Cannot compare {} > {}", actual, expected, e);
            return false;
        }
    }

    private boolean lessThan(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum < expectedNum;
        } catch (Exception e) {
            log.error("Cannot compare {} < {}", actual, expected, e);
            return false;
        }
    }

    private boolean greaterThanOrEquals(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum >= expectedNum;
        } catch (Exception e) {
            log.error("Cannot compare {} >= {}", actual, expected, e);
            return false;
        }
    }

    private boolean lessThanOrEquals(Object actual, Object expected) {
        try {
            double actualNum = toDouble(actual);
            double expectedNum = toDouble(expected);
            return actualNum <= expectedNum;
        } catch (Exception e) {
            log.error("Cannot compare {} <= {}", actual, expected, e);
            return false;
        }
    }

    private boolean contains(Object actual, Object expected) {
        if (actual == null || expected == null) return false;
        return actual.toString().contains(expected.toString());
    }

    private boolean in(Object actual, Object expected) {
        if (actual == null || expected == null) return false;
        if (expected instanceof List) {
            return ((List<?>) expected).contains(actual);
        }
        return false;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
```

**TransformationService.java**
```java
package com.company.documenthub.extraction;

import com.company.documenthub.extraction.model.Transformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TransformationService {

    public Map<String, Object> applyTransformations(
            Map<String, Map<String, Object>> extractedData,
            List<Transformation> transformations) {

        // Flatten extracted data
        Map<String, Object> flatData = new HashMap<>();
        extractedData.values().forEach(flatData::putAll);

        if (transformations == null || transformations.isEmpty()) {
            return flatData;
        }

        Map<String, Object> transformedData = new HashMap<>(flatData);

        for (Transformation transformation : transformations) {
            String field = transformation.getField();
            Object value = transformedData.get(field);

            if (value == null) {
                log.warn("Field {} not found for transformation", field);
                continue;
            }

            for (Map<String, String> operation : transformation.getOperations()) {
                String type = operation.get("type");
                value = applyOperation(value, type, operation);
            }

            transformedData.put(field, value);
        }

        return transformedData;
    }

    private Object applyOperation(Object value, String type, Map<String, String> operation) {
        return switch (type) {
            case "TO_UPPER" -> value.toString().toUpperCase();
            case "TO_LOWER" -> value.toString().toLowerCase();
            case "TO_DOUBLE" -> toDouble(value);
            case "TO_INT" -> toInt(value);
            case "ABS" -> Math.abs(toDouble(value));
            case "ROUND" -> Math.round(toDouble(value));
            case "TRIM" -> value.toString().trim();
            case "SUBSTRING" -> substring(value.toString(), operation);
            case "REPLACE" -> replace(value.toString(), operation);
            default -> {
                log.warn("Unknown transformation type: {}", type);
                yield value;
            }
        };
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private String substring(String value, Map<String, String> operation) {
        int start = Integer.parseInt(operation.getOrDefault("start", "0"));
        int end = Integer.parseInt(operation.getOrDefault("end", String.valueOf(value.length())));
        return value.substring(start, Math.min(end, value.length()));
    }

    private String replace(String value, Map<String, String> operation) {
        String find = operation.getOrDefault("find", "");
        String replaceWith = operation.getOrDefault("replaceWith", "");
        return value.replace(find, replaceWith);
    }
}
```

### Acceptance Criteria
- [ ] All operators implemented (EQUALS, GREATER_THAN, LESS_THAN, etc.)
- [ ] AND/OR logical operators working
- [ ] Short-circuit evaluation for AND conditions
- [ ] Nested field access working (dot notation)
- [ ] Transformations applied before evaluation
- [ ] Type conversion working (String to Number)
- [ ] Null handling (returns false, doesn't crash)
- [ ] Comprehensive logging
- [ ] Unit tests for all operators (95%+ coverage)
- [ ] Performance: < 10ms for 20 conditions

### Testing
```java
@Test
void evaluateCondition_GreaterThan_ReturnsTrue() {
    Map<String, Object> data = Map.of("balance", 15000.0);

    Condition condition = new Condition();
    condition.setField("balance");
    condition.setOperator("GREATER_THAN");
    condition.setValue(10000.0);

    boolean result = ruleEvaluationService.evaluateCondition(data, condition);

    assertTrue(result);
}

@Test
void evaluateConditions_MultipleWithAND_ReturnsCorrectResult() {
    Map<String, Object> data = Map.of(
        "balance", 15000.0,
        "status", "ACTIVE",
        "tier", "GOLD"
    );

    List<Condition> conditions = List.of(
        createCondition("balance", "GREATER_THAN", 10000, "AND"),
        createCondition("status", "EQUALS", "ACTIVE", "AND"),
        createCondition("tier", "EQUALS", "GOLD", "AND")
    );

    boolean result = ruleEvaluationService.evaluateConditions(data, conditions);

    assertTrue(result);
}

@Test
void evaluateConditions_OneFailsWithAND_ReturnsFalse() {
    Map<String, Object> data = Map.of(
        "balance", 5000.0, // Less than threshold
        "status", "ACTIVE"
    );

    List<Condition> conditions = List.of(
        createCondition("balance", "GREATER_THAN", 10000, "AND"),
        createCondition("status", "EQUALS", "ACTIVE", "AND")
    );

    boolean result = ruleEvaluationService.evaluateConditions(data, conditions);

    assertFalse(result);
}
```

### Dependencies
- STORY-009 (Data Orchestration)
- Transformation service

---

## STORY-011: Integrate Generic Extraction Engine

**Type:** Story
**Priority:** High
**Story Points:** 5 SP
**Estimated Hours:** 4-5 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 10

### User Story
```
As a system
I want to integrate all extraction components into a single orchestrator
So that document eligibility can be determined using custom rules
```

### Description
Create the main GenericExtractionEngine that ties together:
1. Schema Parser
2. Data Orchestration
3. Rule Evaluation

And exposes a simple API for evaluating document eligibility.

### Technical Details

**GenericExtractionEngine.java**
```java
package com.company.documenthub.extraction;

import com.company.documenthub.extraction.model.ExtractionConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericExtractionEngine {

    private final ExtractionSchemaParser schemaParser;
    private final DataOrchestrationService orchestrationService;
    private final RuleEvaluationService ruleEvaluationService;

    /**
     * Main entry point for evaluating document eligibility
     *
     * @param schema JSON schema defining extraction and evaluation rules
     * @param customerId Customer UUID
     * @param accountId Account UUID
     * @return Mono<Boolean> true if eligible, false otherwise
     */
    public Mono<Boolean> evaluateEligibility(
            JsonNode schema,
            UUID customerId,
            UUID accountId) {

        return Mono.defer(() -> {
            try {
                // Step 1: Parse schema
                ExtractionConfig config = schemaParser.parse(schema);

                log.info("Evaluating eligibility rule: {} for customer: {}, account: {}",
                    config.getRuleName(), customerId, accountId);

                // Step 2: Execute data sources
                return orchestrationService.executeDataSources(config, customerId, accountId)
                    .flatMap(extractedData -> {
                        log.debug("Extracted data: {}", extractedData);

                        // Step 3: Evaluate rules
                        return ruleEvaluationService.evaluateRule(config, extractedData);
                    })
                    .doOnSuccess(result ->
                        log.info("Eligibility result: {} for rule: {}", result, config.getRuleName()))
                    .onErrorResume(error -> {
                        log.error("Error evaluating eligibility for rule: {}",
                            config.getRuleName(), error);

                        // Return error result from config
                        return Mono.just(config.getEligibilityResult().getOnError());
                    });

            } catch (Exception e) {
                log.error("Error parsing extraction schema", e);
                return Mono.just(false); // Default to not eligible on parse error
            }
        });
    }

    /**
     * Evaluate eligibility with caching
     */
    public Mono<Boolean> evaluateEligibilityWithCache(
            JsonNode schema,
            UUID customerId,
            UUID accountId,
            String cacheKey) {

        // Check cache first
        return cacheService.get(cacheKey)
            .switchIfEmpty(
                evaluateEligibility(schema, customerId, accountId)
                    .flatMap(result ->
                        cacheService.set(cacheKey, result, Duration.ofMinutes(15))
                            .thenReturn(result)
                    )
            );
    }
}
```

### Acceptance Criteria
- [ ] Integration of all extraction components
- [ ] Simple API for eligibility evaluation
- [ ] Proper error handling and logging
- [ ] Caching support for eligibility results
- [ ] Performance: < 500ms for complete extraction and evaluation
- [ ] Unit tests with mocked components
- [ ] Integration tests with real extraction scenarios

### Testing
```java
@Test
void evaluateEligibility_ValidSchema_ReturnsTrue() {
    String schemaJson = """
        {
          "rule_name": "high_balance_customer",
          "description": "Customer with balance > 10000",
          "data_sources": [{
            "source_id": "account",
            "source_type": "REST_API",
            "endpoint_config": {
              "url": "http://localhost:8089/accounts/{accountId}",
              "method": "GET"
            },
            "response_mapping": {
              "balance": "$.data.balance"
            }
          }],
          "conditions": [{
            "field": "balance",
            "operator": "GREATER_THAN",
            "value": 10000
          }],
          "eligibility_result": {
            "on_success": true,
            "on_failure": false,
            "on_error": false
          }
        }
        """;

    // Mock API response
    wireMockServer.stubFor(WireMock.get(urlMatching("/accounts/.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"data\": {\"balance\": 15000}}")));

    JsonNode schema = objectMapper.readTree(schemaJson);
    UUID customerId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    Mono<Boolean> result = extractionEngine.evaluateEligibility(schema, customerId, accountId);

    StepVerifier.create(result)
        .expectNext(true)
        .verifyComplete();
}
```

### Dependencies
- STORY-008, STORY-009, STORY-010

---

## EPIC-DOC-005: Production Readiness

---

## STORY-012: Implement Error Handling and Validation

**Type:** Story
**Priority:** High
**Story Points:** 5 SP
**Estimated Hours:** 4-5 hours
**Assignee:** Mid-level Backend Developer
**Sprint:** Sprint 11

### User Story
```
As an API consumer
I want clear error messages and proper HTTP status codes
So that I can understand and fix issues with my requests
```

### Description
Implement comprehensive error handling:
1. Request validation
2. Error response format (per OpenAPI spec)
3. Global exception handler
4. Proper HTTP status codes

### Technical Details

**ErrorResponse.java**
```java
package com.company.documenthub.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<FieldError> fieldErrors;

    @Data
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
```

**GlobalExceptionHandler.java**
```java
package com.company.documenthub.exception;

import com.company.documenthub.model.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {

        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("Validation Failed");
        error.setMessage("Request validation failed");
        error.setPath(exchange.getRequest().getPath().value());
        error.setTimestamp(LocalDateTime.now());

        List<ErrorResponse.FieldError> fieldErrors = ex.getFieldErrors().stream()
            .map(fe -> {
                ErrorResponse.FieldError fieldError = new ErrorResponse.FieldError();
                fieldError.setField(fe.getField());
                fieldError.setMessage(fe.getDefaultMessage());
                fieldError.setRejectedValue(fe.getRejectedValue());
                return fieldError;
            })
            .collect(Collectors.toList());

        error.setFieldErrors(fieldErrors);

        log.warn("Validation error: {}", error);

        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDocumentNotFound(
            DocumentNotFoundException ex,
            ServerWebExchange exchange) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Document Not Found",
            ex.getMessage(),
            exchange
        );

        log.warn("Document not found: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnauthorized(
            UnauthorizedException ex,
            ServerWebExchange exchange) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            ex.getMessage(),
            exchange
        );

        log.warn("Unauthorized access attempt: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleForbidden(
            ForbiddenException ex,
            ServerWebExchange exchange) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            ex.getMessage(),
            exchange
        );

        log.warn("Forbidden access attempt: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailable(
            ServiceUnavailableException ex,
            ServerWebExchange exchange) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            ex.getMessage(),
            exchange
        );

        log.error("Service unavailable: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex,
            ServerWebExchange exchange) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            exchange
        );

        log.error("Unexpected error", ex);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

    private ErrorResponse createErrorResponse(
            HttpStatus status,
            String error,
            String message,
            ServerWebExchange exchange) {

        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setError(error);
        response.setMessage(message);
        response.setPath(exchange.getRequest().getPath().value());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
```

### Acceptance Criteria
- [ ] All exceptions mapped to appropriate HTTP status codes
- [ ] Error responses follow OpenAPI spec format
- [ ] Field validation errors include field name and rejected value
- [ ] All errors logged appropriately (WARN for client errors, ERROR for server errors)
- [ ] Sensitive information not exposed in error messages
- [ ] Unit tests for all exception handlers

### Testing
```java
@Test
void handleValidationException_ReturnsFieldErrors() {
    DocumentEnquiryRequest request = new DocumentEnquiryRequest();
    // Missing required fields

    webTestClient.post()
        .uri("/api/v1/documents-enquiry")
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.error").isEqualTo("Validation Failed")
        .jsonPath("$.fieldErrors").isArray()
        .jsonPath("$.fieldErrors[0].field").exists()
        .jsonPath("$.fieldErrors[0].message").exists();
}
```

---

## STORY-013: Implement Monitoring and Observability

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 6-8 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 11-12

### User Story
```
As a DevOps engineer
I want comprehensive monitoring and metrics
So that I can track application health and performance
```

### Description
Implement observability features:
1. Micrometer metrics
2. Request/response logging
3. Performance tracking
4. Health checks
5. Prometheus endpoint

### Technical Details

**application.yml**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

**Custom Metrics**
```java
package com.company.documenthub.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class DocumentMetrics {

    private final Counter documentRetrievalCounter;
    private final Counter documentRetrievalErrorCounter;
    private final Timer documentRetrievalTimer;
    private final Counter eligibilityEvaluationCounter;
    private final Timer eligibilityEvaluationTimer;

    public DocumentMetrics(MeterRegistry registry) {
        this.documentRetrievalCounter = Counter.builder("document.retrieval.total")
            .description("Total document retrievals")
            .tag("type", "enquiry")
            .register(registry);

        this.documentRetrievalErrorCounter = Counter.builder("document.retrieval.errors")
            .description("Total document retrieval errors")
            .register(registry);

        this.documentRetrievalTimer = Timer.builder("document.retrieval.duration")
            .description("Document retrieval duration")
            .register(registry);

        this.eligibilityEvaluationCounter = Counter.builder("eligibility.evaluation.total")
            .description("Total eligibility evaluations")
            .register(registry);

        this.eligibilityEvaluationTimer = Timer.builder("eligibility.evaluation.duration")
            .description("Eligibility evaluation duration")
            .register(registry);
    }

    public void recordDocumentRetrieval() {
        documentRetrievalCounter.increment();
    }

    public void recordDocumentRetrievalError() {
        documentRetrievalErrorCounter.increment();
    }

    public Timer.Sample startDocumentRetrievalTimer() {
        return Timer.start();
    }

    public void stopDocumentRetrievalTimer(Timer.Sample sample) {
        sample.stop(documentRetrievalTimer);
    }

    // Similar methods for eligibility evaluation
}
```

**Request Logging Filter**
```java
package com.company.documenthub.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant start = Instant.now();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                Duration duration = Duration.between(start, Instant.now());
                int status = exchange.getResponse().getStatusCode().value();

                log.info("method={} path={} status={} duration={}ms",
                    method, path, status, duration.toMillis());
            })
            .doOnError(error -> {
                Duration duration = Duration.between(start, Instant.now());

                log.error("method={} path={} status=500 duration={}ms error={}",
                    method, path, duration.toMillis(), error.getMessage());
            });
    }
}
```

### Acceptance Criteria
- [ ] Micrometer integrated with Prometheus
- [ ] Custom metrics for document retrieval
- [ ] Custom metrics for eligibility evaluation
- [ ] Request/response logging
- [ ] Performance metrics (duration, count)
- [ ] Health checks for database and external APIs
- [ ] Prometheus endpoint exposed at /actuator/prometheus
- [ ] Grafana dashboard created (optional)
- [ ] Documentation for metrics

---

## STORY-014: Implement API Specification Compliance

**Type:** Story
**Priority:** Critical
**Story Points:** 5 SP
**Estimated Hours:** 4-5 hours
**Assignee:** Mid-level Backend Developer
**Sprint:** Sprint 5

### User Story
```
As an API consumer
I want the API to strictly follow the OpenAPI specification
So that I can integrate with confidence
```

### Description
Ensure full compliance with OpenAPI 3.0 spec:
1. Remove non-spec fields (is_shared, sharing_scope) or add to spec
2. Add missing fields (sizeInMb, languageCode, mimeType, metadata)
3. Ensure camelCase naming
4. Add HATEOAS links
5. Implement pagination
6. Generate OpenAPI documentation

### Pending Items from Prototype
Based on api_spec_validation_and_pending_items.md:

1. ✅ Field naming (camelCase) - COMPLETED in STORY-006
2. ✅ Pagination support - COMPLETED in STORY-006
3. ✅ accountId as array - COMPLETED in STORY-006
4. ⚠️ Add missing fields - IMPLEMENT HERE
5. ⚠️ HATEOAS links - IMPLEMENT HERE
6. ⚠️ Date/category filtering - IMPLEMENT HERE

### Acceptance Criteria
- [ ] All response fields in camelCase
- [ ] pagination object included in response
- [ ] _links object included (self, next, prev)
- [ ] sizeInMb, languageCode, mimeType, metadata fields included
- [ ] OpenAPI spec generated from code (Springdoc)
- [ ] Swagger UI available at /swagger-ui.html
- [ ] API documentation matches implementation 100%

---

## STORY-015: Implement Caching Strategy

**Type:** Story
**Priority:** Medium
**Story Points:** 5 SP
**Estimated Hours:** 4-5 hours
**Assignee:** Mid-level Backend Developer
**Sprint:** Sprint 12

### User Story
```
As a system architect
I want to cache shared documents and eligibility results
So that we can reduce database load and improve performance
```

### Description
Implement Redis caching for:
1. Shared document templates (rarely change)
2. Eligibility evaluation results (15-minute TTL)
3. Customer/account lookups

### Technical Details

**Redis Configuration**
```java
@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Object> serializer =
            new Jackson2JsonRedisSerializer<>(Object.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Object> context =
            builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
```

**Cache Service**
```java
@Service
@RequiredArgsConstructor
public class CacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public Mono<Object> get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> set(String key, Object value, Duration ttl) {
        return redisTemplate.opsForValue().set(key, value, ttl);
    }

    public Mono<Boolean> delete(String key) {
        return redisTemplate.delete(key).map(count -> count > 0);
    }
}
```

### Acceptance Criteria
- [ ] Redis integrated with Spring Data Redis Reactive
- [ ] Shared documents cached with 1-hour TTL
- [ ] Eligibility results cached with 15-minute TTL
- [ ] Cache invalidation on template updates
- [ ] Cache hit/miss metrics
- [ ] Performance improvement: 50%+ reduction in database queries

---

## STORY-016: Security Audit and Penetration Testing

**Type:** Task
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 8-10 hours
**Assignee:** Security Engineer + Senior Developer
**Sprint:** Sprint 12

### User Story
```
As a security officer
I want the application audited for security vulnerabilities
So that customer data is protected
```

### Description
Conduct security audit:
1. OWASP Top 10 vulnerability scan
2. SQL injection testing
3. Authentication/authorization testing
4. Data exposure testing
5. Penetration testing

### Acceptance Criteria
- [ ] OWASP ZAP scan completed with no high/critical issues
- [ ] SQL injection testing passed
- [ ] JWT token validation tested
- [ ] Authorization bypass testing passed
- [ ] Sensitive data not logged
- [ ] Security headers configured (CSP, HSTS, etc.)
- [ ] Penetration test report completed
- [ ] All findings remediated

---

## Summary: Quick Reference

### Total Effort by Epic

| Epic | Stories | Story Points | Estimated Hours |
|------|---------|--------------|-----------------|
| DOC-001: Core Infrastructure | 4 stories | 23 SP | 18-22 hours |
| DOC-002: Template Management | 1 story | 8 SP | 6-8 hours |
| DOC-003: Document Enquiry | 2 stories | 21 SP | 20-25 hours |
| DOC-004: Generic Extraction Engine | 4 stories | 34 SP | 32-40 hours |
| DOC-005: Production Readiness | 5 stories | 31 SP | 26-33 hours |
| **TOTAL** | **16 stories** | **117 SP** | **102-128 hours** |

### Story Point Breakdown
- **1-2 SP** (Simple): 30 minutes - 2 hours
- **3-5 SP** (Medium): 3-5 hours
- **8 SP** (Complex): 6-8 hours
- **13 SP** (Very Complex): 12-15 hours

### Sprint Recommendations

**Sprint 1 (2 weeks):** Foundation
- STORY-001: Database Schema (8 SP)
- STORY-002: Flyway Migrations (2 SP)
- STORY-003: Entity Models (5 SP)
- **Total: 15 SP**

**Sprint 2 (2 weeks):** Security & Templates
- STORY-004: Security & Auth (8 SP)
- STORY-005: Template CRUD (8 SP)
- **Total: 16 SP**

**Sprint 3-4 (4 weeks):** Document Enquiry
- STORY-006: Basic Document Enquiry (8 SP)
- STORY-007: Shared Document Eligibility (13 SP)
- STORY-014: API Spec Compliance (5 SP)
- **Total: 26 SP**

**Sprint 5-6 (4 weeks):** Generic Extraction Engine
- STORY-008: Schema Parser (8 SP)
- STORY-009: Data Orchestration (13 SP)
- STORY-010: Rule Evaluation (8 SP)
- STORY-011: Integration (5 SP)
- **Total: 34 SP**

**Sprint 7-8 (4 weeks):** Production Readiness
- STORY-012: Error Handling (5 SP)
- STORY-013: Monitoring (8 SP)
- STORY-015: Caching (5 SP)
- STORY-016: Security Audit (8 SP)
- **Total: 26 SP**

### Team Allocation Recommendations

**Junior Developer (2-3 years):**
- STORY-002: Flyway Migrations
- Assist with STORY-012: Error Handling
- **Effort: 6-9 hours**

**Mid-level Developer (3-5 years):**
- STORY-003: Entity Models
- STORY-005: Template CRUD
- STORY-014: API Spec Compliance
- STORY-015: Caching
- **Effort: 24-28 hours**

**Senior Developer (5+ years):**
- STORY-001: Database Schema (with DBA)
- STORY-004: Security & Auth
- STORY-006: Basic Document Enquiry
- STORY-007: Shared Document Eligibility
- STORY-008: Schema Parser
- STORY-009: Data Orchestration
- STORY-010: Rule Evaluation
- STORY-011: Extraction Engine Integration
- STORY-013: Monitoring
- **Effort: 72-91 hours**

---

## Notes for Company Implementation

1. **Compliance Requirements:**
   - Follow company's secure coding standards
   - Use approved libraries and frameworks
   - Implement company's logging standards
   - Follow DBA naming conventions

2. **External Dependencies:**
   - Customer Service API (for account type lookup)
   - Account Service API (for account balance, status)
   - Transaction Service API (for transaction history)
   - Company SSO/OAuth provider

3. **Infrastructure Requirements:**
   - PostgreSQL 16+ database
   - Redis for caching
   - Prometheus for metrics
   - Grafana for dashboards (optional)

4. **Testing Strategy:**
   - Unit tests: 80%+ coverage
   - Integration tests: All major flows
   - Load testing: 1000 requests/sec
   - Security testing: OWASP Top 10

5. **Documentation:**
   - OpenAPI 3.0 specification
   - Architecture decision records (ADRs)
   - Deployment guide
   - Operations runbook

---

**END OF JIRA TICKETS PART 2**
