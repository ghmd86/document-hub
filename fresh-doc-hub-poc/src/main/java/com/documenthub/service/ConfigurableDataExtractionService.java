package com.documenthub.service;

import com.documenthub.model.DocumentListRequest;
import com.documenthub.model.extraction.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configurable Data Extraction Service
 * Extracts data from APIs based on JSON configuration stored in database
 * Solves the problem: "Client sends accountId/customerId, but where do we get disclosureCode, customerLocation, etc.?"
 */
@Service
@Slf4j
public class ConfigurableDataExtractionService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Extract required fields based on data_extraction_config JSON
     *
     * @param dataExtractionConfigJson JSON from master_template_definition.data_extraction_config
     * @param request Original request with accountId, customerId
     * @return Map of extracted field values
     */
    public Mono<Map<String, Object>> extractData(
        JsonNode dataExtractionConfigJson,
        DocumentListRequest request
    ) {
        if (dataExtractionConfigJson == null || dataExtractionConfigJson.isNull()) {
            log.debug("No data extraction config provided");
            return Mono.just(Collections.emptyMap());
        }

        try {
            // Parse JSON config to Java objects
            DataExtractionConfig config = objectMapper.treeToValue(
                dataExtractionConfigJson,
                DataExtractionConfig.class
            );

            log.info("Starting data extraction for {} required fields",
                config.getRequiredFields() != null ? config.getRequiredFields().size() : 0);

            // Create initial context with input data
            Map<String, Object> context = createInitialContext(request);

            // Build execution plan
            ExtractionPlan plan = buildExtractionPlan(config, context);

            if (plan.getApiCalls().isEmpty()) {
                log.warn("No API calls to execute");
                return Mono.just(context);
            }

            log.info("Execution plan: {} API calls", plan.getApiCalls().size());

            // Execute API calls
            return executeExtractionPlan(plan, config, context)
                .doOnSuccess(result -> log.info("Data extraction completed. Extracted {} fields",
                    result.size()));

        } catch (Exception e) {
            log.error("Failed to parse data extraction config", e);
            return Mono.error(e);
        }
    }

    /**
     * Create initial context with input data from request
     */
    private Map<String, Object> createInitialContext(DocumentListRequest request) {
        Map<String, Object> context = new HashMap<>();

        if (request.getAccountId() != null && !request.getAccountId().isEmpty()) {
            context.put("accountId", request.getAccountId().get(0));
        }
        if (request.getCustomerId() != null) {
            context.put("customerId", request.getCustomerId().toString());
        }
        if (request.getReferenceKey() != null) {
            context.put("referenceKey", request.getReferenceKey());
        }
        if (request.getReferenceKeyType() != null) {
            context.put("referenceKeyType", request.getReferenceKeyType());
        }

        // Add system variables
        context.put("correlationId", UUID.randomUUID().toString());
        context.put("auth.token", "mock-token-12345"); // TODO: Get from security context

        log.debug("Initial context: {}", context.keySet());
        return context;
    }

    /**
     * Build execution plan: determine which APIs to call and in what order
     */
    private ExtractionPlan buildExtractionPlan(
        DataExtractionConfig config,
        Map<String, Object> availableContext
    ) {
        ExtractionPlan plan = new ExtractionPlan();
        Set<String> fieldsToExtract = new HashSet<>(config.getRequiredFields());
        Set<String> availableFields = new HashSet<>(availableContext.keySet());
        Set<String> processedApis = new HashSet<>();

        log.debug("Planning extraction for fields: {}", fieldsToExtract);
        log.debug("Available fields: {}", availableFields);

        // Iteratively add API calls until all fields are covered
        int iterations = 0;
        int maxIterations = 10;

        while (!fieldsToExtract.isEmpty() && iterations < maxIterations) {
            iterations++;
            boolean progressMade = false;

            for (String fieldName : new ArrayList<>(fieldsToExtract)) {
                FieldSourceConfig fieldSource = config.getFieldSources().get(fieldName);

                if (fieldSource == null) {
                    log.warn("No source configuration for field: {}", fieldName);
                    fieldsToExtract.remove(fieldName);
                    continue;
                }

                // Check if we have all required inputs
                if (hasRequiredInputs(fieldSource, availableFields)) {
                    String apiId = fieldSource.getSourceApi();

                    if (!processedApis.contains(apiId)) {
                        DataSourceConfig dataSource = config.getDataSources().get(apiId);

                        if (dataSource != null) {
                            plan.addApiCall(apiId, dataSource, config.getFieldSources());
                            processedApis.add(apiId);

                            // Mark fields from this API as available
                            availableFields.addAll(dataSource.getProvidesFields());

                            log.debug("Added API '{}' to plan (provides: {})",
                                apiId, dataSource.getProvidesFields());

                            progressMade = true;
                        }
                    }

                    fieldsToExtract.remove(fieldName);
                    progressMade = true;
                }
            }

            if (!progressMade) {
                log.warn("Cannot make progress. Remaining fields: {}", fieldsToExtract);
                break;
            }
        }

        if (!fieldsToExtract.isEmpty()) {
            log.warn("Could not plan extraction for fields: {}", fieldsToExtract);
        }

        return plan;
    }

    /**
     * Check if all required inputs are available
     */
    private boolean hasRequiredInputs(FieldSourceConfig fieldSource, Set<String> availableFields) {
        if (fieldSource.getRequiredInputs() == null || fieldSource.getRequiredInputs().isEmpty()) {
            return true;
        }

        return fieldSource.getRequiredInputs().stream()
            .allMatch(availableFields::contains);
    }

    /**
     * Execute the extraction plan
     */
    private Mono<Map<String, Object>> executeExtractionPlan(
        ExtractionPlan plan,
        DataExtractionConfig config,
        Map<String, Object> context
    ) {
        String executionMode = config.getExecutionStrategy() != null ?
            config.getExecutionStrategy().getMode() : "sequential";

        if ("parallel".equalsIgnoreCase(executionMode)) {
            return executeParallel(plan, context);
        } else {
            return executeSequential(plan, context);
        }
    }

    /**
     * Execute API calls sequentially
     */
    private Mono<Map<String, Object>> executeSequential(
        ExtractionPlan plan,
        Map<String, Object> context
    ) {
        return Flux.fromIterable(plan.getApiCalls())
            .concatMap(apiCall -> callApi(apiCall, context)
                .doOnSuccess(extractedData -> {
                    context.putAll(extractedData);
                    log.debug("API {} completed, extracted {} fields",
                        apiCall.getApiId(), extractedData.size());
                })
                .onErrorResume(e -> {
                    log.error("API {} failed: {}", apiCall.getApiId(), e.getMessage());
                    return Mono.just(Collections.emptyMap());
                })
            )
            .then(Mono.just(context));
    }

    /**
     * Execute API calls in parallel
     */
    private Mono<Map<String, Object>> executeParallel(
        ExtractionPlan plan,
        Map<String, Object> context
    ) {
        return Flux.fromIterable(plan.getApiCalls())
            .flatMap(apiCall -> callApi(apiCall, context)
                .onErrorResume(e -> {
                    log.error("API {} failed: {}", apiCall.getApiId(), e.getMessage());
                    return Mono.just(Collections.emptyMap());
                })
            )
            .reduce(context, (acc, extractedData) -> {
                acc.putAll(extractedData);
                return acc;
            });
    }

    /**
     * Call a single API and extract data
     */
    private Mono<Map<String, Object>> callApi(ApiCall apiCall, Map<String, Object> context) {
        EndpointConfig endpoint = apiCall.getDataSource().getEndpoint();

        // Resolve URL with placeholders
        String url = resolvePlaceholders(endpoint.getUrl(), context);

        if (url.contains("${")) {
            log.warn("Could not resolve all placeholders in URL: {}", url);
            return Mono.just(Collections.emptyMap());
        }

        log.debug("Calling API: {} {}", endpoint.getMethod(), url);

        // Determine timeout
        int timeout = endpoint.getTimeout() != null ? endpoint.getTimeout() : 5000;

        // Build request
        WebClient.RequestBodySpec request = webClient
            .method(HttpMethod.valueOf(endpoint.getMethod()))
            .uri(url);

        // Add headers with placeholder resolution
        if (endpoint.getHeaders() != null) {
            endpoint.getHeaders().forEach((key, value) -> {
                String resolvedValue = resolvePlaceholders(value, context);
                request.header(key, resolvedValue);
            });
        }

        // Add body for POST/PUT requests
        WebClient.RequestHeadersSpec<?> requestSpec;
        if (endpoint.getBody() != null &&
            ("POST".equalsIgnoreCase(endpoint.getMethod()) || "PUT".equalsIgnoreCase(endpoint.getMethod()))) {
            String resolvedBody = resolvePlaceholders(endpoint.getBody(), context);
            log.debug("Request body: {}", resolvedBody);
            requestSpec = request.bodyValue(resolvedBody);
        } else {
            requestSpec = request;
        }

        // Execute request
        return requestSpec
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(timeout))
            .map(responseBody -> extractFields(responseBody, apiCall, context))
            .doOnSuccess(extractedData ->
                log.debug("Extracted fields from {}: {}", apiCall.getApiId(), extractedData.keySet())
            )
            .onErrorResume(e -> {
                log.error("API call failed for {}: {}", apiCall.getApiId(), e.getMessage());
                return Mono.just(useDefaultValues(apiCall));
            });
    }

    /**
     * Extract fields from API response using JSONPath
     */
    private Map<String, Object> extractFields(
        String responseBody,
        ApiCall apiCall,
        Map<String, Object> context
    ) {
        Map<String, Object> extracted = new HashMap<>();

        for (String fieldName : apiCall.getDataSource().getProvidesFields()) {
            FieldSourceConfig fieldSource = apiCall.getFieldSources().get(fieldName);

            if (fieldSource == null || fieldSource.getExtractionPath() == null) {
                continue;
            }

            try {
                Object value = JsonPath.read(responseBody, fieldSource.getExtractionPath());
                if (value != null) {
                    extracted.put(fieldName, value);
                    log.debug("Extracted {}: {}", fieldName, value);
                } else if (fieldSource.getDefaultValue() != null) {
                    extracted.put(fieldName, fieldSource.getDefaultValue());
                    log.debug("Using default value for {}: {}", fieldName, fieldSource.getDefaultValue());
                }
            } catch (Exception e) {
                log.warn("Failed to extract {} using JSONPath {}: {}",
                    fieldName, fieldSource.getExtractionPath(), e.getMessage());

                if (fieldSource.getDefaultValue() != null) {
                    extracted.put(fieldName, fieldSource.getDefaultValue());
                }
            }
        }

        return extracted;
    }

    /**
     * Use default values when API call fails
     */
    private Map<String, Object> useDefaultValues(ApiCall apiCall) {
        Map<String, Object> defaults = new HashMap<>();

        for (String fieldName : apiCall.getDataSource().getProvidesFields()) {
            FieldSourceConfig fieldSource = apiCall.getFieldSources().get(fieldName);

            if (fieldSource != null && fieldSource.getDefaultValue() != null) {
                defaults.put(fieldName, fieldSource.getDefaultValue());
                log.debug("Using default value for {}: {}", fieldName, fieldSource.getDefaultValue());
            }
        }

        return defaults;
    }

    /**
     * Resolve placeholders in string
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
                matcher.appendReplacement(result, value.toString());
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Internal class representing an extraction plan
     */
    private static class ExtractionPlan {
        private final List<ApiCall> apiCalls = new ArrayList<>();

        public void addApiCall(
            String apiId,
            DataSourceConfig dataSource,
            Map<String, FieldSourceConfig> fieldSources
        ) {
            apiCalls.add(new ApiCall(apiId, dataSource, fieldSources));
        }

        public List<ApiCall> getApiCalls() {
            return apiCalls;
        }
    }

    /**
     * Internal class representing a single API call
     */
    private static class ApiCall {
        private final String apiId;
        private final DataSourceConfig dataSource;
        private final Map<String, FieldSourceConfig> fieldSources;

        public ApiCall(String apiId, DataSourceConfig dataSource, Map<String, FieldSourceConfig> fieldSources) {
            this.apiId = apiId;
            this.dataSource = dataSource;
            this.fieldSources = fieldSources;
        }

        public String getApiId() {
            return apiId;
        }

        public DataSourceConfig getDataSource() {
            return dataSource;
        }

        public Map<String, FieldSourceConfig> getFieldSources() {
            return fieldSources;
        }
    }
}
