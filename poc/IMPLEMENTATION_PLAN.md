# Document Hub POC - Implementation Plan

## Overview

This document provides a step-by-step implementation guide for building the Document Hub POC. Follow these steps sequentially to ensure all components are properly integrated.

---

## Phase 1: Foundation Setup

### Task 1.1: Add Dependencies to pom.xml

**File**: `./poc/doc-hub-poc/pom.xml`

Add the following dependencies:

```xml
<!-- R2DBC PostgreSQL (Reactive Database) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- PostgreSQL JDBC Driver (for migrations/tools) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Redis Reactive (Caching) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>

<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>

<!-- JSONPath for data extraction -->
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.8.0</version>
</dependency>

<!-- Lombok (Optional but recommended) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Resilience4j (Circuit Breaker) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
    <version>2.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Test Dependencies -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
```

**Spring Boot Version Update** (if needed):
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.14</version> <!-- Updated version with better WebFlux support -->
</parent>
```

### Task 1.2: Create application.yml

**File**: `./poc/doc-hub-poc/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: document-hub-poc

  # R2DBC Database Configuration
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/documenthub
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      validation-query: SELECT 1

  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379
      password:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms

# Integration Service URLs
integration:
  services:
    account-service:
      base-url: http://localhost:8081
      timeout: 5000
    customer-service:
      base-url: http://localhost:8082
      timeout: 5000
    pricing-service:
      base-url: http://localhost:8083
      timeout: 5000
    transaction-service:
      base-url: http://localhost:8084
      timeout: 5000

# WebClient Configuration
webclient:
  max-connections: 500
  max-pending-acquires: 1000
  connect-timeout: 5000
  read-timeout: 10000

# Circuit Breaker Configuration
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
    instances:
      accountService:
        baseConfig: default
      customerService:
        baseConfig: default
      pricingService:
        baseConfig: default

  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
    instances:
      accountService:
        baseConfig: default
      customerService:
        baseConfig: default

# Caching Configuration
cache:
  default-ttl: 1800  # 30 minutes in seconds
  enabled: true

# Logging
logging:
  level:
    root: INFO
    com.documenthub: DEBUG
    org.springframework.r2dbc: DEBUG
    io.r2dbc.postgresql.QUERY: DEBUG
    reactor.netty.http.client: DEBUG

# Application-specific settings
app:
  extraction:
    max-steps: 10
    default-timeout: 5000
    parallel-execution-enabled: true
  matching:
    max-results-per-template: 100
  pagination:
    default-page-size: 20
    max-page-size: 100
```

### Task 1.3: Create application-mock.yml for Testing

**File**: `./poc/doc-hub-poc/src/main/resources/application-mock.yml`

```yaml
integration:
  services:
    account-service:
      base-url: http://localhost:9001
      timeout: 3000
    customer-service:
      base-url: http://localhost:9002
      timeout: 3000
    pricing-service:
      base-url: http://localhost:9003
      timeout: 3000

spring:
  r2dbc:
    url: r2dbc:h2:mem:///testdb
    username: sa
    password:

  data:
    redis:
      host: localhost
      port: 6380  # Different port for test Redis

cache:
  enabled: false  # Disable cache for testing
```

---

## Phase 2: Entity Classes

### Task 2.1: Create MasterTemplateDefinitionEntity

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/entity/MasterTemplateDefinitionEntity.java`

```java
package io.swagger.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("master_template_definition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterTemplateDefinitionEntity {

    @Id
    @Column("master_template_id")
    private UUID masterTemplateId;

    @Column("template_version")
    private Integer templateVersion;

    @Column("legacy_template_id")
    private String legacyTemplateId;

    @Column("legacy_template_name")
    private String legacyTemplateName;

    @Column("display_name")
    private String displayName;

    @Column("template_name")
    private String templateName;

    @Column("template_description")
    private String templateDescription;

    @Column("template_category")
    private String templateCategory;

    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("template_type")
    private String templateType;

    @Column("language_code")
    private String languageCode;

    @Column("owning_dept")
    private String owningDept;

    @Column("notification_needed")
    private Boolean notificationNeeded;

    @Column("is_active")
    private Boolean isActive;

    @Column("is_regulatory")
    private Boolean isRegulatory;

    @Column("is_message_center_doc")
    private Boolean isMessageCenterDoc;

    @Column("is_shared_document")
    private Boolean isSharedDocument;

    @Column("sharing_scope")
    private String sharingScope;

    @Column("data_extraction_config")
    private JsonNode dataExtractionConfig;

    @Column("access_control")
    private JsonNode accessControl;

    @Column("channels")
    private JsonNode channels;

    @Column("required_fields")
    private JsonNode requiredFields;

    @Column("template_config")
    private JsonNode templateConfig;

    @Column("start_date")
    private Long startDate;

    @Column("end_date")
    private Long endDate;

    @Column("created_by")
    private String createdBy;

    @Column("created_timestamp")
    private LocalDateTime createdTimestamp;

    @Column("update_by")
    private String updateBy;

    @Column("updated_timestamp")
    private LocalDateTime updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private Long recordStatus;
}
```

