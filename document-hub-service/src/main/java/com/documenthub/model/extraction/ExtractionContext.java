package com.documenthub.model.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context holding all variables during extraction execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionContext {

    private UUID accountId;
    private UUID customerId;
    private String correlationId;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    @Builder.Default
    private Map<String, Boolean> dataSourceStatus = new HashMap<>();

    @Builder.Default
    private int cacheHits = 0;

    @Builder.Default
    private int cacheMisses = 0;

    @Builder.Default
    private int totalApiCalls = 0;

    private long startTime;

    public void addVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public void markDataSourceSuccess(String dataSourceId) {
        dataSourceStatus.put(dataSourceId, true);
    }

    public void markDataSourceFailed(String dataSourceId) {
        dataSourceStatus.put(dataSourceId, false);
    }

    public boolean isDataSourceSuccess(String dataSourceId) {
        return Boolean.TRUE.equals(dataSourceStatus.get(dataSourceId));
    }

    public void incrementCacheHit() {
        this.cacheHits++;
    }

    public void incrementCacheMiss() {
        this.cacheMisses++;
    }

    public void incrementApiCall() {
        this.totalApiCalls++;
    }

    public long getExecutionTimeMs() {
        return System.currentTimeMillis() - startTime;
    }
}
