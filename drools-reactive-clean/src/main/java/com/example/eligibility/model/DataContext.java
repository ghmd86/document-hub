package com.example.eligibility.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Data Context
 *
 * Container for all fetched data from external APIs.
 * Used to pass data between services.
 *
 * Structure:
 * {
 *   "arrangements_api": {"pricingId": "P123", "productCode": "CREDIT_CARD"},
 *   "account_service_api": {"accountBalance": 75000, "accountStatus": "ACTIVE"}
 * }
 */
public class DataContext {

    private Map<String, Map<String, Object>> dataBySource;

    public DataContext() {
        this.dataBySource = new HashMap<>();
    }

    public Map<String, Map<String, Object>> getDataBySource() {
        return dataBySource;
    }

    public void setDataBySource(Map<String, Map<String, Object>> dataBySource) {
        this.dataBySource = dataBySource;
    }

    /**
     * Add data for a specific source
     *
     * @param sourceId Source identifier
     * @param data Data map
     */
    public void addSourceData(String sourceId, Map<String, Object> data) {
        dataBySource.put(sourceId, data);
    }

    /**
     * Get data for a specific source
     *
     * @param sourceId Source identifier
     * @return Data map or null if not found
     */
    public Map<String, Object> getSourceData(String sourceId) {
        return dataBySource.get(sourceId);
    }

    /**
     * Get a specific field value from a source
     *
     * @param sourceId Source identifier
     * @param fieldName Field name
     * @return Field value or null if not found
     */
    public Object getField(String sourceId, String fieldName) {
        Map<String, Object> sourceData = dataBySource.get(sourceId);
        return sourceData != null ? sourceData.get(fieldName) : null;
    }

    /**
     * Check if data exists for a source
     *
     * @param sourceId Source identifier
     * @return true if data exists
     */
    public boolean hasSource(String sourceId) {
        return dataBySource.containsKey(sourceId);
    }

    /**
     * Get all source IDs
     *
     * @return Set of source IDs
     */
    public Set<String> getSourceIds() {
        return dataBySource.keySet();
    }

    @Override
    public String toString() {
        return "DataContext{" +
                "dataBySource=" + dataBySource +
                '}';
    }
}
