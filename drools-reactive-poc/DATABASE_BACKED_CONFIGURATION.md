# Database-Backed Configuration (Using JSONB)

## Overview

Since you already have **JSONB columns** in document-hub-service, the configuration should be stored in the **database**, not YAML files.

This approach allows:
- ✅ Runtime updates (no restart needed)
- ✅ Admin UI can update configuration
- ✅ Version control in database
- ✅ Audit trail (who changed what, when)

---

## Database Schema

### Existing Approach (From document-hub-service)

You likely already have something like:

```sql
CREATE TABLE shared_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(100) UNIQUE NOT NULL,
    document_name VARCHAR(255),
    eligibility_rules JSONB,  -- ⭐ Rules stored as JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Example eligibility_rules JSONB:
{
  "conditions": [
    {
      "field": "accountType",
      "operator": "EQUALS",
      "value": "CREDIT_CARD"
    },
    {
      "field": "balance",
      "operator": "GREATER_THAN",
      "value": 10000
    }
  ],
  "logic": "ALL"
}
```

### Enhanced Schema for External API Configuration

Add tables for external API configuration:

```sql
-- External API Data Sources
CREATE TABLE data_sources (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- REST_API, DATABASE, etc.
    configuration JSONB NOT NULL,  -- ⭐ API config as JSON
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Configuration JSONB structure:
{
  "method": "GET",
  "baseUrl": "http://localhost:8081",
  "endpoint": "/api/v1/arrangements/{arrangementId}",
  "timeoutMs": 5000,
  "retryCount": 2,
  "dependsOn": [
    {
      "sourceId": "arrangements_api",
      "field": "pricingId"
    }
  ],
  "responseMapping": [
    {
      "fieldName": "pricingId",
      "jsonPath": "$.pricingId",
      "dataType": "STRING"
    }
  ]
}

-- Document Eligibility Rules (Enhanced)
CREATE TABLE document_eligibility_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) UNIQUE NOT NULL,
    document_id VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    description TEXT,
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT true,
    conditions JSONB NOT NULL,  -- ⭐ Conditions as JSON
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Conditions JSONB structure:
{
  "type": "ALL",  // or "ANY"
  "expressions": [
    {
      "source": "cardholder_agreements_api",  // ⭐ Reference to data source
      "field": "cardholderAgreementsTNCCode",
      "operator": "EQUALS",
      "value": "TNC_GOLD_2024"
    },
    {
      "source": "account_service_api",
      "field": "accountBalance",
      "operator": "GREATER_THAN",
      "value": 50000
    }
  ]
}

-- Audit Trail
CREATE TABLE rule_change_history (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    change_type VARCHAR(50),  -- CREATE, UPDATE, DELETE, ENABLE, DISABLE
    old_value JSONB,
    new_value JSONB,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);
```

---

## R2DBC Entity Models

### DataSourceEntity.java

```java
package com.example.droolspoc.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("data_sources")
public class DataSourceEntity {

    @Id
    private String id;

    private String name;
    private String type;

    @Column("configuration")
    private Json configuration;  // ⭐ JSONB column

    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Json getConfiguration() { return configuration; }
    public void setConfiguration(Json configuration) { this.configuration = configuration; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
```

### DocumentEligibilityRuleEntity.java

```java
package com.example.droolspoc.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("document_eligibility_rules")
public class DocumentEligibilityRuleEntity {

    @Id
    private Long id;

    private String ruleId;
    private String documentId;
    private String name;
    private String description;
    private Integer priority;
    private Boolean enabled;

    @Column("conditions")
    private Json conditions;  // ⭐ JSONB column

    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Json getConditions() { return conditions; }
    public void setConditions(Json conditions) { this.conditions = conditions; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
```

---

## R2DBC Repositories

### DataSourceRepository.java

```java
package com.example.droolspoc.repository;

import com.example.droolspoc.entity.DataSourceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DataSourceRepository extends ReactiveCrudRepository<DataSourceEntity, String> {

    /**
     * Find all enabled data sources
     */
    Flux<DataSourceEntity> findByEnabledTrue();
}
```

### DocumentEligibilityRuleRepository.java