### Task 2.2: Create StorageIndexEntity

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/entity/StorageIndexEntity.java`

```java
package io.swagger.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("storage_index")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageIndexEntity {

    @Id
    @Column("storage_index_id")
    private UUID storageIndexId;

    @Column("master_template_id")
    private UUID masterTemplateId;

    @Column("template_version")
    private Integer templateVersion;

    @Column("template_type")
    private String templateType;

    @Column("reference_key")
    private String referenceKey;

    @Column("reference_key_type")
    private String referenceKeyType;

    @Column("is_shared")
    private Boolean isShared;

    @Column("account_key")
    private UUID accountKey;

    @Column("customer_key")
    private UUID customerKey;

    @Column("storage_vendor")
    private String storageVendor;

    @Column("storage_document_key")
    private UUID storageDocumentKey;

    @Column("generation_vendor")
    private String generationVendor;

    @Column("file_name")
    private String fileName;

    @Column("doc_creation_date")
    private Long docCreationDate;

    @Column("is_accessible")
    private Integer isAccessible;

    @Column("doc_metadata")
    private JsonNode docMetadata;

    @Column("created_by")
    private String createdBy;

    @Column("created_timestamp")
    private LocalDateTime createdTimestamp;

    @Column("update_by")
    private String updateBy;

    @Column("updated_timestamp")
    private LocalDateTime updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private Long recordStatus;
}
```

---

## Phase 3: Configuration Models (POJOs)

### Task 3.1: Create Data Extraction Configuration Models

**Directory**: `./poc/doc-hub-poc/src/main/java/io/swagger/model/config/`

Create the following classes:

1. **DataExtractionConfig.java**
2. **DataSourceConfig.java**
3. **EndpointConfig.java**
4. **CacheConfig.java**
5. **ResponseMapping.java**
6. **NextCall.java**
7. **Condition.java**
8. **DocumentMatchingStrategy.java**
9. **ExecutionRules.java**
10. **ErrorHandlingConfig.java**

Refer to **ARCHITECTURE.md** Section "Configuration Schema" for complete structure.

### Task 3.2: Create Access Control Models

**Directory**: `./poc/doc-hub-poc/src/main/java/io/swagger/model/access/`

Create the following classes:

1. **AccessControl.java**
2. **EligibilityCriteria.java**
3. **RuleGroup.java**

### Task 3.3: Create Extraction Context

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/model/context/ExtractionContext.java`

```java
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
```

---

## Phase 4: Repository Layer

### Task 4.1: Create MasterTemplateDefinitionRepository

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/repository/MasterTemplateDefinitionRepository.java`

```java
package io.swagger.repository;

import io.swagger.entity.MasterTemplateDefinitionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MasterTemplateDefinitionRepository
    extends R2dbcRepository<MasterTemplateDefinitionEntity, UUID> {

    Flux<MasterTemplateDefinitionEntity> findByIsActiveTrue();

    Flux<MasterTemplateDefinitionEntity> findByTemplateType(String templateType);

    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_active = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplates(Long currentDate);

    @Query("SELECT * FROM master_template_definition " +
           "WHERE master_template_id = :templateId " +
           "AND template_version = :version")
    Mono<MasterTemplateDefinitionEntity> findByIdAndVersion(
        UUID templateId,
        Integer version
    );
}
```

### Task 4.2: Create StorageIndexRepository

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/repository/StorageIndexRepository.java`

