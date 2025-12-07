package com.documenthub.service;

import com.documenthub.model.DocumentListRequest;
import com.documenthub.model.extraction.DataExtractionConfig;
import com.documenthub.model.extraction.DataSourceConfig;
import com.documenthub.model.extraction.EndpointConfig;
import com.documenthub.model.extraction.FieldSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configurable Data Extraction Service
 * Extracts data from APIs based on JSON configuration stored in database
 * Solves the problem: "Client sends accountId/customerId, but where do we get disclosureCode, customerLocation, etc.?"
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigurableDataExtractionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Extract required fields based on data_extraction_config JSON
     *
     * @param dataExtractionConfigJson JSON from master_template_definition.data_extraction_config
     * @param request Original request with accountId, customerId
     * @return Map of extracted field values
     */
    public Mono<Map<String, Object>> extractData(
        Json dataExtractionConfigJson,
        DocumentListRequest request
    ) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("STEP 1/6: Starting ConfigurableDataExtractionService.extractData");
        log.info("═══════════════════════════════════════════════════════════════");

        if (dataExtractionConfigJson == null) {
            log.warn("STEP 1/6: No data extraction config provided - returning empty map");
            return Mono.just(Collections.emptyMap());
        }

        try {
            log.info("STEP 2/6: Converting PostgreSQL Json to JsonNode");
            // Convert PostgreSQL Json to JsonNode
            JsonNode configNode = objectMapper.readTree(dataExtractionConfigJson.asString());
            log.debug("STEP 2/6: Json conversion successful");

            log.info("STEP 3/6: Parsing data extraction config to Java objects");
            // Parse JSON config to Java objects
            DataExtractionConfig config = objectMapper.treeToValue(
                configNode,
                DataExtractionConfig.class
            );
            log.info("STEP 3/6: Config parsed - Fields to extract: {}, Data sources: {}",
                config.getFieldsToExtract() != null ? config.getFieldsToExtract().size() : 0,
                config.getDataSources() != null ? config.getDataSources().size() : 0);

            log.info("STEP 4/6: Creating initial context from request");
            // Create initial context with input data
            Map<String, Object> context = createInitialContext(request);
            log.info("STEP 4/6: Initial context created with {} fields: {}",
                context.size(), context.keySet());

            log.info("STEP 5/6: Building execution plan");
            // Build execution plan
            ExtractionPlan plan = buildExtractionPlan(config, context);

            if (plan.getApiCalls().isEmpty()) {
                log.warn("STEP 5/6: No API calls to execute - returning context as-is");
                return Mono.just(context);
            }

            log.info("STEP 5/6: Execution plan built - {} API call(s) scheduled", plan.getApiCalls().size());
            for (int i = 0; i < plan.getApiCalls().size(); i++) {
                ApiCall apiCall = plan.getApiCalls().get(i);
                log.info("  → API Call {}/{}: {} (provides {} fields)",
                    i + 1,
                    plan.getApiCalls().size(),
                    apiCall.getApiId(),
                    apiCall.getDataSource().getProvidesFields().size());
            }

            log.info("STEP 6/6: Executing extraction plan");
            // Execute API calls
            return executeExtractionPlan(plan, config, context)
                .doOnSuccess(result -> {
                    log.info("═══════════════════════════════════════════════════════════════");
                    log.info("STEP 6/6: ✓ Data extraction completed successfully");
                    log.info("         Total fields extracted: {}", result.size());
                    log.info("         Fields: {}", result.keySet());
                    log.info("═══════════════════════════════════════════════════════════════");
                })
                .doOnError(error -> {
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("STEP 6/6: ✗ Data extraction failed");
                    log.error("         Error: {}", error.getMessage());
                    log.error("═══════════════════════════════════════════════════════════════");
                });

        } catch (Exception e) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("STEP 2-3/6: ✗ Failed to parse data extraction config");
            log.error("           Error: {}", e.getMessage());
            log.error("═══════════════════════════════════════════════════════════════", e);
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
        Set<String> fieldsToExtract = new HashSet<>(config.getFieldsToExtract());
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
        log.info("  → Execution mode: SEQUENTIAL");
        final int[] callCount = {0};
        final int totalCalls = plan.getApiCalls().size();

        return Flux.fromIterable(plan.getApiCalls())
            .concatMap(apiCall -> {
                callCount[0]++;
                log.info("  → Executing API call {}/{}: {}", callCount[0], totalCalls, apiCall.getApiId());
                return callApi(apiCall, context)
                    .doOnSuccess(extractedData -> {
                        context.putAll(extractedData);
                        log.info("    ✓ API {}/{} completed: {} - Extracted {} fields: {}",
                            callCount[0], totalCalls, apiCall.getApiId(),
                            extractedData.size(), extractedData.keySet());
                    })
                    .onErrorResume(e -> {
                        log.error("    ✗ API {}/{} failed: {} - Error: {}",
                            callCount[0], totalCalls, apiCall.getApiId(), e.getMessage());
                        return Mono.just(Collections.emptyMap());
                    });
            })
            .then(Mono.just(context));
    }

    /**
     * Execute API calls in parallel
     */
    private Mono<Map<String, Object>> executeParallel(
        ExtractionPlan plan,
        Map<String, Object> context
    ) {
        log.info("  → Execution mode: PARALLEL");
        final int totalCalls = plan.getApiCalls().size();

        return Flux.fromIterable(plan.getApiCalls())
            .flatMap(apiCall -> {
                log.info("  → Initiating parallel API call: {}", apiCall.getApiId());
                return callApi(apiCall, context)
                    .doOnSuccess(extractedData -> {
                        log.info("    ✓ Parallel API completed: {} - Extracted {} fields: {}",
                            apiCall.getApiId(), extractedData.size(), extractedData.keySet());
                    })
                    .onErrorResume(e -> {
                        log.error("    ✗ Parallel API failed: {} - Error: {}",
                            apiCall.getApiId(), e.getMessage());
                        return Mono.just(Collections.emptyMap());
                    });
            })
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

        log.debug("    → Preparing API call: {}", apiCall.getApiId());

        // Resolve URL with placeholders
        String url = resolvePlaceholders(endpoint.getUrl(), context);

        if (url.contains("${")) {
            log.warn("    ✗ Could not resolve all placeholders in URL: {}", url);
            return Mono.just(Collections.emptyMap());
        }

        log.info("    → Calling {} {}", endpoint.getMethod(), url);

        // Determine timeout
        int timeout = endpoint.getTimeout() != null ? endpoint.getTimeout() : 5000;
        log.debug("    → Timeout: {}ms", timeout);

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
        long startTime = System.currentTimeMillis();
        return requestSpec
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(timeout))
            .map(responseBody -> {
                log.debug("    → Received response from {}, extracting fields...", apiCall.getApiId());
                return extractFields(responseBody, apiCall, context);
            })
            .doOnSuccess(extractedData -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("    → API response processed in {}ms - Extracted fields: {}",
                    duration, extractedData.keySet());
            })
            .onErrorResume(e -> {
                long duration = System.currentTimeMillis() - startTime;
                log.error("    ✗ API call failed for {} after {}ms: {} - Using default values",
                    apiCall.getApiId(), duration, e.getMessage());
                Map<String, Object> defaults = useDefaultValues(apiCall);
                log.debug("    → Applied {} default value(s)", defaults.size());
                return Mono.just(defaults);
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
        int totalFields = apiCall.getDataSource().getProvidesFields().size();
        int successCount = 0;
        int defaultCount = 0;
        int failedCount = 0;

        log.debug("      → Extracting {} field(s) from response", totalFields);

        for (String fieldName : apiCall.getDataSource().getProvidesFields()) {
            FieldSourceConfig fieldSource = apiCall.getFieldSources().get(fieldName);

            if (fieldSource == null || fieldSource.getExtractionPath() == null) {
                log.debug("      ⊘ Skipping {} - No extraction config", fieldName);
                continue;
            }

            try {
                Object value = JsonPath.read(responseBody, fieldSource.getExtractionPath());

                // Unwrap single-element arrays to extract the first value
                // This handles cases where JSONPath filters return arrays like ["PRC-12345"]
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (list.size() == 1) {
                        value = list.get(0);
                        log.debug("      ⤷ Unwrapped single-element array to: {}", value);
                    }
                }

                if (value != null) {
                    extracted.put(fieldName, value);
                    successCount++;
                    log.debug("      ✓ Extracted {}: {} (path: {})",
                        fieldName, value, fieldSource.getExtractionPath());
                } else if (fieldSource.getDefaultValue() != null) {
                    extracted.put(fieldName, fieldSource.getDefaultValue());
                    defaultCount++;
                    log.debug("      ◉ Using default for {}: {} (value was null)",
                        fieldName, fieldSource.getDefaultValue());
                }
            } catch (Exception e) {
                failedCount++;
                log.warn("      ✗ Failed to extract {} using JSONPath '{}': {}",
                    fieldName, fieldSource.getExtractionPath(), e.getMessage());

                if (fieldSource.getDefaultValue() != null) {
                    extracted.put(fieldName, fieldSource.getDefaultValue());
                    defaultCount++;
                    log.debug("      ◉ Applied default for {}: {}", fieldName, fieldSource.getDefaultValue());
                }
            }
        }

        log.info("      → Field extraction summary: {} extracted, {} defaults, {} failed",
            successCount, defaultCount, failedCount);

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
