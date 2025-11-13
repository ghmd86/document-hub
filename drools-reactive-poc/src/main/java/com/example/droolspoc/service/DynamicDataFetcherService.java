package com.example.droolspoc.service;

import com.example.droolspoc.config.RuleEngineConfiguration;
import com.example.droolspoc.config.RuleEngineConfiguration.DataSourceConfig;
import com.example.droolspoc.config.RuleEngineConfiguration.DependencyConfig;
import com.example.droolspoc.config.RuleEngineConfiguration.ResponseMappingConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dynamic Data Fetcher Service
 *
 * Fetches data from external APIs based on YAML configuration:
 * - Resolves dependencies (chained API calls)
 * - Handles parallel and sequential calls
 * - Maps responses to internal field names
 * - Returns unified data context
 *
 * NO CODE CHANGES needed to add new APIs!
 */
@Service
public class DynamicDataFetcherService {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataFetcherService.class);

    private final RuleEngineConfiguration config;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Autowired
    public DynamicDataFetcherService(
        RuleEngineConfiguration config,
        WebClient.Builder webClientBuilder,
        ObjectMapper objectMapper
    ) {
        this.config = config;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch all data from configured sources
     *
     * This method:
     * 1. Analyzes dependencies between data sources
     * 2. Fetches independent sources in PARALLEL
     * 3. Fetches dependent sources SEQUENTIALLY (chained)
     * 4. Returns unified data context
     *
     * @param params Input parameters (customerId, accountId, arrangementId, etc.)
     * @return Mono of data context (Map of source → fields)
     */
    public Mono<Map<String, Map<String, Object>>> fetchAllData(Map<String, String> params) {
        log.info("Fetching data from {} sources", config.getDataSources().size());

        // Build dependency graph
        Map<String, List<String>> dependencies = buildDependencyGraph();

        // Determine execution order (topological sort)
        List<List<String>> executionLevels = determineExecutionLevels(dependencies);

        log.debug("Execution levels: {}", executionLevels);

        // Execute level by level
        return executeLevels(executionLevels, params, new ConcurrentHashMap<>());
    }

    /**
     * Build dependency graph
     * Returns map of: dataSourceId → list of dependencies
     */
    private Map<String, List<String>> buildDependencyGraph() {
        Map<String, List<String>> graph = new HashMap<>();

        for (DataSourceConfig ds : config.getDataSources()) {
            List<String> deps = ds.getDependsOn() != null
                ? ds.getDependsOn().stream()
                    .map(DependencyConfig::getSourceId)
                    .collect(Collectors.toList())
                : Collections.emptyList();

            graph.put(ds.getId(), deps);
        }

        return graph;
    }

    /**
     * Determine execution levels using topological sort
     *
     * Level 0: No dependencies (parallel)
     * Level 1: Depends only on Level 0 (parallel within level)
     * Level 2: Depends on Level 0 or 1 (parallel within level)
     * etc.
     */
    private List<List<String>> determineExecutionLevels(Map<String, List<String>> dependencies) {
        List<List<String>> levels = new ArrayList<>();
        Set<String> completed = new HashSet<>();

        while (completed.size() < dependencies.size()) {
            List<String> currentLevel = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
                String sourceId = entry.getKey();
                List<String> deps = entry.getValue();

                // Can execute if not already completed and all dependencies completed
                if (!completed.contains(sourceId) && completed.containsAll(deps)) {
                    currentLevel.add(sourceId);
                }
            }

            if (currentLevel.isEmpty()) {
                throw new IllegalStateException("Circular dependency detected in data sources");
            }

            levels.add(currentLevel);
            completed.addAll(currentLevel);
        }

        return levels;
    }

    /**
     * Execute data sources level by level
     */
    private Mono<Map<String, Map<String, Object>>> executeLevels(
        List<List<String>> levels,
        Map<String, String> params,
        Map<String, Map<String, Object>> dataContext
    ) {
        Mono<Map<String, Map<String, Object>>> result = Mono.just(dataContext);

        for (List<String> level : levels) {
            result = result.flatMap(context ->
                executeLevelInParallel(level, params, context)
            );
        }

        return result;
    }

    /**
     * Execute all data sources in a level in PARALLEL
     */
    private Mono<Map<String, Map<String, Object>>> executeLevelInParallel(
        List<String> sourceIds,
        Map<String, String> params,
        Map<String, Map<String, Object>> dataContext
    ) {
        log.debug("Executing level in parallel: {}", sourceIds);

        // Create Mono for each source in this level
        List<Mono<Map.Entry<String, Map<String, Object>>>> monos = sourceIds.stream()
            .map(sourceId -> fetchDataSource(sourceId, params, dataContext))
            .collect(Collectors.toList());

        // Execute all in parallel and combine results
        return Mono.zip(monos, results -> {
            Map<String, Map<String, Object>> combined = new HashMap<>(dataContext);
            for (Object result : results) {
                @SuppressWarnings("unchecked")
                Map.Entry<String, Map<String, Object>> entry =
                    (Map.Entry<String, Map<String, Object>>) result;
                combined.put(entry.getKey(), entry.getValue());
            }
            return combined;
        });
    }

    /**
     * Fetch data from a single data source
     *
     * @param sourceId Data source ID
     * @param params Input parameters
     * @param dataContext Current data context (for resolving dependencies)
     * @return Mono of entry (sourceId → field data)
     */
    private Mono<Map.Entry<String, Map<String, Object>>> fetchDataSource(
        String sourceId,
        Map<String, String> params,
        Map<String, Map<String, Object>> dataContext
    ) {
        DataSourceConfig ds = findDataSource(sourceId);

        log.debug("Fetching data from source: {}", ds.getName());

        // Build endpoint with parameters (resolve dependencies)
        String endpoint = resolveEndpoint(ds, params, dataContext);

        // Create WebClient for this source
        WebClient webClient = webClientBuilder
            .baseUrl(ds.getBaseUrl())
            .build();

        // Execute API call
        return webClient
            .method(org.springframework.http.HttpMethod.valueOf(ds.getMethod()))
            .uri(endpoint)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(ds.getTimeoutMs()))
            .retry(ds.getRetryCount())
            .map(responseBody -> {
                // Map response fields using JSON Path
                Map<String, Object> mappedData = mapResponseFields(responseBody, ds.getResponseMapping());
                return Map.entry(sourceId, mappedData);
            })
            .doOnSuccess(entry ->
                log.debug("Fetched data from {}: {} fields", sourceId, entry.getValue().size())
            )
            .onErrorResume(error -> {
                log.error("Error fetching data from {}: {}", sourceId, error.getMessage());
                return Mono.just(Map.entry(sourceId, Collections.emptyMap()));
            });
    }

    /**
     * Resolve endpoint by replacing placeholders with actual values
     *
     * Supports:
     * - {paramName} - from input params
     * - {dependentField} - from dependent data source
     */
    private String resolveEndpoint(
        DataSourceConfig ds,
        Map<String, String> params,
        Map<String, Map<String, Object>> dataContext
    ) {
        String endpoint = ds.getEndpoint();

        // Replace input parameters
        for (Map.Entry<String, String> param : params.entrySet()) {
            endpoint = endpoint.replace("{" + param.getKey() + "}", param.getValue());
        }

        // Replace dependent fields
        if (ds.getDependsOn() != null) {
            for (DependencyConfig dep : ds.getDependsOn()) {
                Map<String, Object> depData = dataContext.get(dep.getSourceId());
                if (depData != null && depData.containsKey(dep.getField())) {
                    Object value = depData.get(dep.getField());
                    endpoint = endpoint.replace("{" + dep.getField() + "}", value.toString());
                }
            }
        }

        return endpoint;
    }

    /**
     * Map response fields using JSON Path
     */
    private Map<String, Object> mapResponseFields(
        String responseBody,
        List<ResponseMappingConfig> mappings
    ) {
        Map<String, Object> result = new HashMap<>();

        for (ResponseMappingConfig mapping : mappings) {
            try {
                Object value = JsonPath.read(responseBody, mapping.getJsonPath());
                result.put(mapping.getFieldName(), convertValue(value, mapping.getDataType()));
            } catch (Exception e) {
                log.warn("Failed to extract field {} from response: {}",
                    mapping.getFieldName(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Convert value to specified data type
     */
    private Object convertValue(Object value, String dataType) {
        if (value == null) {
            return null;
        }

        switch (dataType) {
            case "STRING":
                return value.toString();
            case "INTEGER":
                return value instanceof Number
                    ? ((Number) value).intValue()
                    : Integer.parseInt(value.toString());
            case "DECIMAL":
                return value instanceof Number
                    ? ((Number) value).doubleValue()
                    : Double.parseDouble(value.toString());
            case "BOOLEAN":
                return Boolean.parseBoolean(value.toString());
            case "DATE":
                // Parse date string to LocalDate/LocalDateTime
                return value.toString();  // Simplified for demo
            default:
                return value;
        }
    }

    /**
     * Find data source by ID
     */
    private DataSourceConfig findDataSource(String sourceId) {
        return config.getDataSources().stream()
            .filter(ds -> ds.getId().equals(sourceId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown data source: " + sourceId));
    }
}
