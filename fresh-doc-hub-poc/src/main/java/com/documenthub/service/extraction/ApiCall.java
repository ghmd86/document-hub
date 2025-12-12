package com.documenthub.service.extraction;

import com.documenthub.model.extraction.DataSourceConfig;
import com.documenthub.model.extraction.FieldSourceConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Represents a single API call within an extraction plan.
 *
 * <p><b>What:</b> An immutable data class that holds all information needed
 * to execute an API call and extract fields from its response.</p>
 *
 * <p><b>Why:</b> Encapsulates the relationship between:
 * <ul>
 *   <li>The API identifier (for logging and debugging)</li>
 *   <li>The endpoint configuration (URL, method, headers, timeout)</li>
 *   <li>The field extraction rules (JSONPath expressions, defaults)</li>
 * </ul>
 * This grouping makes it easy to pass all required information to the
 * API executor and field extractor.</p>
 *
 * <p><b>How:</b> Created by {@link ExtractionPlan#addApiCall} during plan
 * building and consumed by {@link ApiCallExecutor} during execution.</p>
 *
 * <p><b>Example:</b>
 * <pre>
 * ApiCall call = new ApiCall(
 *     "account-api",
 *     dataSourceConfig,  // Contains endpoint: GET /api/accounts/${accountId}
 *     fieldSources       // Contains: disclosureCode -> $.disclosure.code
 * );
 * </pre>
 * </p>
 *
 * @see ExtractionPlan
 * @see ApiCallExecutor
 * @see FieldExtractor
 */
@Getter
@RequiredArgsConstructor
public class ApiCall {

    /**
     * Unique identifier for this API.
     *
     * <p><b>What:</b> A human-readable name for the API (e.g., "account-api",
     * "customer-profile-api").</p>
     *
     * <p><b>Why:</b> Used for logging, debugging, and tracking which APIs
     * have been called. Also used to prevent duplicate calls to the same API.</p>
     */
    private final String apiId;

    /**
     * Configuration for the API endpoint.
     *
     * <p><b>What:</b> Contains all HTTP request details: URL, method,
     * headers, body template, and timeout.</p>
     *
     * <p><b>Why:</b> The executor needs this to build and send the HTTP request.
     * Also contains the list of fields this API provides.</p>
     *
     * @see DataSourceConfig
     */
    private final DataSourceConfig dataSource;

    /**
     * Map of field names to their extraction configurations.
     *
     * <p><b>What:</b> For each field this API provides, defines how to
     * extract the value from the response.</p>
     *
     * <p><b>Why:</b> After the API responds, the field extractor uses these
     * configurations to pull specific values from the JSON response using
     * JSONPath expressions.</p>
     *
     * <p><b>Note:</b> This map contains configurations for ALL fields in the
     * template, not just the ones this API provides. The extractor filters
     * to only the relevant fields using {@code dataSource.getProvidesFields()}.</p>
     *
     * @see FieldSourceConfig
     */
    private final Map<String, FieldSourceConfig> fieldSources;
}