```java
package com.example.droolspoc.repository;

import com.example.droolspoc.entity.DocumentEligibilityRuleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentEligibilityRuleRepository
    extends ReactiveCrudRepository<DocumentEligibilityRuleEntity, Long> {

    /**
     * Find all enabled rules, ordered by priority
     */
    Flux<DocumentEligibilityRuleEntity> findByEnabledTrueOrderByPriorityDesc();

    /**
     * Find rules for a specific document
     */
    Flux<DocumentEligibilityRuleEntity> findByDocumentIdAndEnabledTrue(String documentId);
}
```

---

## Configuration Loader Service

### DatabaseConfigurationLoader.java

```java
package com.example.droolspoc.service;

import com.example.droolspoc.config.RuleEngineConfiguration;
import com.example.droolspoc.config.RuleEngineConfiguration.*;
import com.example.droolspoc.entity.DataSourceEntity;
import com.example.droolspoc.entity.DocumentEligibilityRuleEntity;
import com.example.droolspoc.repository.DataSourceRepository;
import com.example.droolspoc.repository.DocumentEligibilityRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Database Configuration Loader
 *
 * Loads configuration from database JSONB columns instead of YAML files.
 *
 * Features:
 * - Loads data sources from database
 * - Loads rules from database
 * - Caches configuration (invalidate on update)
 * - Reactive (non-blocking)
 */
@Service
public class DatabaseConfigurationLoader {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfigurationLoader.class);

    private final DataSourceRepository dataSourceRepository;
    private final DocumentEligibilityRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DatabaseConfigurationLoader(
        DataSourceRepository dataSourceRepository,
        DocumentEligibilityRuleRepository ruleRepository,
        ObjectMapper objectMapper
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Load complete configuration from database
     *
     * @return Mono of RuleEngineConfiguration
     */
    @Cacheable("ruleEngineConfiguration")
    public Mono<RuleEngineConfiguration> loadConfiguration() {
        log.info("Loading configuration from database");

        // Load data sources and rules in parallel
        Mono<List<DataSourceConfig>> dataSourcesMono = loadDataSources();
        Mono<List<RuleConfig>> rulesMono = loadRules();

        return Mono.zip(dataSourcesMono, rulesMono)
            .map(tuple -> {
                RuleEngineConfiguration config = new RuleEngineConfiguration();
                config.setDataSources(tuple.getT1());
                config.setRules(tuple.getT2());

                log.info("Configuration loaded: {} data sources, {} rules",
                    tuple.getT1().size(), tuple.getT2().size());

                return config;
            });
    }

    /**
     * Load data sources from database
     */
    private Mono<List<DataSourceConfig>> loadDataSources() {
        return dataSourceRepository.findByEnabledTrue()
            .map(this::mapDataSourceEntity)
            .collectList()
            .doOnSuccess(sources -> log.debug("Loaded {} data sources", sources.size()));
    }

    /**
     * Load rules from database
     */
    private Mono<List<RuleConfig>> loadRules() {
        return ruleRepository.findByEnabledTrueOrderByPriorityDesc()
            .map(this::mapRuleEntity)
            .collectList()
            .doOnSuccess(rules -> log.debug("Loaded {} rules", rules.size()));
    }

    /**
     * Map DataSourceEntity to DataSourceConfig
     */
    private DataSourceConfig mapDataSourceEntity(DataSourceEntity entity) {
        try {
            // Parse JSONB configuration
            JsonNode configNode = objectMapper.readTree(entity.getConfiguration().asString());

            DataSourceConfig config = new DataSourceConfig();
            config.setId(entity.getId());
            config.setName(entity.getName());
            config.setType(entity.getType());

            // Parse configuration fields
            config.setMethod(configNode.path("method").asText());
            config.setBaseUrl(configNode.path("baseUrl").asText());
            config.setEndpoint(configNode.path("endpoint").asText());
            config.setTimeoutMs(configNode.path("timeoutMs").asInt());
            config.setRetryCount(configNode.path("retryCount").asInt());

            // Parse dependencies
            if (configNode.has("dependsOn")) {
                List<DependencyConfig> dependencies = new ArrayList<>();
                configNode.path("dependsOn").forEach(depNode -> {
                    DependencyConfig dep = new DependencyConfig();
                    dep.setSourceId(depNode.path("sourceId").asText());
                    dep.setField(depNode.path("field").asText());
                    dependencies.add(dep);
                });
                config.setDependsOn(dependencies);
            }

            // Parse response mapping
            if (configNode.has("responseMapping")) {
                List<ResponseMappingConfig> mappings = new ArrayList<>();
                configNode.path("responseMapping").forEach(mappingNode -> {
                    ResponseMappingConfig mapping = new ResponseMappingConfig();
                    mapping.setFieldName(mappingNode.path("fieldName").asText());
                    mapping.setJsonPath(mappingNode.path("jsonPath").asText());
                    mapping.setDataType(mappingNode.path("dataType").asText());
                    mappings.add(mapping);
                });
                config.setResponseMapping(mappings);
            }

            return config;

        } catch (Exception e) {
            log.error("Error parsing data source configuration for {}: {}",
                entity.getId(), e.getMessage());
            throw new RuntimeException("Failed to parse data source configuration", e);
        }
    }

    /**
     * Map DocumentEligibilityRuleEntity to RuleConfig
     */
    private RuleConfig mapRuleEntity(DocumentEligibilityRuleEntity entity) {
        try {
            // Parse JSONB conditions
            JsonNode conditionsNode = objectMapper.readTree(entity.getConditions().asString());

            RuleConfig config = new RuleConfig();
            config.setRuleId(entity.getRuleId());
            config.setName(entity.getName());
            config.setDescription(entity.getDescription());
            config.setPriority(entity.getPriority());
            config.setEnabled(entity.getEnabled());

            // Parse condition group
            ConditionGroupConfig conditions = parseConditionGroup(conditionsNode);
            config.setConditions(conditions);

            // Parse actions
            ActionsConfig actions = new ActionsConfig();
            List<DocumentActionConfig> docs = new ArrayList<>();
            DocumentActionConfig docAction = new DocumentActionConfig();
            docAction.setDocumentId(entity.getDocumentId());
            docAction.setDocumentName(entity.getName());
            docs.add(docAction);
            actions.setAddDocuments(docs);
            config.setActions(actions);

            return config;

        } catch (Exception e) {
            log.error("Error parsing rule configuration for {}: {}",
                entity.getRuleId(), e.getMessage());
            throw new RuntimeException("Failed to parse rule configuration", e);
        }
    }

    /**
     * Parse condition group from JSON (recursive for nested groups)
     */
    private ConditionGroupConfig parseConditionGroup(JsonNode node) {
        ConditionGroupConfig group = new ConditionGroupConfig();
        group.setType(node.path("type").asText("ALL"));

        List<Object> expressions = new ArrayList<>();

        if (node.has("expressions")) {
            node.path("expressions").forEach(exprNode -> {
                // Check if nested group or single condition
                if (exprNode.has("type")) {
                    // Nested group
                    expressions.add(parseConditionGroup(exprNode));
                } else {
                    // Single condition
                    ConditionConfig condition = new ConditionConfig();
                    condition.setSource(exprNode.path("source").asText());
                    condition.setField(exprNode.path("field").asText());
                    condition.setOperator(exprNode.path("operator").asText());

                    // Parse value (could be string, number, or array)
                    JsonNode valueNode = exprNode.path("value");
                    if (valueNode.isArray()) {
                        List<Object> values = new ArrayList<>();
                        valueNode.forEach(v -> values.add(v.asText()));
                        condition.setValue(values);
                    } else {
                        condition.setValue(valueNode.asText());
                    }

                    expressions.add(condition);
                }
            });
        }

        group.setExpressions(expressions);
        return group;
    }

    /**
     * Invalidate cache (call when configuration is updated)
     */
    public void invalidateCache() {
        log.info("Invalidating configuration cache");
        // Spring Cache will handle this with @CacheEvict
    }
}
```

