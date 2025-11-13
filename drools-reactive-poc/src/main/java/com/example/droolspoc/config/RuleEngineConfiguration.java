package com.example.droolspoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration-Driven Rule Engine
 *
 * This configuration is loaded from YAML and allows adding new:
 * - External APIs (data sources)
 * - Rules
 * - Conditions
 *
 * WITHOUT any code changes!
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class RuleEngineConfiguration {

    private List<DataSourceConfig> dataSources;
    private List<RuleConfig> rules;

    /**
     * Data Source Configuration (External APIs)
     */
    @Data
    public static class DataSourceConfig {
        private String id;
        private String name;
        private String type;  // REST_API, DATABASE, etc.
        private String method;  // GET, POST, etc.
        private String baseUrl;
        private String endpoint;
        private Integer timeoutMs;
        private Integer retryCount;
        private List<DependencyConfig> dependsOn;  // ⭐ For chained API calls
        private List<ResponseMappingConfig> responseMapping;
    }

    /**
     * Dependency Configuration (for chained API calls)
     */
    @Data
    public static class DependencyConfig {
        private String sourceId;  // Which data source provides the value
        private String field;     // Which field from that source
    }

    /**
     * Response Mapping Configuration
     */
    @Data
    public static class ResponseMappingConfig {
        private String fieldName;  // Internal field name
        private String jsonPath;   // JSON path in response
        private String dataType;   // STRING, INTEGER, DECIMAL, DATE, etc.
    }

    /**
     * Rule Configuration
     */
    @Data
    public static class RuleConfig {
        private String ruleId;
        private String name;
        private String description;
        private Integer priority;
        private Boolean enabled;
        private ConditionGroupConfig conditions;
        private ActionsConfig actions;
    }

    /**
     * Condition Group Configuration (supports nested AND/OR)
     */
    @Data
    public static class ConditionGroupConfig {
        private String type;  // ALL (AND) or ANY (OR)
        private List<Object> expressions;  // Can be ConditionConfig or nested ConditionGroupConfig
    }

    /**
     * Single Condition Configuration
     */
    @Data
    public static class ConditionConfig {
        private String source;    // Data source ID
        private String field;     // Field name
        private String operator;  // EQUALS, GREATER_THAN, IN, MATCHES, etc.
        private Object value;     // Comparison value
    }

    /**
     * Actions Configuration
     */
    @Data
    public static class ActionsConfig {
        private List<DocumentActionConfig> addDocuments;
    }

    /**
     * Document Action Configuration
     */
    @Data
    public static class DocumentActionConfig {
        private String documentId;
        private String documentName;
    }
}
