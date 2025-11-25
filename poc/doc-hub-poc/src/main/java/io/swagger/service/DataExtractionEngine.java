package io.swagger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.model.config.*;
import io.swagger.model.context.ExtractionContext;
import io.swagger.model.DocumentListRequest;
import io.swagger.util.JsonPathExtractor;
import io.swagger.util.PlaceholderResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class DataExtractionEngine {

    @Autowired
    private WebClient webClient;

    @Autowired
    private JsonPathExtractor jsonPathExtractor;

    @Autowired
    private PlaceholderResolver placeholderResolver;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Execute multi-step extraction strategy
     */
    public Mono<ExtractionContext> executeExtractionStrategy(
        DataExtractionConfig config,
        DocumentListRequest request
    ) {
        if (config == null || config.getExtractionStrategy() == null) {
            log.warn("No extraction strategy defined, returning empty context");
            return Mono.just(createInitialContext(request));
        }

        ExtractionContext context = createInitialContext(request);

        log.info("Starting extraction strategy with {} steps",
            config.getExtractionStrategy().size());

        // Execute data sources based on execution mode
        if (config.getExecutionRules() != null &&
            "parallel".equalsIgnoreCase(config.getExecutionRules().getExecutionMode())) {
            return executeParallel(config.getExtractionStrategy(), context);
        } else {
            return executeSequential(config.getExtractionStrategy(), context);
        }
    }

    /**
     * Create initial context with request data
     */
    private ExtractionContext createInitialContext(DocumentListRequest request) {
        ExtractionContext context = ExtractionContext.builder()
            .customerId(request.getCustomerId())
            .correlationId(UUID.randomUUID().toString())
            .build();

        // Add request data to context
        if (request.getAccountId() != null && !request.getAccountId().isEmpty()) {
            context.addVariable("$input.accountId", request.getAccountId().get(0));
            context.setAccountId(UUID.fromString(request.getAccountId().get(0)));
        }
        if (request.getCustomerId() != null) {
            context.addVariable("$input.customerId", request.getCustomerId().toString());
        }
        if (request.getReferenceKey() != null) {
            context.addVariable("$input.referenceKey", request.getReferenceKey());
        }
        if (request.getReferenceKeyType() != null) {
            context.addVariable("$input.referenceKeyType", request.getReferenceKeyType());
        }

        return context;
    }

    /**
     * Execute data sources sequentially (for chained calls)
     */
    private Mono<ExtractionContext> executeSequential(
        List<DataSourceConfig> dataSources,
        ExtractionContext context
    ) {
        return Flux.fromIterable(dataSources)
            .filter(ds -> shouldExecute(ds, context))
            .concatMap(dataSource -> executeDataSource(dataSource, context)
                .doOnSuccess(v -> log.debug("Completed data source: {}", dataSource.getId()))
                .doOnError(e -> log.error("Failed data source: {}", dataSource.getId(), e))
                .onErrorResume(e -> {
                    context.markDataSourceFailed(dataSource.getId());
                    return Mono.empty(); // Continue with other data sources
                })
            )
            .then(Mono.just(context));
    }

    /**
     * Execute independent data sources in parallel
     */
    private Mono<ExtractionContext> executeParallel(
        List<DataSourceConfig> dataSources,
        ExtractionContext context
    ) {
        return Flux.fromIterable(dataSources)
            .filter(ds -> ds.getDependencies() == null || ds.getDependencies().isEmpty())
            .flatMap(dataSource -> executeDataSource(dataSource, context)
                .doOnSuccess(v -> log.debug("Completed data source: {}", dataSource.getId()))
                .doOnError(e -> log.error("Failed data source: {}", dataSource.getId(), e))
                .onErrorResume(e -> {
                    context.markDataSourceFailed(dataSource.getId());
                    return Mono.empty();
                })
            )
            .then(Mono.just(context));
    }

    /**
     * Check if data source should execute based on dependencies
     */
    private boolean shouldExecute(DataSourceConfig dataSource, ExtractionContext context) {
        if (dataSource.getDependencies() == null || dataSource.getDependencies().isEmpty()) {
            return true;
        }

        // Check if all dependencies are satisfied
        boolean canExecute = dataSource.getDependencies().stream()
            .allMatch(dep -> context.getVariables().containsKey(dep) &&
                           context.getVariables().get(dep) != null);

        if (!canExecute) {
            log.debug("Skipping data source {} due to unsatisfied dependencies: {}",
                dataSource.getId(), dataSource.getDependencies());
        }

        return canExecute;
    }

    /**
     * Execute a single data source (API call)
     */
    private Mono<Void> executeDataSource(
        DataSourceConfig dataSource,
        ExtractionContext context
    ) {
        log.debug("Executing data source: {}", dataSource.getId());

        // Resolve URL with placeholders
        String url = placeholderResolver.resolve(
            dataSource.getEndpoint().getUrl(),
            context
        );

        if (url == null || url.contains("${")) {
            log.warn("Could not resolve URL for {}: {}",
                dataSource.getId(), dataSource.getEndpoint().getUrl());
            return Mono.empty();
        }

        log.debug("Resolved URL: {}", url);

        // Determine timeout
        int timeout = dataSource.getEndpoint().getTimeout() != null ?
            dataSource.getEndpoint().getTimeout() : 5000;

        // Make HTTP call
        return webClient
            .method(HttpMethod.valueOf(dataSource.getEndpoint().getMethod()))
            .uri(url)
            .retrieve()
            .onStatus(
                HttpStatus::is4xxClientError,
                response -> {
                    log.error("Client error {} for data source: {}",
                        response.statusCode(), dataSource.getId());
                    return Mono.error(new RuntimeException(
                        "Client error: " + response.statusCode()));
                }
            )
            .onStatus(
                HttpStatus::is5xxServerError,
                response -> {
                    log.error("Server error {} for data source: {}",
                        response.statusCode(), dataSource.getId());
                    return Mono.error(new RuntimeException(
                        "Server error: " + response.statusCode()));
                }
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(timeout))
            .doOnNext(responseBody -> {
                log.debug("Received response from {}: {} chars",
                    dataSource.getId(), responseBody.length());

                // Extract data using JSONPath
                Map<String, Object> extractedData = extractDataFromResponse(
                    responseBody,
                    dataSource.getResponseMapping()
                );

                // Validate extracted data
                if (dataSource.getResponseMapping() != null &&
                    dataSource.getResponseMapping().getValidate() != null) {
                    validateExtractedData(
                        extractedData,
                        dataSource.getResponseMapping().getValidate()
                    );
                }

                // Store in context
                extractedData.forEach((key, value) -> {
                    context.addVariable(key, value);
                    log.debug("Extracted {}: {}", key, value);
                });

                // Mark success
                context.markDataSourceSuccess(dataSource.getId());
                context.incrementTotalApiCalls();

                // Check if next calls should be triggered
                triggerNextCalls(dataSource, context);
            })
            .then();
    }

    /**
     * Extract data from JSON response using JSONPath
     */
    private Map<String, Object> extractDataFromResponse(
        String responseBody,
        ResponseMapping responseMapping
    ) {
        Map<String, Object> extracted = new HashMap<>();

        if (responseMapping == null || responseMapping.getExtract() == null) {
            return extracted;
        }

        // Apply JSONPath expressions
        responseMapping.getExtract().forEach((fieldName, jsonPath) -> {
            try {
                Object value = jsonPathExtractor.extract(responseBody, jsonPath);
                if (value != null) {
                    extracted.put(fieldName, value);
                    log.debug("Extracted field {}: {}", fieldName, value);
                }
            } catch (Exception e) {
                log.warn("Failed to extract {} using JSONPath {}: {}",
                    fieldName, jsonPath, e.getMessage());
            }
        });

        return extracted;
    }

    /**
     * Validate extracted data against rules
     */
    private void validateExtractedData(
        Map<String, Object> extractedData,
        Map<String, ValidationRule> validationRules
    ) {
        validationRules.forEach((fieldName, rule) -> {
            Object value = extractedData.get(fieldName);

            if (rule.getRequired() != null && rule.getRequired() && value == null) {
                log.warn("Required field {} is missing", fieldName);
            }

            if (value != null && rule.getPattern() != null) {
                String stringValue = value.toString();
                if (!stringValue.matches(rule.getPattern())) {
                    log.warn("Field {} value '{}' does not match pattern '{}'",
                        fieldName, stringValue, rule.getPattern());
                }
            }
        });
    }

    /**
     * Trigger next API calls based on conditions
     */
    private void triggerNextCalls(DataSourceConfig dataSource, ExtractionContext context) {
        if (dataSource.getNextCalls() == null || dataSource.getNextCalls().isEmpty()) {
            return;
        }

        dataSource.getNextCalls().forEach(nextCall -> {
            boolean conditionMet = evaluateCondition(nextCall.getCondition(), context);

            if (conditionMet) {
                log.debug("Condition met for next call: {}", nextCall.getTargetDataSource());
                // The next data source will be picked up in the sequential execution
            } else {
                log.debug("Condition not met for next call: {}", nextCall.getTargetDataSource());
            }
        });
    }

    /**
     * Evaluate condition for chaining
     */
    private boolean evaluateCondition(Condition condition, ExtractionContext context) {
        if (condition == null) {
            return true;
        }

        Object fieldValue = context.getVariables().get(condition.getField());

        return switch (condition.getOperator().toLowerCase()) {
            case "notnull" -> fieldValue != null;
            case "equals" -> Objects.equals(fieldValue, condition.getValue());
            case "greaterthan" -> compareValues(fieldValue, condition.getValue()) > 0;
            case "lessthan" -> compareValues(fieldValue, condition.getValue()) < 0;
            case "in" -> {
                if (condition.getValue() instanceof List) {
                    yield ((List<?>) condition.getValue()).contains(fieldValue);
                }
                yield false;
            }
            default -> {
                log.warn("Unknown operator: {}", condition.getOperator());
                yield false;
            }
        };
    }

    /**
     * Compare two values for ordering
     */
    private int compareValues(Object value1, Object value2) {
        if (value1 == null || value2 == null) {
            return 0;
        }

        if (value1 instanceof Number && value2 instanceof Number) {
            double d1 = ((Number) value1).doubleValue();
            double d2 = ((Number) value2).doubleValue();
            return Double.compare(d1, d2);
        }

        if (value1 instanceof Comparable) {
            try {
                return ((Comparable) value1).compareTo(value2);
            } catch (ClassCastException e) {
                log.warn("Cannot compare {} and {}", value1.getClass(), value2.getClass());
                return 0;
            }
        }

        return 0;
    }
}