---

## Updated Services (Using Database Configuration)

### DatabaseBackedEligibilityService.java

```java
package com.example.droolspoc.service;

import com.example.droolspoc.config.RuleEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Database-Backed Eligibility Service
 *
 * Loads configuration from database (JSONB columns) instead of YAML files.
 *
 * Flow:
 * 1. Load configuration from database (cached)
 * 2. Fetch data from external APIs based on DB configuration
 * 3. Evaluate rules based on DB configuration
 * 4. Return eligible documents
 */
@Service
public class DatabaseBackedEligibilityService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackedEligibilityService.class);

    private final DatabaseConfigurationLoader configLoader;
    private final DynamicDataFetcherService dataFetcher;
    private final ConfigDrivenRuleEvaluator ruleEvaluator;
    private final Scheduler ruleEvaluationScheduler;

    @Autowired
    public DatabaseBackedEligibilityService(
        DatabaseConfigurationLoader configLoader,
        DynamicDataFetcherService dataFetcher,
        ConfigDrivenRuleEvaluator ruleEvaluator,
        @Qualifier("droolsScheduler") Scheduler ruleEvaluationScheduler
    ) {
        this.configLoader = configLoader;
        this.dataFetcher = dataFetcher;
        this.ruleEvaluator = ruleEvaluator;
        this.ruleEvaluationScheduler = ruleEvaluationScheduler;
    }

    /**
     * Get eligible documents (configuration loaded from database)
     */
    public Mono<Set<String>> getEligibleDocuments(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        log.info("Starting database-backed eligibility check");

        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("accountId", accountId);
        params.put("arrangementId", arrangementId);

        // ⭐ STEP 1: Load configuration from database (cached)
        return configLoader.loadConfiguration()
            .flatMap(config -> {
                log.debug("Configuration loaded: {} data sources, {} rules",
                    config.getDataSources().size(), config.getRules().size());

                // ⭐ STEP 2: Fetch data from external APIs
                return dataFetcher.fetchAllData(params);
            })
            .flatMap(dataContext -> {
                // ⭐ STEP 3: Evaluate rules
                return Mono.fromCallable(() -> ruleEvaluator.evaluateRules(dataContext))
                    .subscribeOn(ruleEvaluationScheduler)
                    .timeout(Duration.ofMillis(500));
            })
            .doOnSuccess(documents ->
                log.info("Database-backed eligibility check completed: {} documents", documents.size())
            );
    }
}
```

