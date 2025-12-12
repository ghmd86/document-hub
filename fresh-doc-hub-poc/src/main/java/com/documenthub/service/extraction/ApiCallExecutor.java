package com.documenthub.service.extraction;

import com.documenthub.model.extraction.EndpointConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes API calls defined in an extraction plan.
 *
 * <p><b>What:</b> Handles the actual HTTP calls to external APIs and coordinates
 * field extraction from the responses.</p>
 *
 * <p><b>Why:</b> Separating API execution from plan building and field extraction
 * follows the Single Responsibility Principle. This class focuses solely on
 * making HTTP requests reliably with proper error handling.</p>
 *
 * <p><b>How:</b> Uses Spring WebClient for non-blocking HTTP calls. Supports both
 * sequential and parallel execution modes:
 * <ul>
 *   <li><b>Sequential:</b> Each API call waits for the previous one to complete.
 *       Use when later calls depend on data from earlier calls.</li>
 *   <li><b>Parallel:</b> All API calls execute concurrently.
 *       Use when calls are independent for better performance.</li>
 * </ul>
 * </p>
 *
 * @see ExtractionPlan
 * @see FieldExtractor
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiCallExecutor {

    private final WebClient webClient;
    private final FieldExtractor fieldExtractor;

    /**
     * Pattern for matching placeholders like ${fieldName} in URLs and request bodies.
     * These placeholders are replaced with actual values from the context map.
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Default timeout for API calls in milliseconds.
     * Used when no timeout is specified in the endpoint configuration.
     */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /**
     * Executes all API calls in the plan sequentially.
     *
     * <p><b>What:</b> Processes API calls one at a time, waiting for each to complete
     * before starting the next.</p>
     *
     * <p><b>Why:</b> Sequential execution is needed when API calls have dependencies.
     * For example, if API-B needs a value extracted from API-A's response.</p>
     *
     * <p><b>How:</b> Uses Flux.concatMap to ensure ordering. After each call completes,
     * the extracted data is added to the context, making it available for subsequent calls.</p>
     *
     * @param plan The extraction plan containing the ordered list of API calls
     * @param context The context map with initial values and placeholder data
     * @return Mono containing the updated context with all extracted fields
     */
    public Mono<Map<String, Object>> executeSequential(
            ExtractionPlan plan,
            Map<String, Object> context) {

        log.info("Execution mode: SEQUENTIAL");

        return Flux.fromIterable(plan.getApiCalls())
                .concatMap(apiCall -> executeApiCall(apiCall, context))
                .then(Mono.just(context));
    }

    /**
     * Executes all API calls in the plan in parallel.
     *
     * <p><b>What:</b> Initiates all API calls simultaneously without waiting.</p>
     *
     * <p><b>Why:</b> Parallel execution is faster when API calls are independent.
     * If we need data from 3 different APIs that don't depend on each other,
     * parallel execution reduces total time from sum(timeouts) to max(timeouts).</p>
     *
     * <p><b>How:</b> Uses Flux.flatMap which subscribes to all inner publishers
     * immediately. Results are collected and merged into the context map.</p>
     *
     * @param plan The extraction plan containing the API calls
     * @param context The context map with initial values
     * @return Mono containing the updated context with all extracted fields
     */
    public Mono<Map<String, Object>> executeParallel(
            ExtractionPlan plan,
            Map<String, Object> context) {

        log.info("Execution mode: PARALLEL ({} calls)", plan.size());

        return Flux.fromIterable(plan.getApiCalls())
                .flatMap(apiCall -> callApiSafe(apiCall, context))
                .reduce(context, (acc, data) -> {
                    acc.putAll(data);
                    return acc;
                });
    }

    /**
     * Executes a single API call and updates the context with extracted data.
     *
     * <p><b>What:</b> Wrapper that calls the API and merges results into context.</p>
     *
     * <p><b>Why:</b> In sequential mode, we need to update the shared context
     * after each call so subsequent calls can use the extracted values.</p>
     *
     * @param apiCall The API call to execute
     * @param context The context map to update
     * @return Mono containing the extracted data
     */
    private Mono<Map<String, Object>> executeApiCall(
            ApiCall apiCall,
            Map<String, Object> context) {

        return callApiSafe(apiCall, context)
                .doOnSuccess(data -> context.putAll(data));
    }

    /**
     * Calls an API with error handling that returns defaults on failure.
     *
     * <p><b>What:</b> Wraps the API call with error recovery.</p>
     *
     * <p><b>Why:</b> API failures shouldn't break the entire extraction process.
     * If an API is down, we use configured default values and continue.</p>
     *
     * <p><b>How:</b> Uses onErrorResume to catch any exception and return
     * default values instead of propagating the error.</p>
     *
     * @param apiCall The API call to execute
     * @param context The context map for placeholder resolution
     * @return Mono containing extracted data or defaults on error
     */
    private Mono<Map<String, Object>> callApiSafe(
            ApiCall apiCall,
            Map<String, Object> context) {

        return callApi(apiCall, context)
                .onErrorResume(e -> handleApiError(apiCall, e));
    }

    /**
     * Makes the actual HTTP call to the API endpoint.
     *
     * <p><b>What:</b> Constructs and executes the HTTP request, then extracts fields.</p>
     *
     * <p><b>Why:</b> This is the core method that translates the endpoint configuration
     * into an actual HTTP request.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Resolves placeholders in the URL using context values</li>
     *   <li>Checks for unresolved placeholders (indicates missing data)</li>
     *   <li>Builds the request with headers and optional body</li>
     *   <li>Executes with configured timeout</li>
     *   <li>Passes response to FieldExtractor for data extraction</li>
     * </ol>
     * </p>
     *
     * @param apiCall The API call configuration
     * @param context The context map for placeholder resolution
     * @return Mono containing the extracted fields
     */
    private Mono<Map<String, Object>> callApi(
            ApiCall apiCall,
            Map<String, Object> context) {

        EndpointConfig endpoint = apiCall.getDataSource().getEndpoint();
        String url = resolvePlaceholders(endpoint.getUrl(), context);

        if (hasUnresolvedPlaceholders(url)) {
            log.warn("Unresolved placeholders in URL: {}", url);
            return Mono.just(Collections.emptyMap());
        }

        log.info("Calling {} {}", endpoint.getMethod(), url);

        return buildRequest(endpoint, url, context)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(getTimeout(endpoint)))
                .map(body -> fieldExtractor.extractFields(body, apiCall));
    }

    /**
     * Builds a WebClient request from the endpoint configuration.
     *
     * <p><b>What:</b> Creates a configured HTTP request ready for execution.</p>
     *
     * <p><b>Why:</b> Endpoint configs define method, URL, headers, and body.
     * This method translates that config into a WebClient request.</p>
     *
     * <p><b>How:</b> Creates the base request with method and URL, then adds
     * headers and body if configured.</p>
     *
     * @param endpoint The endpoint configuration
     * @param url The resolved URL (with placeholders filled in)
     * @param context The context map for header/body placeholder resolution
     * @return A configured WebClient request spec
     */
    private WebClient.RequestHeadersSpec<?> buildRequest(
            EndpointConfig endpoint,
            String url,
            Map<String, Object> context) {

        WebClient.RequestBodySpec request = webClient
                .method(HttpMethod.valueOf(endpoint.getMethod()))
                .uri(url);

        addHeaders(request, endpoint, context);

        return addBody(request, endpoint, context);
    }

    /**
     * Adds headers to the request from endpoint configuration.
     *
     * <p><b>What:</b> Processes and adds HTTP headers to the request.</p>
     *
     * <p><b>Why:</b> APIs often require headers like Authorization, Content-Type,
     * or custom correlation IDs. Header values may contain placeholders.</p>
     *
     * <p><b>How:</b> Iterates through configured headers, resolves any placeholders
     * in values, and adds them to the request.</p>
     *
     * @param request The WebClient request to add headers to
     * @param endpoint The endpoint configuration containing headers
     * @param context The context map for placeholder resolution
     */
    private void addHeaders(
            WebClient.RequestBodySpec request,
            EndpointConfig endpoint,
            Map<String, Object> context) {

        if (endpoint.getHeaders() == null) {
            return;
        }

        endpoint.getHeaders().forEach((key, value) -> {
            String resolved = resolvePlaceholders(value, context);
            request.header(key, resolved);
        });
    }

    /**
     * Adds a request body for POST/PUT requests.
     *
     * <p><b>What:</b> Attaches a body to the request if needed.</p>
     *
     * <p><b>Why:</b> POST and PUT requests typically require a request body.
     * The body template may contain placeholders that need resolution.</p>
     *
     * <p><b>How:</b> Checks if the method requires a body, resolves placeholders
     * in the body template, and attaches it to the request.</p>
     *
     * @param request The WebClient request to add body to
     * @param endpoint The endpoint configuration containing the body template
     * @param context The context map for placeholder resolution
     * @return The request with body added (or unchanged if no body needed)
     */
    private WebClient.RequestHeadersSpec<?> addBody(
            WebClient.RequestBodySpec request,
            EndpointConfig endpoint,
            Map<String, Object> context) {

        if (!needsBody(endpoint)) {
            return request;
        }

        String body = resolvePlaceholders(endpoint.getBody(), context);
        return request.bodyValue(body);
    }

    /**
     * Determines if the request needs a body based on HTTP method.
     *
     * <p><b>Why:</b> Only POST and PUT methods typically have request bodies.
     * GET and DELETE requests don't need bodies.</p>
     *
     * @param endpoint The endpoint configuration
     * @return true if the request should have a body
     */
    private boolean needsBody(EndpointConfig endpoint) {
        if (endpoint.getBody() == null) {
            return false;
        }

        String method = endpoint.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    }

    /**
     * Handles API call errors by returning default values.
     *
     * <p><b>What:</b> Error recovery that provides fallback values.</p>
     *
     * <p><b>Why:</b> When an API fails (timeout, 500 error, etc.), we don't want
     * to fail the entire document retrieval. Default values allow graceful degradation.</p>
     *
     * <p><b>How:</b> Logs the error and delegates to FieldExtractor to get
     * configured default values for the fields this API was supposed to provide.</p>
     *
     * @param apiCall The failed API call
     * @param e The exception that occurred
     * @return Mono containing default values for the API's fields
     */
    private Mono<Map<String, Object>> handleApiError(ApiCall apiCall, Throwable e) {
        log.error("API call failed for {}: {}", apiCall.getApiId(), e.getMessage());
        Map<String, Object> defaults = fieldExtractor.getDefaultValues(apiCall);
        return Mono.just(defaults);
    }

    /**
     * Resolves placeholders in a template string using context values.
     *
     * <p><b>What:</b> Replaces ${fieldName} patterns with actual values.</p>
     *
     * <p><b>Why:</b> URLs and request bodies often contain placeholders like
     * ${accountId} that need to be replaced with actual request values.</p>
     *
     * <p><b>How:</b> Uses regex to find all ${...} patterns and replaces them
     * with corresponding values from the context map.</p>
     *
     * <p><b>Example:</b>
     * <pre>
     * Template: "/api/accounts/${accountId}/details"
     * Context: {accountId: "12345"}
     * Result: "/api/accounts/12345/details"
     * </pre>
     * </p>
     *
     * @param template The string containing placeholders
     * @param context The map of values to substitute
     * @return The template with placeholders replaced
     */
    private String resolvePlaceholders(String template, Map<String, Object> context) {
        if (template == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context.get(key);

            if (value != null) {
                matcher.appendReplacement(result, value.toString());
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Checks if a URL still contains unresolved placeholders.
     *
     * <p><b>Why:</b> If placeholders remain after resolution, it means required
     * data is missing. Making an API call with unresolved placeholders would fail.</p>
     *
     * @param url The URL to check
     * @return true if unresolved placeholders exist
     */
    private boolean hasUnresolvedPlaceholders(String url) {
        return url != null && url.contains("${");
    }

    /**
     * Gets the timeout for an API call.
     *
     * <p><b>Why:</b> Different APIs may need different timeouts. Slow APIs get
     * longer timeouts, fast APIs get shorter ones for fail-fast behavior.</p>
     *
     * @param endpoint The endpoint configuration
     * @return Timeout in milliseconds
     */
    private int getTimeout(EndpointConfig endpoint) {
        return endpoint.getTimeout() != null ? endpoint.getTimeout() : DEFAULT_TIMEOUT_MS;
    }
}
