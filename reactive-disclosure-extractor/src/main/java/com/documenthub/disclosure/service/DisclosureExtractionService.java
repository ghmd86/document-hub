package com.documenthub.disclosure.service;

import com.documenthub.disclosure.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Reactive service for extracting disclosure codes using configured API chain
 */
@Slf4j
@Service
public class DisclosureExtractionService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Configuration jsonPathConfig;

    // Metrics
    private final Timer extractionTimer;
    private final Counter extractionSuccessCounter;
    private final Counter extractionFailureCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public DisclosureExtractionService(
            WebClient.Builder webClientBuilder,
            ReactiveRedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {

        this.webClient = webClientBuilder.build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        // Configure JSONPath
        this.jsonPathConfig = Configuration.builder()
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();

        // Initialize metrics
        this.extractionTimer = Timer.builder("disclosure.extraction.time")
                .description("Time taken to extract disclosure code")
                .register(meterRegistry);

        this.extractionSuccessCounter = Counter.builder("disclosure.extraction.success")
                .description("Successful disclosure extractions")
                .register(meterRegistry);

        this.extractionFailureCounter = Counter.builder("disclosure.extraction.failure")
                .description("Failed disclosure extractions")
                .register(meterRegistry);

        this.cacheHitCounter = Counter.builder("disclosure.cache.hit")
                .description("Cache hits")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("disclosure.cache.miss")
                .description("Cache misses")
                .register(meterRegistry);
    }

    /**
     * Extract disclosure code based on configuration
     */
    public Mono<ExtractionResult> extract(
            ExtractionConfig config,
            Map<String, Object> inputContext,
            String correlationId) {

        long startTime = System.currentTimeMillis();
        ExecutionContext context = new ExecutionContext(inputContext, correlationId);

        return extractionTimer.record(() ->
                executeStrategy(config, context)
                        .map(results -> buildResult(results, context, startTime, true, null))
                        .doOnSuccess(result -> extractionSuccessCounter.increment())
                        .onErrorResume(error -> {
                            log.error("[{}] Extraction failed", correlationId, error);
                            extractionFailureCounter.increment();

                            // Return default response if configured
                            if (config.getExecutionRules().getErrorHandling() != null &&
                                    config.getExecutionRules().getErrorHandling().getDefaultResponse() != null) {
                                return Mono.just(buildResult(
                                        config.getExecutionRules().getErrorHandling().getDefaultResponse(),
                                        context,
                                        startTime,
                                        false,
                                        error.getMessage()
                                ));
                            }
                            return Mono.error(error);
                        })
        );
    }

    /**
     * Execute the extraction strategy
     */
    private Mono<Map<String, Object>> executeStrategy(
            ExtractionConfig config,
            ExecutionContext context) {

        String startFrom = config.getExecutionRules().getStartFrom();
        DataSource firstDataSource = findDataSource(config, startFrom);

        if (firstDataSource == null) {
            return Mono.error(new IllegalArgumentException(
                    "Start data source not found: " + startFrom));
        }

        return executeDataSourceChain(config, firstDataSource, context, new HashMap<>());
    }

    /**
     * Execute data source chain recursively
     */
    private Mono<Map<String, Object>> executeDataSourceChain(
            ExtractionConfig config,
            DataSource dataSource,
            ExecutionContext context,
            Map<String, Object> results) {

        log.info("[{}] Executing data source: {}", context.getCorrelationId(), dataSource.getId());

        return executeDataSource(dataSource, context, results)
                .flatMap(extractedData -> {
                    // Store result
                    if (dataSource.getStoreAs() != null) {
                        results.put(dataSource.getStoreAs(), extractedData);
                    }
                    results.put(dataSource.getId(), extractedData);

                    // Check for next calls
                    DataSource nextDataSource = determineNextDataSource(
                            config, dataSource, results);

                    if (nextDataSource != null) {
                        // Continue chain
                        return executeDataSourceChain(config, nextDataSource, context, results);
                    } else {
                        // End of chain
                        return Mono.just(results);
                    }
                });
    }

    /**
     * Execute a single data source
     */
    private Mono<Map<String, Object>> executeDataSource(
            DataSource dataSource,
            ExecutionContext context,
            Map<String, Object> previousResults) {

        // Check cache first
        if (dataSource.getCache() != null && dataSource.getCache().getEnabled()) {
            String cacheKey = interpolateString(
                    dataSource.getCache().getKeyPattern(),
                    context,
                    previousResults);

            return getFromCache(cacheKey)
                    .flatMap(cached -> {
                        cacheHitCounter.increment();
                        log.debug("[{}] Cache hit: {}", context.getCorrelationId(), cacheKey);
                        return Mono.just(cached);
                    })
                    .switchIfEmpty(
                            Mono.defer(() -> {
                                cacheMissCounter.increment();
                                log.debug("[{}] Cache miss: {}", context.getCorrelationId(), cacheKey);
                                return fetchAndProcess(dataSource, context, previousResults)
                                        .flatMap(result ->
                                                setInCache(cacheKey, result, dataSource.getCache().getTtl())
                                                        .thenReturn(result)
                                        );
                            })
                    );
        }

        // No cache, fetch directly
        return fetchAndProcess(dataSource, context, previousResults);
    }

    /**
     * Fetch data from API and process response
     */
    private Mono<Map<String, Object>> fetchAndProcess(
            DataSource dataSource,
            ExecutionContext context,
            Map<String, Object> previousResults) {

        return makeAPICall(dataSource, context, previousResults)
                .flatMap(response -> processResponse(response, dataSource, context));
    }

    /**
     * Make API call with retry and circuit breaker
     */
    private Mono<String> makeAPICall(
            DataSource dataSource,
            ExecutionContext context,
            Map<String, Object> previousResults) {

        EndpointConfig endpoint = dataSource.getEndpoint();
        String url = interpolateString(endpoint.getUrl(), context, previousResults);
        Map<String, String> headers = interpolateMap(endpoint.getHeaders(), context, previousResults);

        log.info("[{}] API call: {} {}", context.getCorrelationId(), endpoint.getMethod(), url);

        context.incrementApiCalls();

        WebClient.RequestHeadersSpec<?> request = webClient
                .method(HttpMethod.valueOf(endpoint.getMethod()))
                .uri(url)
                .headers(h -> headers.forEach(h::add));

        Mono<String> apiCall = request
                .retrieve()
                .onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> handle4xxError(clientResponse, dataSource, context)
                )
                .onStatus(
                        HttpStatus::is5xxServerError,
                        clientResponse -> handle5xxError(clientResponse, dataSource, context)
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(endpoint.getTimeout()));

        // Apply retry policy
        if (endpoint.getRetryPolicy() != null) {
            apiCall = applyRetryPolicy(apiCall, endpoint.getRetryPolicy(), context);
        }

        // Apply circuit breaker if enabled
        if (dataSource.getErrorHandling() != null) {
            apiCall = applyCircuitBreaker(apiCall, dataSource.getId());
        }

        return apiCall;
    }

    /**
     * Handle 4xx errors
     */
    private Mono<Throwable> handle4xxError(
            ClientResponse response,
            DataSource dataSource,
            ExecutionContext context) {

        if (response.statusCode() == HttpStatus.NOT_FOUND &&
                dataSource.getErrorHandling() != null &&
                dataSource.getErrorHandling().getOn404() != null) {

            ErrorAction action = dataSource.getErrorHandling().getOn404();
            if ("return-default".equals(action.getAction())) {
                log.warn("[{}] 404 Not Found, returning default", context.getCorrelationId());
                // This will be caught and handled in processResponse
                return Mono.error(new NotFoundException(action.getMessage()));
            }
        }

        return response.bodyToMono(String.class)
                .flatMap(body -> Mono.error(
                        new ApiException("API error: " + response.statusCode() + " - " + body)
                ));
    }

    /**
     * Handle 5xx errors
     */
    private Mono<Throwable> handle5xxError(
            ClientResponse response,
            DataSource dataSource,
            ExecutionContext context) {

        return response.bodyToMono(String.class)
                .flatMap(body -> Mono.error(
                        new ApiException("Server error: " + response.statusCode() + " - " + body)
                ));
    }

    /**
     * Apply retry policy
     */
    private Mono<String> applyRetryPolicy(
            Mono<String> mono,
            RetryPolicy retryPolicy,
            ExecutionContext context) {

        RetryBackoffSpec retrySpec;

        if ("exponential".equals(retryPolicy.getBackoffStrategy())) {
            retrySpec = reactor.util.retry.Retry.backoff(
                            retryPolicy.getMaxAttempts() - 1,
                            Duration.ofMillis(retryPolicy.getInitialDelayMs())
                    )
                    .maxBackoff(Duration.ofMillis(retryPolicy.getMaxDelayMs()))
                    .filter(throwable -> shouldRetry(throwable, retryPolicy))
                    .doBeforeRetry(signal ->
                            log.warn("[{}] Retrying API call, attempt: {}",
                                    context.getCorrelationId(),
                                    signal.totalRetries() + 1)
                    );
        } else {
            // Linear backoff
            retrySpec = reactor.util.retry.Retry.fixedDelay(
                            retryPolicy.getMaxAttempts() - 1,
                            Duration.ofMillis(retryPolicy.getInitialDelayMs())
                    )
                    .filter(throwable -> shouldRetry(throwable, retryPolicy))
                    .doBeforeRetry(signal ->
                            log.warn("[{}] Retrying API call, attempt: {}",
                                    context.getCorrelationId(),
                                    signal.totalRetries() + 1)
                    );
        }

        return mono.retryWhen(retrySpec);
    }

    /**
     * Check if error should be retried
     */
    private boolean shouldRetry(Throwable throwable, RetryPolicy retryPolicy) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            return retryPolicy.getRetryOn() != null &&
                    retryPolicy.getRetryOn().contains(ex.getStatusCode().value());
        }
        return false;
    }

    /**
     * Apply circuit breaker
     */
    private Mono<String> applyCircuitBreaker(Mono<String> mono, String dataSourceId) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(dataSourceId);
        return mono.transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    /**
     * Process API response - extract, transform, validate
     */
    private Mono<Map<String, Object>> processResponse(
            String responseBody,
            DataSource dataSource,
            ExecutionContext context) {

        try {
            // Parse JSON
            Object jsonDoc = Configuration.defaultConfiguration().jsonProvider()
                    .parse(responseBody);

            // Extract fields
            Map<String, Object> extracted = extractFields(
                    jsonDoc,
                    dataSource.getResponseMapping().getExtract()
            );

            log.debug("[{}] Extracted fields: {}", context.getCorrelationId(), extracted.keySet());

            // Transform
            if (dataSource.getResponseMapping().getTransform() != null) {
                extracted = transformData(extracted, dataSource.getResponseMapping().getTransform());
            }

            // Validate
            if (dataSource.getResponseMapping().getValidate() != null) {
                validateData(extracted, dataSource.getResponseMapping().getValidate(), dataSource);
            }

            return Mono.just(extracted);

        } catch (Exception e) {
            log.error("[{}] Failed to process response", context.getCorrelationId(), e);
            return Mono.error(new ExtractionException("Failed to process response", e));
        }
    }

    /**
     * Extract fields using JSONPath
     */
    private Map<String, Object> extractFields(Object jsonDoc, Map<String, String> extractConfig) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, String> entry : extractConfig.entrySet()) {
            String fieldName = entry.getKey();
            String jsonPath = entry.getValue();

            try {
                Object value = JsonPath.using(jsonPathConfig).parse(jsonDoc).read(jsonPath);
                result.put(fieldName, value);
            } catch (Exception e) {
                log.warn("Failed to extract field '{}' with path '{}': {}",
                        fieldName, jsonPath, e.getMessage());
                result.put(fieldName, null);
            }
        }

        return result;
    }

    /**
     * Transform extracted data
     */
    private Map<String, Object> transformData(
            Map<String, Object> data,
            Map<String, Transform> transformConfig) {

        Map<String, Object> transformed = new HashMap<>(data);

        for (Map.Entry<String, Transform> entry : transformConfig.entrySet()) {
            String fieldName = entry.getKey();
            Transform transform = entry.getValue();
            Object value = transformed.get(fieldName);

            switch (transform.getType()) {
                case "selectFirst":
                    if (value instanceof List && !((List<?>) value).isEmpty()) {
                        transformed.put(fieldName, ((List<?>) value).get(0));
                    } else if (value == null || (value instanceof List && ((List<?>) value).isEmpty())) {
                        transformed.put(fieldName, transform.getFallback());
                    }
                    break;

                case "uppercase":
                    if (value instanceof String) {
                        transformed.put(fieldName, ((String) value).toUpperCase());
                    }
                    break;

                case "lowercase":
                    if (value instanceof String) {
                        transformed.put(fieldName, ((String) value).toLowerCase());
                    }
                    break;

                case "trim":
                    if (value instanceof String) {
                        transformed.put(fieldName, ((String) value).trim());
                    }
                    break;
            }
        }

        return transformed;
    }

    /**
     * Validate extracted data
     */
    private void validateData(
            Map<String, Object> data,
            Map<String, Validation> validationConfig,
            DataSource dataSource) {

        for (Map.Entry<String, Validation> entry : validationConfig.entrySet()) {
            String fieldName = entry.getKey();
            Validation validation = entry.getValue();
            Object value = data.get(fieldName);

            // Required check
            if (Boolean.TRUE.equals(validation.getRequired()) && (value == null || value.toString().isEmpty())) {
                throw new ValidationException(
                        validation.getErrorMessage() != null ?
                                validation.getErrorMessage() :
                                "Missing required field: " + fieldName
                );
            }

            // Pattern check
            if (validation.getPattern() != null && value != null) {
                if (!Pattern.matches(validation.getPattern(), value.toString())) {
                    throw new ValidationException(
                            validation.getErrorMessage() != null ?
                                    validation.getErrorMessage() :
                                    "Invalid format for field: " + fieldName
                    );
                }
            }

            // Date validations
            if ("date".equals(validation.getType()) && value != null) {
                LocalDateTime date = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME);
                if (Boolean.TRUE.equals(validation.getValidateInPast()) && date.isAfter(LocalDateTime.now())) {
                    throw new ValidationException("Date must be in the past: " + fieldName);
                }
                if (Boolean.TRUE.equals(validation.getValidateInFuture()) && date.isBefore(LocalDateTime.now())) {
                    throw new ValidationException("Date must be in the future: " + fieldName);
                }
            }
        }
    }

    /**
     * Determine next data source to execute
     */
    private DataSource determineNextDataSource(
            ExtractionConfig config,
            DataSource currentDataSource,
            Map<String, Object> results) {

        if (currentDataSource.getNextCalls() == null || currentDataSource.getNextCalls().isEmpty()) {
            return null;
        }

        for (NextCall nextCall : currentDataSource.getNextCalls()) {
            // Check condition if specified
            if (nextCall.getCondition() != null) {
                Map<String, Object> currentResult = (Map<String, Object>) results.get(currentDataSource.getId());
                if (!evaluateCondition(nextCall.getCondition(), currentResult)) {
                    continue;
                }
            }

            // Find and return the next data source
            return findDataSource(config, nextCall.getTargetDataSource());
        }

        return null;
    }

    /**
     * Evaluate condition
     */
    private boolean evaluateCondition(Condition condition, Map<String, Object> data) {
        Object fieldValue = data.get(condition.getField());

        switch (condition.getOperator()) {
            case "notNull":
                return fieldValue != null;
            case "equals":
                return Objects.equals(fieldValue, condition.getValue());
            case "greaterThan":
                if (fieldValue instanceof Number && condition.getValue() instanceof Number) {
                    return ((Number) fieldValue).doubleValue() > ((Number) condition.getValue()).doubleValue();
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Find data source by ID
     */
    private DataSource findDataSource(ExtractionConfig config, String id) {
        return config.getExtractionStrategy().stream()
                .filter(ds -> ds.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Interpolate string with variables
     */
    private String interpolateString(
            String template,
            ExecutionContext context,
            Map<String, Object> results) {

        if (template == null) {
            return null;
        }

        String result = template;

        // Replace ${$input.xxx}
        for (Map.Entry<String, Object> entry : context.getInput().entrySet()) {
            result = result.replace("${$input." + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        // Replace ${correlationId}
        result = result.replace("${x-correlation-Id}", context.getCorrelationId());

        // Replace ${env.xxx}
        result = result.replaceAll("\\$\\{env\\.([^}]+)\\}", matcher -> {
            String envVar = System.getenv(matcher.group(1));
            return envVar != null ? envVar : matcher.group(0);
        });

        // Replace ${dataSourceId.field}
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> dataSourceResult = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> field : dataSourceResult.entrySet()) {
                    String placeholder = "${" + entry.getKey() + "." + field.getKey() + "}";
                    if (result.contains(placeholder)) {
                        result = result.replace(placeholder, String.valueOf(field.getValue()));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Interpolate map
     */
    private Map<String, String> interpolateMap(
            Map<String, String> map,
            ExecutionContext context,
            Map<String, Object> results) {

        if (map == null) {
            return new HashMap<>();
        }

        Map<String, String> interpolated = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            interpolated.put(entry.getKey(), interpolateString(entry.getValue(), context, results));
        }
        return interpolated;
    }

    /**
     * Get from cache
     */
    private Mono<Map<String, Object>> getFromCache(String key) {
        return redisTemplate.opsForValue()
                .get(key)
                .map(value -> (Map<String, Object>) value)
                .onErrorResume(error -> {
                    log.warn("Cache get error for key: {}", key, error);
                    return Mono.empty();
                });
    }

    /**
     * Set in cache
     */
    private Mono<Boolean> setInCache(String key, Map<String, Object> value, Integer ttlSeconds) {
        return redisTemplate.opsForValue()
                .set(key, value, Duration.ofSeconds(ttlSeconds))
                .onErrorResume(error -> {
                    log.warn("Cache set error for key: {}", key, error);
                    return Mono.just(false);
                });
    }

    /**
     * Build extraction result
     */
    private ExtractionResult buildResult(
            Map<String, Object> data,
            ExecutionContext context,
            long startTime,
            boolean success,
            String errorMessage) {

        ExtractionResult result = new ExtractionResult();
        result.setSuccess(success);
        result.setData(data);

        if (!success && errorMessage != null) {
            result.setError(new ExtractionResult.ErrorDetail("EXTRACTION_FAILED", errorMessage));
        }

        ExtractionResult.Metadata metadata = new ExtractionResult.Metadata();
        metadata.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        metadata.setCacheHits(context.getCacheHits());
        metadata.setApiCalls(context.getApiCalls());
        metadata.setTraceId(context.getCorrelationId());
        result.setMetadata(metadata);

        return result;
    }

    // Inner classes
    @lombok.Data
    public static class ExecutionContext {
        private final Map<String, Object> input;
        private final String correlationId;
        private int cacheHits = 0;
        private int apiCalls = 0;

        public void incrementCacheHits() {
            this.cacheHits++;
        }

        public void incrementApiCalls() {
            this.apiCalls++;
        }
    }

    @lombok.Data
    public static class ExtractionResult {
        private boolean success;
        private Map<String, Object> data;
        private ErrorDetail error;
        private Metadata metadata;

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class ErrorDetail {
            private String code;
            private String message;
        }

        @lombok.Data
        public static class Metadata {
            private long executionTimeMs;
            private int cacheHits;
            private int apiCalls;
            private String traceId;
        }
    }

    // Custom exceptions
    public static class ExtractionException extends RuntimeException {
        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