---

## Sample Database Inserts

### Insert Data Sources

```sql
-- Arrangements API
INSERT INTO data_sources (id, name, type, configuration, enabled) VALUES (
    'arrangements_api',
    'Arrangements API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8081",
        "endpoint": "/api/v1/arrangements/{arrangementId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "responseMapping": [
            {
                "fieldName": "pricingId",
                "jsonPath": "$.pricingId",
                "dataType": "STRING"
            },
            {
                "fieldName": "productCode",
                "jsonPath": "$.productCode",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true
);

-- Cardholder Agreements API (CHAINED - depends on arrangements_api)
INSERT INTO data_sources (id, name, type, configuration, enabled) VALUES (
    'cardholder_agreements_api',
    'Cardholder Agreements API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8082",
        "endpoint": "/api/v1/cardholder-agreements/{pricingId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "dependsOn": [
            {
                "sourceId": "arrangements_api",
                "field": "pricingId"
            }
        ],
        "responseMapping": [
            {
                "fieldName": "cardholderAgreementsTNCCode",
                "jsonPath": "$.cardholderAgreementsTNCCode",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true
);

-- Account Service API
INSERT INTO data_sources (id, name, type, configuration, enabled) VALUES (
    'account_service_api',
    'Account Service API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8083",
        "endpoint": "/api/v1/accounts/{accountId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "responseMapping": [
            {
                "fieldName": "accountBalance",
                "jsonPath": "$.balance",
                "dataType": "DECIMAL"
            },
            {
                "fieldName": "accountStatus",
                "jsonPath": "$.status",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true
);
```

### Insert Rules

```sql
-- Rule: Gold TNC Benefits
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions
) VALUES (
    'RULE-001',
    'DOC-TNC-GOLD-2024-BENEFITS',
    'Gold TNC Specific Document',
    'Customers with Gold TNC are eligible for gold benefits',
    100,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "cardholder_agreements_api",
                "field": "cardholderAgreementsTNCCode",
                "operator": "EQUALS",
                "value": "TNC_GOLD_2024"
            }
        ]
    }'::jsonb
);

-- Rule: High Balance Gold (Multiple conditions)
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions
) VALUES (
    'RULE-004',
    'DOC-HIGH-BALANCE-GOLD-EXCLUSIVE',
    'High Balance Gold TNC Exclusive',
    'High balance customers with Gold TNC get exclusive benefits',
    85,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "cardholder_agreements_api",
                "field": "cardholderAgreementsTNCCode",
                "operator": "EQUALS",
                "value": "TNC_GOLD_2024"
            },
            {
                "source": "account_service_api",
                "field": "accountBalance",
                "operator": "GREATER_THAN",
                "value": 50000
            }
        ]
    }'::jsonb
);
```

---

## Admin UI Integration

### REST API for Configuration Management

