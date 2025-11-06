package com.documenthub.rules.engine;

import com.documenthub.rules.evaluator.RuleEvaluator;
import com.documenthub.rules.model.ExtractionRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom rule engine that orchestrates data extraction and rule evaluation.
 * Executes extraction strategies defined in JSON and evaluates eligibility criteria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomRuleEngine {

    private final WebClient webClient;
    private final RuleEvaluator ruleEvaluator;
    private final ObjectMapper objectMapper;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Evaluate a custom rule defined in JSON format.
     *
     * @param ruleJson JSON string containing the extraction rule
     * @param customerId Customer ID for context
     * @param accountId Account ID for context
     * @return Mono<Boolean> indicating if the rule passes
     */
    public Mono<Boolean> evaluate(String ruleJson, UUID customerId, UUID accountId) {
        try {
            ExtractionRule rule = objectMapper.readValue(ruleJson, ExtractionRule.class);
            return evaluate(rule, customerId, accountId);
        } catch (Exception e) {
            log.error("Error parsing extraction rule JSON: {}", e.getMessage(), e);
            return Mono.just(false);
        }
    }

    /**
     * Evaluate an extraction rule object.
     */
    public Mono<Boolean> evaluate(ExtractionRule rule, UUID customerId, UUID accountId) {
        log.debug("Evaluating extraction rule of type: {}", rule.getRuleType());

        // Handle composite rules (AND/OR logic)
        if ("composite".equalsIgnoreCase(rule.getRuleType())) {
            return evaluateCompositeRule(rule, customerId, accountId);
        }

        // Execute extraction strategy
        return executeExtractionStrategy(rule, customerId, accountId)
                .flatMap(extractedData -> {
                    // Evaluate eligibility criteria
                    boolean result = ruleEvaluator.evaluateAll(rule.getEligibilityCriteria(), extractedData);
                    log.debug("Rule evaluation result: {}", result);
                    return Mono.just(result);
                })
                .onErrorResume(error -> {
                    log.error("Error during rule evaluation: {}", error.getMessage(), error);
                    return handleError(rule.getErrorHandling());
                });
    }

    /**
     * Evaluate composite rule with AND/OR logic.
     */
    private Mono<Boolean> evaluateCompositeRule(ExtractionRule compositeRule, UUID customerId, UUID accountId) {
        String logicOperator = compositeRule.getLogicOperator();

        if (compositeRule.getRules() == null || compositeRule.getRules().isEmpty()) {
            log.warn("Composite rule has no sub-rules");
            return Mono.just(false);
        }

        if ("OR".equalsIgnoreCase(logicOperator)) {
            // OR logic: at least one rule must pass
            return Flux.fromIterable(compositeRule.getRules())
                    .flatMap(subRule -> evaluate(subRule, customerId, accountId))
                    .any(result -> result)
                    .defaultIfEmpty(false);
        } else {
            // AND logic (default): all rules must pass
            return Flux.fromIterable(compositeRule.getRules())
                    .flatMap(subRule -> evaluate(subRule, customerId, accountId))
                    .all(result -> result);
        }
    }

    /**
     * Execute extraction strategy by calling APIs and extracting data.
     */
    private Mono<Map<String, Object>> executeExtractionStrategy(ExtractionRule rule, UUID customerId, UUID accountId) {
        if (rule.getExtractionStrategy() == null || rule.getExtractionStrategy().isEmpty()) {
            log.warn("No extraction strategy defined");
            return Mono.just(new HashMap<>());
        }

        Map<String, Object> context = createContext(customerId, accountId);

        // Execute data sources sequentially (could be parallelized for independent sources)
        return Flux.fromIterable(rule.getExtractionStrategy())
                .flatMap(dataSource -> executeDataSource(dataSource, context))
                .reduce(new HashMap<String, Object>(), (acc, extractedData) -> {
                    acc.putAll(extractedData);
                    return acc;
                });
    }

    /**
     * Execute a single data source (API call).
     */
    private Mono<Map<String, Object>> executeDataSource(ExtractionRule.DataSource dataSource, Map<String, Object> context) {
        log.debug("Executing data source: {}", dataSource.getId());

        try {
            String url = resolveUrl(dataSource.getEndpoint().getUrl(), context);
            int timeout = dataSource.getEndpoint().getTimeout() != null ?
                    dataSource.getEndpoint().getTimeout() : 3000;

            return webClient
                    .method(org.springframework.http.HttpMethod.valueOf(dataSource.getEndpoint().getMethod()))
                    .uri(url)
                    .headers(headers -> {
                        if (dataSource.getEndpoint().getHeaders() != null) {
                            dataSource.getEndpoint().getHeaders().forEach((key, value) -> {
                                String resolvedValue = resolvePlaceholders(value, context);
                                headers.add(key, resolvedValue);
                            });
                        }
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .map(responseBody -> extractDataFromResponse(responseBody, dataSource.getResponseMapping()))
                    .doOnSuccess(data -> log.debug("Successfully executed data source: {}", dataSource.getId()))
                    .doOnError(error -> log.error("Error executing data source {}: {}", dataSource.getId(), error.getMessage()))
                    .onErrorResume(error -> Mono.just(new HashMap<>()));
        } catch (Exception e) {
            log.error("Error preparing data source execution: {}", e.getMessage(), e);
            return Mono.just(new HashMap<>());
        }
    }

    /**
     * Extract data from API response using JSONPath expressions.
     */
    private Map<String, Object> extractDataFromResponse(String responseBody, ExtractionRule.ResponseMapping mapping) {
        Map<String, Object> extractedData = new HashMap<>();

        if (mapping == null || mapping.getExtract() == null) {
            return extractedData;
        }

        for (Map.Entry<String, String> entry : mapping.getExtract().entrySet()) {
            String fieldName = entry.getKey();
            String jsonPath = entry.getValue();

            try {
                Object value = JsonPath.read(responseBody, jsonPath);
                extractedData.put(fieldName, value);
                log.debug("Extracted field {}: {}", fieldName, value);
            } catch (Exception e) {
                log.warn("Error extracting field {} with JSONPath {}: {}", fieldName, jsonPath, e.getMessage());
            }
        }

        return extractedData;
    }

    /**
     * Create context map with customer and account IDs.
     */
    private Map<String, Object> createContext(UUID customerId, UUID accountId) {
        Map<String, Object> context = new HashMap<>();
        context.put("$input.customerId", customerId.toString());
        context.put("$input.accountId", accountId.toString());
        context.put("customerId", customerId.toString());
        context.put("accountId", accountId.toString());
        return context;
    }

    /**
     * Resolve URL with placeholders.
     */
    private String resolveUrl(String url, Map<String, Object> context) {
        return resolvePlaceholders(url, context);
    }

    /**
     * Resolve placeholders in a string using context.
     */
    private String resolvePlaceholders(String template, Map<String, Object> context) {
        if (template == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = context.get(placeholder);
            if (value != null) {
                matcher.appendReplacement(result, String.valueOf(value));
            } else {
                log.warn("Placeholder {} not found in context", placeholder);
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Handle errors based on error handling configuration.
     */
    private Mono<Boolean> handleError(ExtractionRule.ErrorHandlingConfig errorHandling) {
        if (errorHandling == null) {
            return Mono.just(false);
        }

        String action = errorHandling.getOnExtractionFailure();
        if ("include".equalsIgnoreCase(action)) {
            log.debug("Error handling: including document on error");
            return Mono.just(true);
        } else {
            log.debug("Error handling: excluding document on error");
            return Mono.just(false);
        }
    }
}