```java
package io.swagger.repository;

import io.swagger.entity.StorageIndexEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface StorageIndexRepository
    extends R2dbcRepository<StorageIndexEntity, UUID> {

    Flux<StorageIndexEntity> findByAccountKey(UUID accountKey);

    Flux<StorageIndexEntity> findByCustomerKey(UUID customerKey);

    @Query("SELECT * FROM storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion")
    Flux<StorageIndexEntity> findByReferenceKey(
        String referenceKey,
        String referenceKeyType,
        String templateType,
        Integer templateVersion
    );

    @Query("SELECT * FROM storage_index " +
           "WHERE template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND is_shared = true")
    Flux<StorageIndexEntity> findSharedDocuments(
        String templateType,
        Integer templateVersion
    );

    @Query("SELECT * FROM storage_index " +
           "WHERE template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND doc_metadata @> cast(:metadataJson as jsonb)")
    Flux<StorageIndexEntity> findByMetadataFields(
        String templateType,
        Integer templateVersion,
        String metadataJson
    );
}
```

---

## Phase 5: Core Services

### Task 5.1: WebClient Configuration

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/config/WebClientConfig.java`

```java
package io.swagger.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${webclient.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${webclient.read-timeout:10000}")
    private int readTimeout;

    @Value("${webclient.max-connections:500}")
    private int maxConnections;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
            .responseTimeout(Duration.ofMillis(readTimeout))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
            );

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
```

### Task 5.2: JsonPath Extractor Utility

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/util/JsonPathExtractor.java`

```java
package io.swagger.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JsonPathExtractor {

    private static final Configuration CONFIG = Configuration.builder()
        .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)
        .build();

    public Object extract(String json, String jsonPathExpression) {
        try {
            return JsonPath.using(CONFIG).parse(json).read(jsonPathExpression);
        } catch (Exception e) {
            log.warn("Failed to extract using JSONPath: {}", jsonPathExpression, e);
            return null;
        }
    }
}
```

### Task 5.3: Placeholder Resolver Utility

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/util/PlaceholderResolver.java`

```java
package io.swagger.util;

import io.swagger.model.context.ExtractionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PlaceholderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public String resolve(String template, ExtractionContext context) {
        if (template == null) {
            return null;
        }

        String resolved = template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = context.getVariables().get(placeholder);

            if (value != null) {
                resolved = resolved.replace("${" + placeholder + "}", value.toString());
                log.debug("Resolved placeholder {} to {}", placeholder, value);
            } else {
                log.warn("Placeholder {} not found in context", placeholder);
            }
        }

        return resolved;
    }

    public Map<String, Object> resolveMap(
        Map<String, String> templateMap,
        ExtractionContext context
    ) {
        Map<String, Object> resolved = new java.util.HashMap<>();

        templateMap.forEach((key, template) -> {
            String resolvedValue = resolve(template, context);
            resolved.put(key, resolvedValue);
        });

        return resolved;
    }
}
```

### Task 5.4: Data Extraction Engine

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/service/DataExtractionEngine.java`

This is the core service. Refer to **ARCHITECTURE.md** Section "Implementation Components" > "DataExtractionEngine" for complete implementation.

Key methods to implement:
- `executeExtractionStrategy()`
- `executeDataSource()`
- `resolvePlaceholders()`
- `extractDataFromResponse()`
- `triggerNextCalls()`
- `evaluateCondition()`

### Task 5.5: Rule Evaluation Service

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/service/RuleEvaluationService.java`

Implement eligibility rule evaluation logic.

### Task 5.6: Document Matching Service

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/service/DocumentMatchingService.java`

Implement document matching strategies.

### Task 5.7: Document Enquiry Service

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/service/DocumentEnquiryService.java`

Main orchestration service that ties everything together.

---

## Phase 6: Controller Integration

### Task 6.1: Update DocumentsEnquiryApiController

**File**: `./poc/doc-hub-poc/src/main/java/io/swagger/api/DocumentsEnquiryApiController.java`

Replace the mock implementation with actual service calls:

```java
@RestController
@Slf4j
public class DocumentsEnquiryApiController implements DocumentsEnquiryApi {

    @Autowired
    private DocumentEnquiryService documentEnquiryService;