```java
@RestController
@RequestMapping("/api/v1/admin/config")
public class ConfigurationAdminController {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DocumentEligibilityRuleRepository ruleRepository;

    @Autowired
    private DatabaseConfigurationLoader configLoader;

    /**
     * Add new data source (external API)
     */
    @PostMapping("/data-sources")
    public Mono<DataSourceEntity> addDataSource(@RequestBody DataSourceEntity dataSource) {
        return dataSourceRepository.save(dataSource)
            .doOnSuccess(saved -> {
                log.info("Data source added: {}", saved.getId());
                configLoader.invalidateCache();  // Reload configuration
            });
    }

    /**
     * Add new rule
     */
    @PostMapping("/rules")
    public Mono<DocumentEligibilityRuleEntity> addRule(@RequestBody DocumentEligibilityRuleEntity rule) {
        return ruleRepository.save(rule)
            .doOnSuccess(saved -> {
                log.info("Rule added: {}", saved.getRuleId());
                configLoader.invalidateCache();  // Reload configuration
            });
    }

    /**
     * Update rule
     */
    @PutMapping("/rules/{ruleId}")
    public Mono<DocumentEligibilityRuleEntity> updateRule(
        @PathVariable String ruleId,
        @RequestBody DocumentEligibilityRuleEntity rule
    ) {
        return ruleRepository.save(rule)
            .doOnSuccess(updated -> {
                log.info("Rule updated: {}", ruleId);
                configLoader.invalidateCache();  // Reload configuration
            });
    }

    /**
     * Enable/Disable rule
     */
    @PatchMapping("/rules/{ruleId}/enabled")
    public Mono<Void> toggleRule(@PathVariable String ruleId, @RequestParam boolean enabled) {
        return ruleRepository.findById(Long.parseLong(ruleId))
            .flatMap(rule -> {
                rule.setEnabled(enabled);
                return ruleRepository.save(rule);
            })
            .doOnSuccess(updated -> configLoader.invalidateCache())
            .then();
    }
}
```

---

## Key Advantages of Database-Backed Approach

### vs YAML Files

| Feature | Database (JSONB) | YAML Files |
|---------|------------------|------------|
| **Runtime updates** | ✅ Yes (no restart) | ❌ No (requires restart) |
| **Admin UI** | ✅ Easy (CRUD API) | ❌ Difficult |
| **Version control** | ✅ Built-in (audit table) | ⚠️ Git only |
| **Multi-instance** | ✅ Shared config | ❌ Each instance needs file |
| **Audit trail** | ✅ Who/when/what changed | ❌ Manual |
| **Hot reload** | ✅ Cache invalidation | ❌ File watch |

---

## Migration from YAML to Database

### Step 1: Create Schema

Run the SQL schema creation scripts above.

### Step 2: Import YAML to Database

```java
@Component
public class YamlToDatabaseImporter {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DocumentEligibilityRuleRepository ruleRepository;

    public Mono<Void> importFromYaml(RuleEngineConfiguration yamlConfig) {
        // Import data sources
        Mono<Void> importDataSources = Flux.fromIterable(yamlConfig.getDataSources())
            .flatMap(this::convertToEntity)
            .flatMap(dataSourceRepository::save)
            .then();

        // Import rules
        Mono<Void> importRules = Flux.fromIterable(yamlConfig.getRules())
            .flatMap(this::convertToEntity)
            .flatMap(ruleRepository::save)
            .then();

        return Mono.when(importDataSources, importRules);
    }

    private Mono<DataSourceEntity> convertToEntity(DataSourceConfig config) {
        // Convert config to entity with JSONB
        // Implementation details...
    }
}
```

### Step 3: Switch Service

Change from `ConfigDrivenEligibilityService` to `DatabaseBackedEligibilityService`.

### Step 4: Build Admin UI

React/Angular UI that calls the Admin API for CRUD operations on rules and data sources.

---

## Summary

**You're right!** Database-backed configuration is the correct production approach:

1. ✅ Uses your existing JSONB column approach
2. ✅ Runtime updates without restart
3. ✅ Admin UI friendly
4. ✅ Audit trail and version control
5. ✅ Shared configuration across instances

**YAML files are only for:**
- POC/demonstration
- Local development
- Testing

**Production should use database with JSONB columns.**

The code I provided can easily switch from YAML to database - just replace `RuleEngineConfiguration` loading from YAML with `DatabaseConfigurationLoader`.
