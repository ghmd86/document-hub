package com.documenthub.service.extraction;

import com.documenthub.model.extraction.FieldSourceConfig;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts fields from API responses using JSONPath expressions.
 *
 * <p><b>What:</b> Parses JSON API responses and extracts specific field values
 * based on configured JSONPath expressions.</p>
 *
 * <p><b>Why:</b> API responses contain nested JSON structures. We need a flexible
 * way to extract specific values without hardcoding the JSON structure. JSONPath
 * provides a query language for JSON similar to XPath for XML.</p>
 *
 * <p><b>How:</b> For each field defined in the API's configuration:
 * <ol>
 *   <li>Looks up the JSONPath expression from FieldSourceConfig</li>
 *   <li>Executes the JSONPath query against the response body</li>
 *   <li>Handles special cases (single-element arrays, nulls)</li>
 *   <li>Falls back to default values when extraction fails</li>
 * </ol>
 * </p>
 *
 * <p><b>JSONPath Examples:</b>
 * <ul>
 *   <li>$.customer.name - Gets the name field from customer object</li>
 *   <li>$.accounts[0].balance - Gets balance from first account</li>
 *   <li>$.items[?(@.type=='ACTIVE')].id - Gets IDs of active items</li>
 * </ul>
 * </p>
 *
 * @see FieldSourceConfig
 * @see ApiCall
 */
@Component
@Slf4j
public class FieldExtractor {

    /**
     * Extracts fields from an API response body.
     *
     * <p><b>What:</b> Main extraction method that processes all fields for an API call.</p>
     *
     * <p><b>Why:</b> After an API call succeeds, we need to extract the specific
     * field values that this API provides (as defined in data_extraction_config).</p>
     *
     * <p><b>How:</b> Iterates through all fields the API provides, attempts to
     * extract each using its configured JSONPath, and tracks statistics for logging.</p>
     *
     * @param responseBody The raw JSON response from the API
     * @param apiCall The API call configuration containing field mappings
     * @return Map of field names to extracted values
     */
    public Map<String, Object> extractFields(String responseBody, ApiCall apiCall) {
        Map<String, Object> extracted = new HashMap<>();
        ExtractionStats stats = new ExtractionStats();

        for (String fieldName : apiCall.getDataSource().getProvidesFields()) {
            extractField(responseBody, apiCall, fieldName, extracted, stats);
        }

        logStats(stats);
        return extracted;
    }

    /**
     * Gets default values for all fields when an API call fails.
     *
     * <p><b>What:</b> Returns configured default values for an API's fields.</p>
     *
     * <p><b>Why:</b> When an API is unavailable, we still want to provide
     * reasonable fallback values so document retrieval can continue.</p>
     *
     * <p><b>How:</b> Iterates through all fields the API provides and collects
     * those that have default values configured.</p>
     *
     * @param apiCall The API call configuration
     * @return Map of field names to their default values
     */
    public Map<String, Object> getDefaultValues(ApiCall apiCall) {
        Map<String, Object> defaults = new HashMap<>();

        for (String fieldName : apiCall.getDataSource().getProvidesFields()) {
            FieldSourceConfig fieldSource = apiCall.getFieldSources().get(fieldName);

            if (hasDefaultValue(fieldSource)) {
                defaults.put(fieldName, fieldSource.getDefaultValue());
            }
        }

        return defaults;
    }

    /**
     * Extracts a single field from the response body.
     *
     * <p><b>What:</b> Attempts to extract one field using its JSONPath configuration.</p>
     *
     * <p><b>Why:</b> Each field has its own extraction path and default value.
     * Extracting fields individually allows for granular error handling.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Looks up the field's configuration</li>
     *   <li>Skips if no extraction config exists</li>
     *   <li>Executes JSONPath and handles the result</li>
     *   <li>Falls back to default on error</li>
     * </ol>
     * </p>
     *
     * @param responseBody The JSON response body
     * @param apiCall The API call configuration
     * @param fieldName The name of the field to extract
     * @param extracted The map to store extracted values
     * @param stats Statistics tracker for logging
     */
    private void extractField(
            String responseBody,
            ApiCall apiCall,
            String fieldName,
            Map<String, Object> extracted,
            ExtractionStats stats) {

        FieldSourceConfig fieldSource = apiCall.getFieldSources().get(fieldName);

        if (!hasExtractionConfig(fieldSource)) {
            log.debug("Skipping {} - No extraction config", fieldName);
            return;
        }

        try {
            Object value = extractValue(responseBody, fieldSource);
            handleExtractedValue(fieldName, value, fieldSource, extracted, stats);
        } catch (Exception e) {
            handleExtractionError(fieldName, fieldSource, extracted, stats, e);
        }
    }

    /**
     * Executes the JSONPath query and processes the result.
     *
     * <p><b>What:</b> Runs the JSONPath expression against the response.</p>
     *
     * <p><b>Why:</b> JSONPath queries can return various types (objects, arrays,
     * primitives). We need to normalize the result for consistent handling.</p>
     *
     * <p><b>How:</b> Uses the JsonPath library to execute the query, then
     * unwraps single-element arrays for convenience.</p>
     *
     * @param responseBody The JSON response to query
     * @param fieldSource The field configuration with extraction path
     * @return The extracted value (may be null)
     */
    private Object extractValue(String responseBody, FieldSourceConfig fieldSource) {
        Object value = JsonPath.read(responseBody, fieldSource.getExtractionPath());
        return unwrapSingleElementArray(value);
    }