    @Override
    public ResponseEntity<DocumentRetrievalResponse> documentsEnquiryPost(
        @RequestHeader("X-version") Integer xVersion,
        @RequestHeader("X-correlation-id") String xCorrelationId,
        @RequestHeader("X-requestor-id") UUID xRequestorId,
        @RequestHeader("X-requestor-type") XRequestorType xRequestorType,
        @Valid @RequestBody DocumentListRequest body
    ) {
        log.info("Received document enquiry request. CorrelationId: {}", xCorrelationId);

        return documentEnquiryService.getDocuments(body, xCorrelationId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .block(); // Or use Mono<ResponseEntity> if controller supports reactive
    }
}
```

---

## Phase 7: Sample Data Creation

### Task 7.1: Create Sample Templates SQL

**File**: `./poc/sample-data/templates.sql`

Create INSERT statements for:
1. Simple Statement template
2. Privacy Policy template (location-based)
3. Cardholder Agreement template (multi-step)
4. VIP Customer Letter template

### Task 7.2: Create Sample Documents SQL

**File**: `./poc/sample-data/documents.sql`

Create INSERT statements for documents in `storage_index` table.

### Task 7.3: Create Sample Test Data

**File**: `./poc/sample-data/test-scenarios.md`

Document test scenarios with:
- Input requests
- Expected extraction flow
- Expected documents returned

---

## Phase 8: Testing

### Task 8.1: Unit Tests

Create unit tests for:
- `JsonPathExtractor`
- `PlaceholderResolver`
- `RuleEvaluationService`
- Individual rule evaluators

### Task 8.2: Integration Tests

Create integration tests for:
- DataExtractionEngine with mock WebClient
- DocumentMatchingService with embedded database
- End-to-end flow with test data

### Task 8.3: Mock External Services

**File**: `./poc/doc-hub-poc/src/test/java/io/swagger/mock/MockExternalServices.java`

Use MockWebServer to simulate external API responses.

---

## Phase 9: Documentation

### Task 9.1: API Documentation

Ensure Swagger UI is accessible at `/swagger-ui.html`

### Task 9.2: Configuration Examples

Create example JSON configurations for common scenarios.

### Task 9.3: Troubleshooting Guide

Document common issues and solutions.

---

## Implementation Checklist

### Phase 1: Foundation
- [ ] Add all dependencies to pom.xml
- [ ] Create application.yml
- [ ] Create application-mock.yml
- [ ] Test database connectivity

### Phase 2: Entities
- [ ] Create MasterTemplateDefinitionEntity
- [ ] Create StorageIndexEntity
- [ ] Verify table mappings

### Phase 3: Configuration Models
- [ ] Create all data extraction config POJOs
- [ ] Create access control POJOs
- [ ] Create ExtractionContext

### Phase 4: Repositories
- [ ] Create MasterTemplateDefinitionRepository
- [ ] Create StorageIndexRepository
- [ ] Write repository tests

### Phase 5: Core Services
- [ ] Implement WebClientConfig
- [ ] Implement JsonPathExtractor
- [ ] Implement PlaceholderResolver
- [ ] Implement DataExtractionEngine
- [ ] Implement RuleEvaluationService
- [ ] Implement DocumentMatchingService
- [ ] Implement DocumentEnquiryService

### Phase 6: Controller
- [ ] Update DocumentsEnquiryApiController
- [ ] Test endpoint with Postman/curl

### Phase 7: Sample Data
- [ ] Create sample templates
- [ ] Create sample documents
- [ ] Load data into database

### Phase 8: Testing
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Create mock services
- [ ] Run all tests

### Phase 9: Documentation
- [ ] Update API documentation
- [ ] Create configuration examples
- [ ] Write troubleshooting guide

---

## Testing Commands

### Build Project
```bash
cd ./poc/doc-hub-poc
mvn clean install
```

### Run Application
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

### Run with Mock Profile
```bash
mvn spring-boot:run -Dspring.profiles.active=mock
```

---

## Next Steps After POC

1. **Performance Testing**: Load test with realistic data volumes
2. **Security Hardening**: Add authentication, authorization
3. **Monitoring**: Add metrics, distributed tracing
4. **Admin UI**: Build configuration management UI
5. **Production Deployment**: Containerize, deploy to cloud

---

**Document Version**: 1.0
**Last Updated**: 2025-11-24
