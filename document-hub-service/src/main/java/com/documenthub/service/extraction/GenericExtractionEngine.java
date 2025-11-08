package com.documenthub.service.extraction;

import com.documenthub.model.extraction.ExtractionConfig;
import com.documenthub.model.extraction.ExtractionContext;
import com.documenthub.model.extraction.ExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generic extraction engine that executes configuration-driven data extraction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenericExtractionEngine {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TransformationService transformationService;
    private final RuleEvaluationService ruleEvaluationService;
    private final ObjectMapper objectMapper;

    /**
     * Execute extraction based on configuration
     */
    public Mono<ExtractionResult> execute(ExtractionConfig config, UUID accountId, UUID customerId, String correlationId) {
        log.info("Starting extraction execution for accountId: {}, customerId: {}", accountId, customerId);

        ExtractionContext context = ExtractionContext.builder()
                .accountId(accountId)
                .customerId(customerId)
                .correlationId(correlationId)
                .startTime(System.currentTimeMillis())
                .build();

        // Add input variables to context
        context.addVariable("$input.accountId", accountId.toString());
        context.addVariable("$input.customerId", customerId.toString());
        context.addVariable("$input.correlationId", correlationId);

        return executeExtractionStrategy(config, context)
                .then(evaluateInclusionRules(config, context))
                .flatMap(shouldInclude -> buildExtractionResult(config, context, shouldInclude))
                .doOnSuccess(result -> log.info("Extraction completed. ShouldInclude: {}, ExecutionTime: {}ms",
                        result.getShouldInclude(), result.getExecutionMetrics().getExecutionTimeMs()))
                .doOnError(e -> log.error("Extraction failed", e));
    }

    /**
     * Execute all data sources in extraction strategy
     */
    private Mono<Void> executeExtractionStrategy(ExtractionConfig config, ExtractionContext context) {
        List<ExtractionConfig.DataSourceConfig> dataSources = config.getExtractionStrategy();
        if (dataSources == null || dataSources.isEmpty()) {
            return Mono.empty();
        }

        // Build dependency graph
        Map<String, List<String>> dependencyGraph = buildDependencyGraph(dataSources);

        // Execute in topological order
        return executeDataSourcesInOrder(dataSources, dependencyGraph, context);
    }

    /**
     * Build dependency graph from data sources
     */
    private Map<String, List<String>> buildDependencyGraph(List<ExtractionConfig.DataSourceConfig> dataSources) {
        Map<String, List<String>> graph = new HashMap<>();

        for (ExtractionConfig.DataSourceConfig ds : dataSources) {
            List<String> deps = ds.getDependencies() != null ? ds.getDependencies() : Collections.emptyList();
            graph.put(ds.getId(), deps);
        }

        return graph;
    }

    /**
     * Execute data sources respecting dependencies
     */
    private Mono<Void> executeDataSourcesInOrder(
            List<ExtractionConfig.DataSourceConfig> dataSources,
            Map<String, List<String>> dependencyGraph,
            ExtractionContext context) {

        // Simple sequential execution for now (can be optimized for parallel execution later)
        return Flux.fromIterable(dataSources)
                .concatMap(dataSource -> executeDataSource(dataSource, context)
                        .then(evaluateNextCalls(dataSource, dataSources, context)))
                .then();
    }

    /**
     * Execute a single data source
     */
    private Mono<Void> executeDataSource(ExtractionConfig.DataSourceConfig dataSource, ExtractionContext context) {
        log.debug("Executing data source: {}", dataSource.getId());

        // Check cache first
        if (dataSource.getCache() != null && dataSource.getCache().getEnabled()) {
            String cacheKey = interpolateVariables(dataSource.getCache().getKeyPattern(), context);

            return redisTemplate.opsForValue().get(cacheKey)
                    .doOnNext(cached -> {
                        log.debug("Cache hit for key: {}", cacheKey);
                        context.incrementCacheHit();
                        processCachedResponse(cached, dataSource, context);
                    })
                    .then()
                    .switchIfEmpty(Mono.defer(() -> {
                        log.debug("Cache miss for key: {}", cacheKey);
                        context.incrementCacheMiss();
                        return executeApiCall(dataSource, context, cacheKey);
                    }));
        } else {
            return executeApiCall(dataSource, context, null);
        }
    }

    /**
     * Execute API call to external service
     */
    private Mono<Void> executeApiCall(ExtractionConfig.DataSourceConfig dataSource, ExtractionContext context, String cacheKey) {
        String dataSourceId = dataSource.getId();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(dataSourceId);

        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker OPEN for data source: {}", dataSourceId);
            context.markDataSourceFailed(dataSourceId);
            return Mono.empty();
        }

        ExtractionConfig.EndpointConfig endpoint = dataSource.getEndpoint();
        String url = interpolateVariables(endpoint.getUrl(), context);

        context.incrementApiCall();

        WebClient.RequestBodySpec request = webClient.method(
                org.springframework.http.HttpMethod.valueOf(endpoint.getMethod()))
                .uri(url);

        // Add headers
        if (endpoint.getHeaders() != null) {
            endpoint.getHeaders().forEach((key, value) -> {
                String interpolatedValue = interpolateVariables(value, context);
                request.header(key, interpolatedValue);
            });
        }

        // Add body for POST/PUT
        if (endpoint.getBody() != null) {
            Object interpolatedBody = interpolateVariables(endpoint.getBody(), context);
            request.bodyValue(interpolatedBody);
        }

        // Apply retry policy
        RetryBackoffSpec retrySpec = buildRetrySpec(endpoint.getRetryPolicy());

        return request.retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(endpoint.getTimeout() != null ? endpoint.getTimeout() : 5000))
                .retryWhen(retrySpec)
                .doOnSuccess(response -> {
                    log.debug("API call successful for data source: {}", dataSourceId);
                    processResponse(response, dataSource, context, cacheKey);
                    circuitBreaker.onSuccess(0, Duration.ZERO);
                    context.markDataSourceSuccess(dataSourceId);
                })
                .doOnError(e -> {
                    log.error("API call failed for data source: {}", dataSourceId, e);
                    circuitBreaker.onError(0, Duration.ZERO, e);
                    context.markDataSourceFailed(dataSourceId);
                })
                .then()
                .onErrorResume(e -> Mono.empty()); // Continue even if this data source fails
    }

    /**
     * Process API response and extract data
     */
    private void processResponse(String response, ExtractionConfig.DataSourceConfig dataSource, ExtractionContext context, String cacheKey) {
        try {
            // Parse JSON response
            Object jsonDoc = JsonPath.parse(response).json();

            // Extract fields using JSONPath
            if (dataSource.getResponseMapping() != null && dataSource.getResponseMapping().getExtract() != null) {
                dataSource.getResponseMapping().getExtract().forEach((fieldName, jsonPath) -> {
                    try {
                        Object extractedValue = JsonPath.read(jsonDoc, jsonPath);
                        context.addVariable(fieldName, extractedValue);
                        log.debug("Extracted {}: {}", fieldName, extractedValue);
                    } catch (Exception e) {
                        log.warn("Failed to extract field {} using path {}", fieldName, jsonPath, e);
                    }
                });
            }

            // Apply transformations
            if (dataSource.getResponseMapping() != null && dataSource.getResponseMapping().getTransform() != null) {
                dataSource.getResponseMapping().getTransform().forEach((fieldName, transformConfig) -> {
                    Object transformedValue = transformationService.transform(transformConfig, context);
                    context.addVariable(fieldName, transformedValue);
                    log.debug("Transformed {}: {}", fieldName, transformedValue);
                });
            }

            // Validate extracted data
            if (dataSource.getResponseMapping() != null && dataSource.getResponseMapping().getValidate() != null) {
                boolean validationPassed = validateExtractedData(dataSource.getResponseMapping().getValidate(), context);
                if (!validationPassed) {
                    log.warn("Validation failed for data source: {}", dataSource.getId());
                    context.markDataSourceFailed(dataSource.getId());
                    return;
                }
            }

            // Cache the response
            if (cacheKey != null && dataSource.getCache() != null) {
                Integer ttl = dataSource.getCache().getTtl();
                redisTemplate.opsForValue().set(cacheKey, response, Duration.ofSeconds(ttl))
                        .subscribe();
            }

        } catch (Exception e) {
            log.error("Failed to process response for data source: {}", dataSource.getId(), e);
            context.markDataSourceFailed(dataSource.getId());
        }
    }

    /**
     * Process cached response
     */
    private void processCachedResponse(Object cached, ExtractionConfig.DataSourceConfig dataSource, ExtractionContext context) {
        if (cached instanceof String) {
            processResponse((String) cached, dataSource, context, null);
        }
    }

    /**
     * Validate extracted data against rules
     */
    private boolean validateExtractedData(Map<String, ExtractionConfig.ValidationRule> validationRules, ExtractionContext context) {
        for (Map.Entry<String, ExtractionConfig.ValidationRule> entry : validationRules.entrySet()) {
            String fieldName = entry.getKey();
            ExtractionConfig.ValidationRule rule = entry.getValue();
            Object value = context.getVariable(fieldName);

            // Required check
            if (Boolean.TRUE.equals(rule.getRequired()) && value == null) {
                log.warn("Validation failed: {} is required but was null", fieldName);
                return false;
            }

            if (value != null) {
                // Pattern check
                if (rule.getPattern() != null && value instanceof String) {
                    if (!Pattern.matches(rule.getPattern(), (String) value)) {
                        log.warn("Validation failed: {} does not match pattern {}", fieldName, rule.getPattern());
                        return false;
                    }
                }

                // Min/Max checks for numbers
                if (value instanceof Number) {
                    Number numValue = (Number) value;
                    if (rule.getMin() != null && numValue.doubleValue() < ((Number) rule.getMin()).doubleValue()) {
                        log.warn("Validation failed: {} is less than min {}", fieldName, rule.getMin());
                        return false;
                    }
                    if (rule.getMax() != null && numValue.doubleValue() > ((Number) rule.getMax()).doubleValue()) {
                        log.warn("Validation failed: {} is greater than max {}", fieldName, rule.getMax());
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Evaluate next calls based on conditions
     */
    private Mono<Void> evaluateNextCalls(
            ExtractionConfig.DataSourceConfig dataSource,
            List<ExtractionConfig.DataSourceConfig> allDataSources,
            ExtractionContext context) {

        if (dataSource.getNextCalls() == null || dataSource.getNextCalls().isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(dataSource.getNextCalls())
                .filter(nextCall -> evaluateNextCallCondition(nextCall.getCondition(), context))
                .flatMap(nextCall -> {
                    String targetId = nextCall.getTargetDataSource();
                    return Flux.fromIterable(allDataSources)
                            .filter(ds -> ds.getId().equals(targetId))
                            .next()
                            .flatMap(targetDs -> executeDataSource(targetDs, context));
                })
                .then();
    }

    /**
     * Evaluate next call condition
     */
    private boolean evaluateNextCallCondition(ExtractionConfig.Condition condition, ExtractionContext context) {
        if (condition == null) {
            return true;
        }

        Object value = context.getVariable(condition.getField());
        String operator = condition.getOperator();

        switch (operator) {
            case "notNull":
            case "exists":
                return value != null;
            case "equals":
                return Objects.equals(value, condition.getValue());
            default:
                return true;
        }
    }

    /**
     * Evaluate inclusion rules
     */
    private Mono<Boolean> evaluateInclusionRules(ExtractionConfig config, ExtractionContext context) {
        if (config.getInclusionRules() == null) {
            return Mono.just(true);
        }

        return Mono.fromCallable(() -> ruleEvaluationService.evaluate(config.getInclusionRules(), context));
    }

    /**
     * Build extraction result
     */
    private Mono<ExtractionResult> buildExtractionResult(ExtractionConfig config, ExtractionContext context, Boolean shouldInclude) {
        ExtractionResult.MatchingCriteria matchingCriteria = buildMatchingCriteria(config, context);

        ExtractionResult.RuleEvaluation ruleEvaluation = ruleEvaluationService.getLastEvaluation();

        ExtractionResult.ExecutionMetrics metrics = ExtractionResult.ExecutionMetrics.builder()
                .totalApiCalls(context.getTotalApiCalls())
                .cacheHits(context.getCacheHits())
                .executionTimeMs(context.getExecutionTimeMs())
                .dataSourcesExecuted(new ArrayList<>(context.getDataSourceStatus().keySet()))
                .build();

        return Mono.just(ExtractionResult.builder()
                .shouldInclude(shouldInclude)
                .extractedVariables(new HashMap<>(context.getVariables()))
                .matchingCriteria(matchingCriteria)
                .ruleEvaluation(ruleEvaluation)
                .executionMetrics(metrics)
                .build());
    }

    /**
     * Build matching criteria from config and context
     */
    private ExtractionResult.MatchingCriteria buildMatchingCriteria(ExtractionConfig config, ExtractionContext context) {
        ExtractionConfig.DocumentMatchingStrategy strategy = config.getDocumentMatchingStrategy();
        if (strategy == null) {
            return null;
        }

        ExtractionResult.MatchingCriteria.MatchingCriteriaBuilder builder = ExtractionResult.MatchingCriteria.builder()
                .matchBy(strategy.getMatchBy());

        if ("reference_key".equals(strategy.getMatchBy())) {
            String refKeyValue = interpolateVariables(config.getOutputMapping().getDocumentReferenceKey(), context);
            builder.referenceKeyType(strategy.getReferenceKeyType())
                    .referenceKeyValue(refKeyValue);
        } else if ("metadata".equals(strategy.getMatchBy())) {
            Map<String, Object> metadata = new HashMap<>();
            if (config.getOutputMapping() != null && config.getOutputMapping().getDocumentMetadata() != null) {
                config.getOutputMapping().getDocumentMetadata().forEach((key, valueTemplate) -> {
                    String interpolated = interpolateVariables(valueTemplate, context);
                    metadata.put(key, interpolated);
                });
            }
            builder.metadata(metadata);
        }

        return builder.build();
    }

    /**
     * Interpolate variables in string template
     */
    private String interpolateVariables(String template, ExtractionContext context) {
        if (template == null) {
            return null;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return result;
    }

    /**
     * Interpolate variables in object (for request bodies)
     */
    @SuppressWarnings("unchecked")
    private Object interpolateVariables(Object obj, ExtractionContext context) {
        if (obj instanceof String) {
            return interpolateVariables((String) obj, context);
        } else if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Map<String, Object> result = new HashMap<>();
            map.forEach((key, value) -> result.put(key, interpolateVariables(value, context)));
            return result;
        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            return list.stream()
                    .map(item -> interpolateVariables(item, context))
                    .collect(Collectors.toList());
        }
        return obj;
    }

    /**
     * Build retry spec from retry policy
     */
    private RetryBackoffSpec buildRetrySpec(ExtractionConfig.RetryPolicy policy) {
        if (policy == null) {
            return reactor.util.retry.Retry.max(0);
        }

        int maxAttempts = policy.getMaxAttempts() != null ? policy.getMaxAttempts() : 3;
        long initialDelay = policy.getInitialDelayMs() != null ? policy.getInitialDelayMs() : 100;

        return reactor.util.retry.Retry.backoff(maxAttempts, Duration.ofMillis(initialDelay))
                .filter(throwable -> shouldRetry(throwable, policy))
                .doBeforeRetry(signal -> log.debug("Retrying API call, attempt: {}", signal.totalRetries() + 1));
    }

    /**
     * Determine if error should trigger retry
     */
    private boolean shouldRetry(Throwable throwable, ExtractionConfig.RetryPolicy policy) {
        if (policy.getRetryOn() == null || policy.getRetryOn().isEmpty()) {
            return true; // Retry on all errors
        }

        // Check if it's a WebClient error with status code in retryOn list
        // Implementation depends on how you handle WebClient errors
        return true;
    }
}