    /**
     * Unwraps single-element arrays to their contained value.
     *
     * <p><b>What:</b> Converts [value] to value for single-element arrays.</p>
     *
     * <p><b>Why:</b> JSONPath filter expressions (like $.items[?(@.active)].id)
     * always return arrays, even for single matches. For single values, it's
     * more convenient to work with the unwrapped value.</p>
     *
     * <p><b>Example:</b>
     * <pre>
     * JSONPath result: ["PRC-12345"]
     * After unwrap: "PRC-12345"
     * </pre>
     * </p>
     *
     * @param value The value to potentially unwrap
     * @return The unwrapped value if single-element array, otherwise unchanged
     */
    private Object unwrapSingleElementArray(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() == 1) {
                return list.get(0);
            }
        }
        return value;
    }

    /**
     * Handles a successfully extracted value.
     *
     * <p><b>What:</b> Processes the extraction result and updates statistics.</p>
     *
     * <p><b>Why:</b> After extraction, we need to decide what to store:
     * the extracted value (if present) or a default value (if null).</p>
     *
     * <p><b>How:</b> If value is non-null, stores it. If null but a default
     * is configured, stores the default. Updates stats accordingly.</p>
     *
     * @param fieldName The name of the field
     * @param value The extracted value (may be null)
     * @param fieldSource The field configuration with default value
     * @param extracted The map to store the result
     * @param stats Statistics tracker
     */
    private void handleExtractedValue(
            String fieldName,
            Object value,
            FieldSourceConfig fieldSource,
            Map<String, Object> extracted,
            ExtractionStats stats) {

        if (value != null) {
            extracted.put(fieldName, value);
            stats.successCount++;
            log.debug("Extracted {}: {}", fieldName, value);
        } else if (hasDefaultValue(fieldSource)) {
            extracted.put(fieldName, fieldSource.getDefaultValue());
            stats.defaultCount++;
            log.debug("Using default for {}: {}", fieldName, fieldSource.getDefaultValue());
        }
    }

    /**
     * Handles extraction errors by using default values.
     *
     * <p><b>What:</b> Error recovery for failed field extractions.</p>
     *
     * <p><b>Why:</b> JSONPath queries can fail for various reasons:
     * invalid path, missing data, type mismatches. We log the error
     * and fall back to defaults rather than failing the whole process.</p>
     *
     * <p><b>How:</b> Logs the warning, increments failure count, and
     * applies default value if configured.</p>
     *
     * @param fieldName The field that failed to extract
     * @param fieldSource The field configuration with default value
     * @param extracted The map to store the default (if available)
     * @param stats Statistics tracker
     * @param e The exception that occurred
     */
    private void handleExtractionError(
            String fieldName,
            FieldSourceConfig fieldSource,
            Map<String, Object> extracted,
            ExtractionStats stats,
            Exception e) {

        stats.failedCount++;
        log.warn("Failed to extract {}: {}", fieldName, e.getMessage());

        if (hasDefaultValue(fieldSource)) {
            extracted.put(fieldName, fieldSource.getDefaultValue());
            stats.defaultCount++;
        }
    }

    /**
     * Checks if a field has extraction configuration.
     *
     * <p><b>Why:</b> Some fields in the "providesFields" list may not have
     * extraction paths configured (e.g., they use hardcoded defaults).</p>
     *
     * @param fieldSource The field configuration to check
     * @return true if the field has an extraction path configured
     */
    private boolean hasExtractionConfig(FieldSourceConfig fieldSource) {
        return fieldSource != null && fieldSource.getExtractionPath() != null;
    }

    /**
     * Checks if a field has a default value configured.
     *
     * <p><b>Why:</b> Default values provide fallbacks when extraction fails
     * or returns null.</p>
     *
     * @param fieldSource The field configuration to check
     * @return true if the field has a default value configured
     */
    private boolean hasDefaultValue(FieldSourceConfig fieldSource) {
        return fieldSource != null && fieldSource.getDefaultValue() != null;
    }

    /**
     * Logs extraction statistics for monitoring and debugging.
     *
     * <p><b>Why:</b> Tracking success/default/failure counts helps identify
     * issues with API responses or JSONPath configurations.</p>
     *
     * @param stats The statistics to log
     */
    private void logStats(ExtractionStats stats) {
        log.info("Field extraction: {} extracted, {} defaults, {} failed",
                stats.successCount, stats.defaultCount, stats.failedCount);
    }

    /**
     * Internal class for tracking extraction statistics.
     *
     * <p><b>What:</b> Simple counter class for extraction results.</p>
     *
     * <p><b>Why:</b> Aggregating counts across multiple field extractions
     * provides useful metrics for logging and monitoring.</p>
     */
    private static class ExtractionStats {
        /** Count of successfully extracted fields. */
        int successCount = 0;

        /** Count of fields using default values. */
        int defaultCount = 0;

        /** Count of fields that failed to extract. */
        int failedCount = 0;
    }
}
