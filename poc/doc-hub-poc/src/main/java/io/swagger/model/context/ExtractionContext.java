package io.swagger.model.context;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
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

    @Builder.Default
    private long startTime = System.currentTimeMillis();

    public void addVariable(String key, Object value) {
        variables.put(key, value);
    }

    public void markDataSourceSuccess(String dataSourceId) {
        dataSourceStatus.put(dataSourceId, true);
    }

    public void markDataSourceFailed(String dataSourceId) {
        dataSourceStatus.put(dataSourceId, false);
    }

    public void incrementCacheHits() {
        this.cacheHits++;
    }

    public void incrementCacheMisses() {
        this.cacheMisses++;
    }

    public void incrementTotalApiCalls() {
        this.totalApiCalls++;
    }

    public long getExecutionTimeMs() {
        return System.currentTimeMillis() - startTime;
    }
}
