# Document Hub API - JIRA Tickets for Company Implementation

**Project:** Document Hub API
**Created:** 2025-11-09
**Purpose:** Comprehensive breakdown of work completed in prototype and tasks needed for company implementation
**Compliance Note:** This breakdown is based on prototype work that cannot be used due to compliance requirements

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Work Already Completed (Reference Only)](#work-already-completed-reference-only)
3. [Epic Structure](#epic-structure)
4. [Sprint Planning](#sprint-planning)
5. [Detailed User Stories](#detailed-user-stories)
6. [Implementation Roadmap](#implementation-roadmap)

---

## Project Overview

### Background
A prototype Document Hub API was developed with the following capabilities:
- Document enquiry endpoints for retrieving account-specific and shared documents
- Template management with dynamic configuration
- Generic data extraction engine for custom document eligibility rules
- Multi-vendor support for print and email services

**IMPORTANT:** Due to compliance requirements, the prototype code cannot be directly used. These JIRA tickets describe the functionality that was validated in the prototype and needs to be re-implemented following company standards.

### Business Value
- Unified document retrieval API for all customer-facing channels
- Dynamic document selection based on account type and eligibility rules
- Flexible vendor management for document delivery
- Scalable architecture supporting multiple lines of business

### Technical Stack (Recommended)
- Backend: Spring Boot 3.x with WebFlux (Reactive)
- Database: PostgreSQL 16+ with JSONB support
- API: REST with OpenAPI 3.0 specification
- Migration: Flyway
- Caching: Redis (for shared documents)
- Monitoring: Micrometer + Prometheus

---

## Work Already Completed (Reference Only)

### Database Schema Design ✅
**What Was Done:**
- Designed `master_template_definition` table with composite primary key (template_id, version)
- Added `storage_index` table for document metadata
- Implemented JSONB columns for flexible configuration:
  - `doc_supporting_data` - Access control, validation rules, retention policies
  - `template_config` - Vendor preferences, failover settings, upload configuration
  - `data_extraction_schema` - Dynamic data extraction rules for custom eligibility
- Created indexes for performance optimization
- Established foreign key relationships

**Reference Documents:**
- Database schema documentation
- Migration scripts (V1 and V2)

---

### Template Configuration Feature ✅
**What Was Done:**
- Added `template_config` JSONB column to store operational configurations
- Supported configurations:
  - `defaultPrintVendor`: SMARTCOMM, ASSENTIS, HANDLEBAR, CUSTOM
  - `defaultEmailVendor`: SENDGRID, MAILGUN, AWS_SES, CUSTOM
  - `printVendorFailover`: Failover actions and secondary vendor
  - `uploadReferenceKeyField`: Dynamic reference key mapping
- Created GIN indexes for efficient JSONB queries
- Validated all JSONB operations and query patterns

**Test Coverage:**
- Insert operations with JSONB data
- Nested field extraction
- Index usage validation
- Use case scenarios (printing, email, failover, upload)

---

### Document Enquiry API ✅
**What Was Done:**
- Implemented POST /api/test/documents-enquiry endpoint
- Features:
  - Retrieve account-specific documents filtered by accountId and customerId
  - Retrieve shared documents with multiple sharing scopes:
    - `all` - Available to all customers
    - `credit_card_account_only` - Available only to credit card accounts
    - `custom_rule` - Dynamic eligibility based on data extraction
  - Merge and sort documents by date posted
  - Return summary statistics
- Performance: < 100ms response time for 50+ documents

**Test Results:**
- 6 documents retrieved correctly (4 account-specific + 2 shared)
- Summary statistics accurate
- Response format validated
- Database queries optimized

---

### Generic Data Extraction Engine ✅
**What Was Done:**
- Designed reactive extraction engine for custom document eligibility
- Support for multiple data sources:
  - REST API calls (customer service, account service, transaction service)
  - Database queries
  - Cache lookups
- Extraction flow:
  1. Parse `data_extraction_schema` from template
  2. Execute API calls in parallel with circuit breaker
  3. Apply transformations and validations
  4. Evaluate conditional logic
  5. Return eligibility result
- Error handling with retry policies and fallback strategies

**Capabilities Validated:**
- Multi-step API orchestration
- Field mapping and transformation
- Conditional logic evaluation
- Error recovery

---

### API Specification ✅
**What Was Done:**
- Created OpenAPI 3.0 specification
- Documented endpoints:
  - POST /documents-enquiry - Document retrieval
  - POST /documents/upload - Document upload
  - POST /templates - Template creation
  - GET /documents/{documentId} - Document download
- Request/response schemas
- Error response formats
- Postman collection with 6 endpoint categories

**Validation:**
- Identified discrepancies between spec and implementation
- Documented 16 pending tasks for full compliance
- Estimated effort: 27.5-43 hours

---

### Testing & Documentation ✅
**What Was Done:**
- Database-level SQL testing
- REST endpoint testing
- API specification validation
- Sequence diagrams for all major flows
- Performance benchmarking
- Test data creation and validation

**Test Coverage:**
- Happy path scenarios
- Edge cases (null handling, empty results)
- Performance testing
- Integration testing

---

## Epic Structure

```
EPIC-DOC-001: Document Hub Core Infrastructure
├── EPIC-DOC-001.1: Database Schema & Migrations
├── EPIC-DOC-001.2: Entity Models & Repositories
└── EPIC-DOC-001.3: Base Configuration & Security

EPIC-DOC-002: Template Management System
├── EPIC-DOC-002.1: Template CRUD Operations
├── EPIC-DOC-002.2: Template Configuration Management
└── EPIC-DOC-002.3: Template Versioning

EPIC-DOC-003: Document Enquiry & Retrieval
├── EPIC-DOC-003.1: Basic Document Enquiry API
├── EPIC-DOC-003.2: Shared Document Eligibility
├── EPIC-DOC-003.3: API Spec Compliance
└── EPIC-DOC-003.4: Performance & Caching

EPIC-DOC-004: Generic Data Extraction Engine
├── EPIC-DOC-004.1: Extraction Schema Parser
├── EPIC-DOC-004.2: Multi-Source Data Orchestration
├── EPIC-DOC-004.3: Rule Evaluation Engine
└── EPIC-DOC-004.4: Error Handling & Resilience

EPIC-DOC-005: Production Readiness
├── EPIC-DOC-005.1: Security & Authentication
├── EPIC-DOC-005.2: Monitoring & Observability
├── EPIC-DOC-005.3: Error Handling & Validation
└── EPIC-DOC-005.4: Performance Optimization
```

---

## Sprint Planning

### Recommended Sprint Structure (2-week sprints)

**Sprint 1-2: Foundation**
- Database schema
- Entity models
- Base configuration
- **Effort:** 20-25 hours

**Sprint 3-4: Template Management**
- Template CRUD
- Configuration management
- Versioning
- **Effort:** 18-22 hours

**Sprint 5-7: Document Enquiry**
- Basic retrieval API
- Shared document logic
- API compliance
- **Effort:** 35-45 hours

**Sprint 8-10: Data Extraction Engine**
- Schema parser
- Data orchestration
- Rule evaluation
- **Effort:** 40-50 hours

**Sprint 11-12: Production Readiness**
- Security
- Monitoring
- Error handling
- Testing
- **Effort:** 25-30 hours

**Total Estimated Effort:** 138-172 hours (17-21 developer days)

---

# Detailed User Stories

---

## EPIC-DOC-001: Document Hub Core Infrastructure

### EPIC-DOC-001.1: Database Schema & Migrations

---

## STORY-001: Design and Implement Database Schema

**Type:** Story
**Priority:** Critical
**Story Points:** 8 SP
**Estimated Hours:** 6-8 hours
**Assignee:** Senior Backend Developer + DBA
**Sprint:** Sprint 1

### User Story
```
As a platform architect
I want a normalized database schema for document and template management
So that we can efficiently store and retrieve document metadata with flexibility for future changes
```

### Description
Design and implement the core database schema including:
1. `master_template_definition` - Template metadata and configuration
2. `storage_index` - Document storage metadata and references
3. Supporting tables (if needed)

### Technical Requirements

**Table 1: master_template_definition**
```sql
CREATE TABLE master_template_definition (
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    legacy_template_id VARCHAR(100),
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    line_of_business VARCHAR(50),
    category VARCHAR(100),
    doc_type VARCHAR(100),
    language_code VARCHAR(10),
    owning_dept VARCHAR(100),
    notification_needed BOOLEAN DEFAULT false,

    -- JSONB configuration fields
    doc_supporting_data JSONB,  -- Access control, validation rules
    template_config JSONB,       -- Vendor preferences, upload settings
    data_extraction_schema JSONB, -- Custom eligibility rules

    -- Shared document fields
    is_shared_document BOOLEAN DEFAULT false,
    sharing_scope VARCHAR(50),  -- 'all', 'credit_card_account_only', 'custom_rule'

    -- Template metadata
    is_regulatory BOOLEAN DEFAULT false,
    is_message_center_doc BOOLEAN DEFAULT false,
    document_channel JSONB,
    template_variables JSONB,
    template_status VARCHAR(20),
    effective_date BIGINT,
    valid_until BIGINT,

    -- Audit fields (DBA standard)
    created_by VARCHAR(100) NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_timestamp TIMESTAMP,
    archive_indicator BOOLEAN DEFAULT false,
    archive_timestamp TIMESTAMP,
    version_number BIGINT DEFAULT 1,
    record_status VARCHAR(20) DEFAULT 'ACTIVE',

    PRIMARY KEY (template_id, version)
);

-- Indexes
CREATE INDEX idx_template_name ON master_template_definition(template_name);
CREATE INDEX idx_doc_type ON master_template_definition(doc_type);
CREATE INDEX idx_category ON master_template_definition(category);
CREATE INDEX idx_effective_date ON master_template_definition(effective_date);
CREATE INDEX idx_shared_documents ON master_template_definition(is_shared_document, sharing_scope)
    WHERE archive_indicator = false;

-- GIN indexes for JSONB
CREATE INDEX idx_template_config_print_vendor ON master_template_definition
    USING GIN ((template_config -> 'defaultPrintVendor'));
CREATE INDEX idx_template_config_email_vendor ON master_template_definition
    USING GIN ((template_config -> 'defaultEmailVendor'));
```

**Table 2: storage_index**
```sql
CREATE TABLE storage_index (
    storage_index_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,

    -- Account/Customer references
    account_key UUID NOT NULL,
    customer_key UUID NOT NULL,

    -- Document metadata
    doc_type VARCHAR(100) NOT NULL,
    doc_creation_date TIMESTAMP NOT NULL,
    mime_type VARCHAR(100),
    file_size_bytes BIGINT,

    -- Storage reference
    storage_location VARCHAR(500), -- S3 path, file path, etc.
    storage_type VARCHAR(50),      -- 'S3', 'SFTP', 'DATABASE'

    -- Document metadata (flexible)
    doc_metadata JSONB,

    -- Access control
    is_accessible BOOLEAN DEFAULT true,
    access_level VARCHAR(50),

    -- Audit fields
    created_by VARCHAR(100) NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_timestamp TIMESTAMP,
    archive_indicator BOOLEAN DEFAULT false,
    archive_timestamp TIMESTAMP,

    FOREIGN KEY (template_id, template_version)
        REFERENCES master_template_definition(template_id, version)
);

-- Indexes
CREATE INDEX idx_account_customer ON storage_index(account_key, customer_key, doc_creation_date DESC);
CREATE INDEX idx_template_ref ON storage_index(template_id, template_version);
CREATE INDEX idx_doc_creation_date ON storage_index(doc_creation_date DESC);
CREATE INDEX idx_doc_type_storage ON storage_index(doc_type);
CREATE INDEX idx_accessible_docs ON storage_index(account_key, customer_key)
    WHERE archive_indicator = false AND is_accessible = true;
```

### Acceptance Criteria
- [ ] Tables created with all required columns
- [ ] Primary keys and foreign keys established
- [ ] All indexes created for performance
- [ ] GIN indexes created for JSONB fields
- [ ] Audit fields follow company DBA standards
- [ ] Migration script follows company naming convention (e.g., Flyway V1__initial_schema.sql)
- [ ] Migration tested on DEV environment
- [ ] Rollback script created and tested
- [ ] Schema documentation updated
- [ ] Peer review completed by DBA

### Testing
```sql
-- Test 1: Insert template
INSERT INTO master_template_definition (
    template_id, version, template_name, doc_type,
    category, created_by, template_config
) VALUES (
    gen_random_uuid(), 1, 'Test Template', 'disclosure',
    'Privacy Policy', 'system',
    '{"defaultPrintVendor": "SMARTCOMM"}'::jsonb
);

-- Test 2: Query with JSONB
SELECT template_name, template_config->'defaultPrintVendor'
FROM master_template_definition
WHERE template_config->'defaultPrintVendor' = '"SMARTCOMM"'::jsonb;

-- Test 3: Insert document
INSERT INTO storage_index (
    template_id, template_version, account_key, customer_key,
    doc_type, doc_creation_date, created_by
) VALUES (
    (SELECT template_id FROM master_template_definition LIMIT 1),
    1, gen_random_uuid(), gen_random_uuid(),
    'disclosure', CURRENT_TIMESTAMP, 'system'
);

-- Test 4: Join query
SELECT si.storage_index_id, mt.template_name, si.doc_creation_date
FROM storage_index si
JOIN master_template_definition mt
    ON si.template_id = mt.template_id
    AND si.template_version = mt.version
WHERE si.archive_indicator = false;
```

### Dependencies
- DBA approval for schema design
- Environment setup (PostgreSQL 16+)
- Flyway configuration in application

### Related Documentation
- Database schema documentation (prototype reference)
- Company DBA standards
- Flyway migration guide

---

## STORY-002: Create Flyway Migration Scripts

**Type:** Task
**Priority:** Critical
**Story Points:** 2 SP
**Estimated Hours:** 1-2 hours
**Assignee:** Backend Developer
**Sprint:** Sprint 1

### User Story
```
As a DevOps engineer
I want automated database migrations using Flyway
So that schema changes are version-controlled and consistently applied across environments
```

### Description
Set up Flyway for database version control and create initial migration scripts.

### Technical Details

**pom.xml (Maven) or build.gradle (Gradle)**
```xml
<!-- Maven -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**application.yml**
```yaml
spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://localhost:5432/documenthub
    user: ${DB_USER}
    password: ${DB_PASSWORD}
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
```

**Migration Files:**
- `src/main/resources/db/migration/V1__initial_schema.sql`
- `src/main/resources/db/migration/V2__add_template_config.sql` (if needed as separate migration)
- `src/main/resources/db/migration/R__seed_test_data.sql` (repeatable migration for test data)

### Acceptance Criteria
- [ ] Flyway dependency added to build file
- [ ] Flyway configured in application.yml
- [ ] Migration scripts created following Flyway naming convention (V{version}__{description}.sql)
- [ ] Migrations execute successfully on clean database
- [ ] Migrations are idempotent (can run multiple times safely)
- [ ] Rollback scripts created (V{version}__rollback_{description}.sql)
- [ ] Migration history tracked in `flyway_schema_history` table
- [ ] Documentation updated with migration process

### Testing
```bash
# Test migration
mvn flyway:migrate

# Verify schema
mvn flyway:info

# Test rollback
mvn flyway:undo
```

### Dependencies
- STORY-001 (database schema design)
- Database credentials configured

---

## STORY-003: Implement Entity Models with R2DBC

**Type:** Story
**Priority:** High
**Story Points:** 5 SP
**Estimated Hours:** 4-5 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 1

### User Story
```
As a backend developer
I want JPA/R2DBC entity models for database tables
So that I can interact with the database using object-oriented code
```

### Description
Create entity classes for `master_template_definition` and `storage_index` tables with proper annotations for reactive R2DBC.

### Technical Details

**MasterTemplateDefinition.java**
```java
package com.company.documenthub.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("master_template_definition")
public class MasterTemplateDefinition {

    @Id
    @Column("template_id")
    private UUID templateId;

    @Column("version")
    private Integer version;

    @Column("template_name")
    private String templateName;

    @Column("description")
    private String description;

    @Column("line_of_business")
    private String lineOfBusiness;

    @Column("category")
    private String category;

    @Column("doc_type")
    private String docType;

    @Column("language_code")
    private String languageCode;

    @Column("owning_dept")
    private String owningDept;

    @Column("notification_needed")
    private Boolean notificationNeeded;

    // JSONB fields
    @Column("doc_supporting_data")
    private JsonNode docSupportingData;

    @Column("template_config")
    private JsonNode templateConfig;

    @Column("data_extraction_schema")
    private JsonNode dataExtractionSchema;

    // Shared document fields
    @Column("is_shared_document")
    private Boolean isSharedDocument;

    @Column("sharing_scope")
    private String sharingScope;

    // Template metadata
    @Column("is_regulatory")
    private Boolean isRegulatory;

    @Column("is_message_center_doc")
    private Boolean isMessageCenterDoc;

    @Column("document_channel")
    private JsonNode documentChannel;

    @Column("template_variables")
    private JsonNode templateVariables;

    @Column("template_status")
    private String templateStatus;

    @Column("effective_date")
    private Long effectiveDate;

    @Column("valid_until")
    private Long validUntil;

    // Audit fields
    @Column("created_by")
    private String createdBy;

    @Column("created_timestamp")
    private LocalDateTime createdTimestamp;

    @Column("updated_by")
    private String updatedBy;

    @Column("updated_timestamp")
    private LocalDateTime updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("archive_timestamp")
    private LocalDateTime archiveTimestamp;

    @Column("version_number")
    private Long versionNumber;

    @Column("record_status")
    private String recordStatus;
}
```

**StorageIndex.java**
```java
package com.company.documenthub.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("storage_index")
public class StorageIndex {

    @Id
    @Column("storage_index_id")
    private UUID storageIndexId;

    @Column("template_id")
    private UUID templateId;

    @Column("template_version")
    private Integer templateVersion;

    @Column("account_key")
    private UUID accountKey;

    @Column("customer_key")
    private UUID customerKey;

    @Column("doc_type")
    private String docType;

    @Column("doc_creation_date")
    private LocalDateTime docCreationDate;

    @Column("mime_type")
    private String mimeType;

    @Column("file_size_bytes")
    private Long fileSizeBytes;

    @Column("storage_location")
    private String storageLocation;

    @Column("storage_type")
    private String storageType;

    @Column("doc_metadata")
    private JsonNode docMetadata;

    @Column("is_accessible")
    private Boolean isAccessible;

    @Column("access_level")
    private String accessLevel;

    // Audit fields
    @Column("created_by")
    private String createdBy;

    @Column("created_timestamp")
    private LocalDateTime createdTimestamp;

    @Column("updated_by")
    private String updatedBy;

    @Column("updated_timestamp")
    private LocalDateTime updatedTimestamp;

    @Column("archive_indicator")
    private Boolean archiveIndicator;

    @Column("archive_timestamp")
    private LocalDateTime archiveTimestamp;
}
```

**R2DBC Configuration**
```java
package com.company.documenthub.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

@Configuration
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {

    @Override
    public ConnectionFactory connectionFactory() {
        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .port(5432)
                .database("documenthub")
                .username("user")
                .password("password")
                .codecRegistrar((connection, allocator, registry) -> {
                    // Register JSONB codec
                    return registry.register(Json.CODEC);
                })
                .build()
        );
    }
}
```

### Acceptance Criteria
- [ ] Entity classes created for both tables
- [ ] All columns mapped with @Column annotations
- [ ] JsonNode used for JSONB fields
- [ ] Lombok annotations used (@Data, @Builder, etc.)
- [ ] R2DBC configuration properly set up
- [ ] JSONB codec registered
- [ ] Unit tests created for entity mapping
- [ ] Javadoc comments added
- [ ] Code follows company coding standards

### Testing
```java
@Test
void testMasterTemplateDefinitionEntity() {
    MasterTemplateDefinition template = new MasterTemplateDefinition();
    template.setTemplateId(UUID.randomUUID());
    template.setVersion(1);
    template.setTemplateName("Test Template");

    // Test JSONB field
    ObjectMapper mapper = new ObjectMapper();
    JsonNode config = mapper.createObjectNode()
        .put("defaultPrintVendor", "SMARTCOMM");
    template.setTemplateConfig(config);

    assertNotNull(template.getTemplateId());
    assertEquals("SMARTCOMM",
        template.getTemplateConfig().get("defaultPrintVendor").asText());
}
```

### Dependencies
- STORY-001 (database schema)
- Spring Data R2DBC dependency
- Jackson for JSON handling

### Known Issues
**R2DBC Composite Key Limitation:**
- R2DBC doesn't support multiple @Id annotations
- Workaround: Use template_id as @Id, version as regular column
- Use DatabaseClient for queries requiring composite key

---

## STORY-004: Configure Spring Security and Authentication

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 6-8 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 1-2

### User Story
```
As a security architect
I want all API endpoints protected with authentication and authorization
So that only authorized users can access document data
```

### Description
Implement Spring Security with JWT token-based authentication for all document hub endpoints.

### Technical Requirements

1. **JWT Token Validation**
   - Validate JWT tokens from company SSO/OAuth provider
   - Extract user roles and permissions
   - Support service-to-service authentication

2. **Role-Based Access Control (RBAC)**
   - Roles: CUSTOMER, BACK_OFFICE, ADMIN, AGENT
   - Map permissions to endpoints
   - Customer can only access their own documents

3. **Security Configuration**
   - Secure all endpoints except health check
   - CORS configuration
   - CSRF protection for non-REST endpoints

### Technical Details

**SecurityConfig.java**
```java
package com.company.documenthub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/api/v1/documents-enquiry").hasAnyRole("CUSTOMER", "BACK_OFFICE", "ADMIN")
                .pathMatchers("/api/v1/documents/upload").hasAnyRole("SYSTEM", "ADMIN")
                .pathMatchers("/api/v1/templates/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Configure JWT decoder with company's OAuth provider
        return NimbusReactiveJwtDecoder.withJwkSetUri("https://company-oauth.com/.well-known/jwks.json").build();
    }
}
```

**Customer Ownership Validation**
```java
@Component
public class DocumentAccessValidator {

    public Mono<Boolean> canAccessDocument(UUID customerId, UUID accountId, Authentication auth) {
        String authenticatedCustomerId = extractCustomerId(auth);

        // Customers can only access their own documents
        if (hasRole(auth, "CUSTOMER")) {
            return Mono.just(customerId.toString().equals(authenticatedCustomerId));
        }

        // Back office and admin can access any document
        if (hasRole(auth, "BACK_OFFICE") || hasRole(auth, "ADMIN")) {
            return Mono.just(true);
        }

        return Mono.just(false);
    }
}
```

### Acceptance Criteria
- [ ] Spring Security configured with WebFlux
- [ ] JWT token validation working
- [ ] Role-based access control implemented
- [ ] Customer can only access own documents
- [ ] Back office can access all documents
- [ ] Admin has full access
- [ ] Unauthorized requests return 401
- [ ] Forbidden requests return 403
- [ ] Security audit logging implemented
- [ ] Unit tests for security configuration
- [ ] Integration tests for authentication flows

### Testing
```java
@Test
@WithMockUser(roles = "CUSTOMER")
void testCustomerCanAccessOwnDocuments() {
    webTestClient.post()
        .uri("/api/v1/documents-enquiry")
        .bodyValue(Map.of(
            "customerId", "123e4567-e89b-12d3-a456-426614174000",
            "accountId", "223e4567-e89b-12d3-a456-426614174000"
        ))
        .exchange()
        .expectStatus().isOk();
}

@Test
@WithMockUser(roles = "CUSTOMER")
void testCustomerCannotAccessOtherDocuments() {
    webTestClient.post()
        .uri("/api/v1/documents-enquiry")
        .bodyValue(Map.of(
            "customerId", "different-customer-id",
            "accountId", "different-account-id"
        ))
        .exchange()
        .expectStatus().isForbidden();
}
```

### Dependencies
- Company OAuth/SSO integration
- JWT library (Nimbus JOSE + JWT)

---

## EPIC-DOC-002: Template Management System

---

## STORY-005: Implement Template CRUD Operations

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 6-8 hours
**Assignee:** Mid-level Backend Developer
**Sprint:** Sprint 3

### User Story
```
As a template administrator
I want to create, read, update, and delete templates
So that I can manage document templates for all channels
```

### Description
Implement RESTful endpoints for template management following OpenAPI specification.

### Endpoints to Implement

1. **POST /api/v1/templates** - Create new template
2. **GET /api/v1/templates/{templateId}** - Get template by ID
3. **GET /api/v1/templates** - List templates with filtering
4. **PUT /api/v1/templates/{templateId}** - Update template
5. **DELETE /api/v1/templates/{templateId}** - Archive template (soft delete)

### Technical Details

**TemplateController.java**
```java
package com.company.documenthub.controller;

import com.company.documenthub.model.dto.CreateTemplateRequest;
import com.company.documenthub.model.dto.TemplateResponse;
import com.company.documenthub.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TemplateResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {
        return templateService.createTemplate(request);
    }

    @GetMapping("/{templateId}")
    public Mono<TemplateResponse> getTemplate(
            @PathVariable UUID templateId,
            @RequestParam(required = false) Integer version) {
        return templateService.getTemplate(templateId, version);
    }

    @GetMapping
    public Flux<TemplateResponse> listTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) Boolean isShared,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize) {
        return templateService.listTemplates(category, docType, isShared, pageNumber, pageSize);
    }

    @PutMapping("/{templateId}")
    public Mono<TemplateResponse> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateTemplateRequest request) {
        return templateService.updateTemplate(templateId, request);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTemplate(@PathVariable UUID templateId) {
        return templateService.archiveTemplate(templateId);
    }
}
```

**CreateTemplateRequest.java**
```java
package com.company.documenthub.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String templateName;

    private String description;

    @NotBlank(message = "Document type is required")
    private String docType;

    @NotBlank(message = "Category is required")
    private String category;

    private String lineOfBusiness;
    private String languageCode;
    private String owningDept;
    private Boolean notificationNeeded;

    // JSONB fields
    private JsonNode docSupportingData;
    private JsonNode templateConfig;
    private JsonNode dataExtractionSchema;

    // Shared document fields
    private Boolean isSharedDocument;
    private String sharingScope;

    // Template metadata
    private Boolean isRegulatory;
    private Boolean isMessageCenterDoc;
    private JsonNode documentChannel;
    private JsonNode templateVariables;
    private String templateStatus;
    private Long effectiveDate;
    private Long validUntil;
}
```

**TemplateService.java**
```java
package com.company.documenthub.service;

import com.company.documenthub.model.dto.CreateTemplateRequest;
import com.company.documenthub.model.dto.TemplateResponse;
import com.company.documenthub.model.entity.MasterTemplateDefinition;
import com.company.documenthub.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    @Transactional
    public Mono<TemplateResponse> createTemplate(CreateTemplateRequest request) {
        MasterTemplateDefinition template = new MasterTemplateDefinition();
        template.setTemplateId(UUID.randomUUID());
        template.setVersion(1);
        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setDocType(request.getDocType());
        template.setCategory(request.getCategory());
        template.setLineOfBusiness(request.getLineOfBusiness());
        template.setLanguageCode(request.getLanguageCode());
        template.setOwningDept(request.getOwningDept());
        template.setNotificationNeeded(request.getNotificationNeeded());

        // JSONB fields
        template.setDocSupportingData(request.getDocSupportingData());
        template.setTemplateConfig(request.getTemplateConfig());
        template.setDataExtractionSchema(request.getDataExtractionSchema());

        // Shared document fields
        template.setIsSharedDocument(request.getIsSharedDocument());
        template.setSharingScope(request.getSharingScope());

        // Template metadata
        template.setIsRegulatory(request.getIsRegulatory());
        template.setIsMessageCenterDoc(request.getIsMessageCenterDoc());
        template.setDocumentChannel(request.getDocumentChannel());
        template.setTemplateVariables(request.getTemplateVariables());
        template.setTemplateStatus(request.getTemplateStatus());
        template.setEffectiveDate(request.getEffectiveDate());
        template.setValidUntil(request.getValidUntil());

        // Audit fields
        template.setCreatedBy("system"); // Replace with authenticated user
        template.setCreatedTimestamp(LocalDateTime.now());
        template.setArchiveIndicator(false);
        template.setVersionNumber(1L);
        template.setRecordStatus("ACTIVE");

        return templateRepository.save(template)
            .map(this::toResponse);
    }

    public Mono<TemplateResponse> getTemplate(UUID templateId, Integer version) {
        if (version != null) {
            return templateRepository.findByTemplateIdAndVersion(templateId, version)
                .map(this::toResponse);
        }
        return templateRepository.findLatestByTemplateId(templateId)
            .map(this::toResponse);
    }

    public Flux<TemplateResponse> listTemplates(
            String category, String docType, Boolean isShared,
            int pageNumber, int pageSize) {
        // Implement filtering logic
        return templateRepository.findAllWithFilters(category, docType, isShared, pageNumber, pageSize)
            .map(this::toResponse);
    }

    @Transactional
    public Mono<TemplateResponse> updateTemplate(UUID templateId, CreateTemplateRequest request) {
        return templateRepository.findLatestByTemplateId(templateId)
            .flatMap(existing -> {
                // Create new version
                MasterTemplateDefinition newVersion = new MasterTemplateDefinition();
                newVersion.setTemplateId(existing.getTemplateId());
                newVersion.setVersion(existing.getVersion() + 1);
                // Copy updated fields from request
                // ...
                return templateRepository.save(newVersion);
            })
            .map(this::toResponse);
    }

    @Transactional
    public Mono<Void> archiveTemplate(UUID templateId) {
        return templateRepository.findLatestByTemplateId(templateId)
            .flatMap(template -> {
                template.setArchiveIndicator(true);
                template.setArchiveTimestamp(LocalDateTime.now());
                return templateRepository.save(template);
            })
            .then();
    }

    private TemplateResponse toResponse(MasterTemplateDefinition template) {
        TemplateResponse response = new TemplateResponse();
        response.setTemplateId(template.getTemplateId());
        response.setVersion(template.getVersion());
        response.setTemplateName(template.getTemplateName());
        // Map all fields...
        return response;
    }
}
```

### Acceptance Criteria
- [ ] All 5 CRUD endpoints implemented
- [ ] Request validation using Jakarta Validation
- [ ] Proper HTTP status codes returned
- [ ] Template versioning working correctly
- [ ] Soft delete (archive) instead of hard delete
- [ ] Pagination implemented for list endpoint
- [ ] Filtering by category, docType, isShared working
- [ ] Error responses follow OpenAPI spec
- [ ] Unit tests for service layer (80%+ coverage)
- [ ] Integration tests for all endpoints
- [ ] API documentation generated (Springdoc OpenAPI)

### Testing
```java
@WebFluxTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TemplateService templateService;

    @Test
    void createTemplate_ValidRequest_ReturnsCreated() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setTemplateName("Test Template");
        request.setDocType("disclosure");
        request.setCategory("Privacy Policy");

        TemplateResponse response = new TemplateResponse();
        response.setTemplateId(UUID.randomUUID());

        when(templateService.createTemplate(any())).thenReturn(Mono.just(response));

        webTestClient.post()
            .uri("/api/v1/templates")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TemplateResponse.class);
    }
}
```

### Dependencies
- STORY-003 (Entity models)
- Validation framework (Jakarta Validation)

---

## EPIC-DOC-003: Document Enquiry & Retrieval

---

## STORY-006: Implement Basic Document Enquiry API

**Type:** Story
**Priority:** Critical
**Story Points:** 8 SP
**Estimated Hours:** 8-10 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 5

### User Story
```
As a customer
I want to retrieve my account-specific documents
So that I can view my statements, notices, and disclosures
```

### Description
Implement the core document enquiry endpoint that retrieves account-specific documents for a given customer and account.

### API Endpoint
**POST /api/v1/documents-enquiry**

### Request Schema
```json
{
  "customerId": "880e8400-e29b-41d4-a716-446655440001",
  "accountId": ["770e8400-e29b-41d4-a716-446655440001"],
  "documentTypeCategoryGroup": ["Statement", "Disclosure"],
  "postedFromDate": 1609459200,
  "postedToDate": 1640995200,
  "pageNumber": 0,
  "pageSize": 20,
  "sortOrder": "DESC"
}
```

### Response Schema
```json
{
  "documentList": [
    {
      "documentId": "660e8400-e29b-41d4-a716-446655440001",
      "displayName": "Monthly Statement",
      "category": "Statement",
      "documentType": "monthly_statement",
      "datePosted": 1762403484,
      "sizeInMb": 2.5,
      "languageCode": "en_us",
      "mimeType": "application/pdf",
      "metadata": {
        "statementCycle": "2024-01",
        "accountNumber": "****1234"
      },
      "_links": {
        "download": {
          "href": "/api/v1/documents/660e8400-e29b-41d4-a716-446655440001"
        }
      }
    }
  ],
  "pagination": {
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8,
    "pageNumber": 0
  },
  "_links": {
    "self": {
      "href": "/api/v1/documents-enquiry"
    },
    "next": {
      "href": "/api/v1/documents-enquiry?pageNumber=1"
    }
  }
}
```

### Technical Details

**DocumentEnquiryController.java**
```java
package com.company.documenthub.controller;

import com.company.documenthub.model.dto.DocumentEnquiryRequest;
import com.company.documenthub.model.dto.DocumentRetrievalResponse;
import com.company.documenthub.service.DocumentEnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentEnquiryController {

    private final DocumentEnquiryService documentEnquiryService;

    @PostMapping("/documents-enquiry")
    public Mono<DocumentRetrievalResponse> getDocuments(
            @Valid @RequestBody DocumentEnquiryRequest request) {
        return documentEnquiryService.getDocuments(request);
    }
}
```

**DocumentEnquiryService.java**
```java
package com.company.documenthub.service;

import com.company.documenthub.model.dto.DocumentEnquiryRequest;
import com.company.documenthub.model.dto.DocumentRetrievalResponse;
import com.company.documenthub.model.dto.DocumentDetailsNode;
import com.company.documenthub.model.dto.PaginationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentEnquiryService {

    private final DatabaseClient databaseClient;

    public Mono<DocumentRetrievalResponse> getDocuments(DocumentEnquiryRequest request) {
        // Extract parameters
        UUID customerId = request.getCustomerId();
        List<UUID> accountIds = request.getAccountId();
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        // Build query
        String query = buildQuery(request);

        // Execute query
        return databaseClient.sql(query)
            .bind("customerId", customerId)
            .bind("accountIds", accountIds.toArray())
            .bind("limit", pageSize)
            .bind("offset", pageNumber * pageSize)
            .map((row, metadata) -> mapToDocumentNode(row))
            .all()
            .collectList()
            .zipWith(getDocumentCount(request))
            .map(tuple -> buildResponse(tuple.getT1(), tuple.getT2(), pageNumber, pageSize));
    }

    private String buildQuery(DocumentEnquiryRequest request) {
        StringBuilder query = new StringBuilder("""
            SELECT
                si.storage_index_id::text as documentId,
                mt.template_name as displayName,
                mt.category,
                si.doc_type as documentType,
                EXTRACT(EPOCH FROM si.doc_creation_date)::bigint as datePosted,
                si.file_size_bytes / 1048576.0 as sizeInMb,
                mt.language_code as languageCode,
                si.mime_type as mimeType,
                si.doc_metadata as metadata
            FROM storage_index si
            JOIN master_template_definition mt
                ON si.template_id = mt.template_id
                AND si.template_version = mt.version
            WHERE si.customer_key = :customerId
              AND si.account_key = ANY(:accountIds)
              AND si.archive_indicator = false
              AND si.is_accessible = true
            """);

        // Add optional filters
        if (request.getDocumentTypeCategoryGroup() != null && !request.getDocumentTypeCategoryGroup().isEmpty()) {
            query.append(" AND mt.category = ANY(:categories)");
        }

        if (request.getPostedFromDate() != null) {
            query.append(" AND EXTRACT(EPOCH FROM si.doc_creation_date)::bigint >= :postedFromDate");
        }

        if (request.getPostedToDate() != null) {
            query.append(" AND EXTRACT(EPOCH FROM si.doc_creation_date)::bigint <= :postedToDate");
        }

        // Add sorting
        String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "DESC";
        query.append(" ORDER BY si.doc_creation_date ").append(sortOrder);

        // Add pagination
        query.append(" LIMIT :limit OFFSET :offset");

        return query.toString();
    }

    private Mono<Long> getDocumentCount(DocumentEnquiryRequest request) {
        // Build count query (same filters as main query)
        String countQuery = """
            SELECT COUNT(*)
            FROM storage_index si
            JOIN master_template_definition mt
                ON si.template_id = mt.template_id
                AND si.template_version = mt.version
            WHERE si.customer_key = :customerId
              AND si.account_key = ANY(:accountIds)
              AND si.archive_indicator = false
              AND si.is_accessible = true
            """;

        return databaseClient.sql(countQuery)
            .bind("customerId", request.getCustomerId())
            .bind("accountIds", request.getAccountId().toArray())
            .map(row -> row.get(0, Long.class))
            .one();
    }

    private DocumentDetailsNode mapToDocumentNode(Row row) {
        DocumentDetailsNode node = new DocumentDetailsNode();
        node.setDocumentId(UUID.fromString(row.get("documentId", String.class)));
        node.setDisplayName(row.get("displayName", String.class));
        node.setCategory(row.get("category", String.class));
        node.setDocumentType(row.get("documentType", String.class));
        node.setDatePosted(row.get("datePosted", Long.class));
        node.setSizeInMb(row.get("sizeInMb", Double.class));
        node.setLanguageCode(row.get("languageCode", String.class));
        node.setMimeType(row.get("mimeType", String.class));
        node.setMetadata(row.get("metadata", JsonNode.class));

        // Add download link
        Map<String, Object> downloadLink = new HashMap<>();
        downloadLink.put("href", "/api/v1/documents/" + node.getDocumentId());
        Map<String, Object> links = new HashMap<>();
        links.put("download", downloadLink);
        node.setLinks(links);

        return node;
    }

    private DocumentRetrievalResponse buildResponse(
            List<DocumentDetailsNode> documents,
            Long totalCount,
            int pageNumber,
            int pageSize) {

        DocumentRetrievalResponse response = new DocumentRetrievalResponse();
        response.setDocumentList(documents);

        // Build pagination
        PaginationResponse pagination = new PaginationResponse();
        pagination.setPageSize(pageSize);
        pagination.setTotalItems(totalCount);
        pagination.setTotalPages((int) Math.ceil(totalCount / (double) pageSize));
        pagination.setPageNumber(pageNumber);
        response.setPagination(pagination);

        // Build links
        Map<String, Object> selfLink = new HashMap<>();
        selfLink.put("href", "/api/v1/documents-enquiry");
        Map<String, Object> links = new HashMap<>();
        links.put("self", selfLink);

        if (pageNumber < pagination.getTotalPages() - 1) {
            Map<String, Object> nextLink = new HashMap<>();
            nextLink.put("href", "/api/v1/documents-enquiry?pageNumber=" + (pageNumber + 1));
            links.put("next", nextLink);
        }

        response.setLinks(links);

        return response;
    }
}
```

### Acceptance Criteria
- [ ] Endpoint returns account-specific documents for given customer and account(s)
- [ ] Supports multiple accountIds in request
- [ ] Pagination working correctly (pageNumber, pageSize)
- [ ] Filtering by category/documentType working
- [ ] Date range filtering working (postedFromDate, postedToDate)
- [ ] Sorting by datePosted (ASC/DESC) working
- [ ] Response includes all required fields per OpenAPI spec
- [ ] Response uses camelCase field names
- [ ] HATEOAS links included (_links)
- [ ] Performance: < 200ms for 1000+ documents
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests for all scenarios
- [ ] Load testing completed (1000 requests/sec)

### Testing
```java
@Test
void getDocuments_ValidRequest_ReturnsDocuments() {
    DocumentEnquiryRequest request = new DocumentEnquiryRequest();
    request.setCustomerId(UUID.fromString("880e8400-e29b-41d4-a716-446655440001"));
    request.setAccountId(List.of(UUID.fromString("770e8400-e29b-41d4-a716-446655440001")));
    request.setPageNumber(0);
    request.setPageSize(20);

    webTestClient.post()
        .uri("/api/v1/documents-enquiry")
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.documentList").isArray()
        .jsonPath("$.pagination.pageSize").isEqualTo(20)
        .jsonPath("$.pagination.totalItems").exists()
        .jsonPath("$._links.self.href").exists();
}

@Test
void getDocuments_WithDateFilter_ReturnsFilteredDocuments() {
    DocumentEnquiryRequest request = new DocumentEnquiryRequest();
    request.setCustomerId(UUID.fromString("880e8400-e29b-41d4-a716-446655440001"));
    request.setAccountId(List.of(UUID.fromString("770e8400-e29b-41d4-a716-446655440001")));
    request.setPostedFromDate(1609459200L); // 2021-01-01
    request.setPostedToDate(1640995200L);   // 2022-01-01

    webTestClient.post()
        .uri("/api/v1/documents-enquiry")
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.documentList[*].datePosted")
            .value(dates -> {
                // All dates should be within range
                ((List<Long>) dates).forEach(date -> {
                    assertTrue(date >= 1609459200L && date <= 1640995200L);
                });
            });
}
```

### Dependencies
- STORY-003 (Entity models)
- STORY-001 (Database schema)
- R2DBC DatabaseClient

### Performance Considerations
- Index on (account_key, customer_key, doc_creation_date DESC)
- Query optimization for large result sets
- Consider caching for frequently accessed documents

---

## STORY-007: Implement Shared Document Eligibility Logic

**Type:** Story
**Priority:** High
**Story Points:** 13 SP
**Estimated Hours:** 12-15 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 6-7

### User Story
```
As a customer
I want to see shared documents that are relevant to my account type
So that I can access privacy policies, cardholder agreements, and regulatory disclosures
```

### Description
Implement logic to retrieve and merge shared documents based on sharing scope:
1. **all** - Available to all customers
2. **credit_card_account_only** - Available only to credit card account holders
3. **custom_rule** - Dynamic eligibility based on data extraction rules

This is the most complex story as it involves integrating with the Generic Data Extraction Engine for custom rules.

### Technical Details

**SharedDocumentEligibilityService.java**
```java
package com.company.documenthub.service;

import com.company.documenthub.model.dto.DocumentDetailsNode;
import com.company.documenthub.model.entity.MasterTemplateDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SharedDocumentEligibilityService {

    private final DatabaseClient databaseClient;
    private final GenericExtractionEngine extractionEngine;
    private final AccountTypeService accountTypeService;

    public Flux<DocumentDetailsNode> getEligibleSharedDocuments(
            UUID customerId,
            UUID accountId) {

        // Get shared documents with scope "all"
        Flux<DocumentDetailsNode> allScopeDocuments = getSharedDocumentsByScope("all");

        // Get account type and filter documents by account type
        Mono<String> accountTypeMono = accountTypeService.getAccountType(accountId);

        Flux<DocumentDetailsNode> accountTypeDocuments = accountTypeMono
            .flatMapMany(accountType -> {
                if ("CREDIT_CARD".equals(accountType)) {
                    return getSharedDocumentsByScope("credit_card_account_only");
                } else if ("DIGITAL_BANK".equals(accountType)) {
                    return getSharedDocumentsByScope("digital_bank_customer_only");
                } else if ("ENTERPRISE".equals(accountType)) {
                    return getSharedDocumentsByScope("enterprise_customer_only");
                }
                return Flux.empty();
            });

        // Get documents with custom rules and evaluate eligibility
        Flux<DocumentDetailsNode> customRuleDocuments = getSharedDocumentsByScope("custom_rule")
            .filterWhen(doc -> evaluateCustomRule(doc, customerId, accountId));

        // Merge all eligible shared documents
        return Flux.concat(allScopeDocuments, accountTypeDocuments, customRuleDocuments)
            .distinct(DocumentDetailsNode::getDocumentId);
    }

    private Flux<DocumentDetailsNode> getSharedDocumentsByScope(String scope) {
        String query = """
            SELECT
                template_id::text as documentId,
                template_name as displayName,
                category,
                doc_type as documentType,
                EXTRACT(EPOCH FROM effective_date)::bigint as datePosted,
                language_code as languageCode,
                sharing_scope as sharingScope
            FROM master_template_definition
            WHERE is_shared_document = true
              AND archive_indicator = false
              AND sharing_scope = :scope
              AND (effective_date IS NULL OR effective_date <= EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::bigint)
              AND (valid_until IS NULL OR valid_until >= EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::bigint)
            ORDER BY effective_date DESC
            """;

        return databaseClient.sql(query)
            .bind("scope", scope)
            .map((row, metadata) -> {
                DocumentDetailsNode node = new DocumentDetailsNode();
                node.setDocumentId(UUID.fromString(row.get("documentId", String.class)));
                node.setDisplayName(row.get("displayName", String.class));
                node.setCategory(row.get("category", String.class));
                node.setDocumentType(row.get("documentType", String.class));
                node.setDatePosted(row.get("datePosted", Long.class));
                node.setLanguageCode(row.get("languageCode", String.class));
                // Add shared document indicator
                node.setIsShared(true);
                node.setSharingScope(row.get("sharingScope", String.class));
                return node;
            })
            .all();
    }

    private Mono<Boolean> evaluateCustomRule(
            DocumentDetailsNode document,
            UUID customerId,
            UUID accountId) {

        // Get data extraction schema for this template
        return databaseClient.sql("""
                SELECT data_extraction_schema
                FROM master_template_definition
                WHERE template_id = :templateId
                  AND archive_indicator = false
                LIMIT 1
                """)
            .bind("templateId", document.getDocumentId())
            .map(row -> row.get("data_extraction_schema", JsonNode.class))
            .one()
            .flatMap(schema -> {
                if (schema == null || schema.isNull()) {
                    // No custom rule, default to not eligible
                    return Mono.just(false);
                }

                // Execute extraction engine to evaluate eligibility
                return extractionEngine.evaluateEligibility(schema, customerId, accountId);
            })
            .onErrorReturn(false); // On error, document is not eligible
    }
}
```

**Update DocumentEnquiryService to merge shared documents**
```java
public Mono<DocumentRetrievalResponse> getDocuments(DocumentEnquiryRequest request) {
    UUID customerId = request.getCustomerId();
    List<UUID> accountIds = request.getAccountId();

    // Get account-specific documents
    Mono<List<DocumentDetailsNode>> accountDocsMono = getAccountSpecificDocuments(request);

    // Get shared documents (for first account only, or merge for all accounts)
    UUID primaryAccountId = accountIds.get(0);
    Mono<List<DocumentDetailsNode>> sharedDocsMono = sharedDocumentEligibilityService
        .getEligibleSharedDocuments(customerId, primaryAccountId)
        .collectList();

    // Merge and build response
    return Mono.zip(accountDocsMono, sharedDocsMono, getDocumentCount(request))
        .map(tuple -> {
            List<DocumentDetailsNode> accountDocs = tuple.getT1();
            List<DocumentDetailsNode> sharedDocs = tuple.getT2();
            Long totalCount = tuple.getT3();

            // Merge documents
            List<DocumentDetailsNode> allDocs = new ArrayList<>(accountDocs);
            allDocs.addAll(sharedDocs);

            // Sort by date posted DESC
            allDocs.sort((a, b) -> Long.compare(
                b.getDatePosted() != null ? b.getDatePosted() : 0,
                a.getDatePosted() != null ? a.getDatePosted() : 0
            ));

            return buildResponse(allDocs, totalCount + sharedDocs.size(),
                request.getPageNumber(), request.getPageSize());
        });
}
```

### Acceptance Criteria
- [ ] Shared documents with scope "all" returned for all customers
- [ ] Account-type-specific documents returned based on account type
- [ ] Custom rule evaluation working with GenericExtractionEngine
- [ ] Shared documents merged with account-specific documents
- [ ] No duplicate documents in response
- [ ] Documents sorted by datePosted DESC
- [ ] Effective date and valid until date filters applied
- [ ] isShared flag set to true for shared documents
- [ ] sharingScope field included for shared documents
- [ ] Performance: < 300ms with custom rule evaluation
- [ ] Unit tests for all sharing scopes
- [ ] Integration tests with mock extraction engine

### Testing
```java
@Test
void getEligibleSharedDocuments_AllScope_ReturnsAllScopeDocuments() {
    UUID customerId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    StepVerifier.create(sharedDocumentEligibilityService
            .getEligibleSharedDocuments(customerId, accountId))
        .assertNext(doc -> {
            assertEquals("all", doc.getSharingScope());
            assertTrue(doc.getIsShared());
        })
        .verifyComplete();
}

@Test
void getEligibleSharedDocuments_CreditCardAccount_ReturnsCreditCardDocs() {
    UUID customerId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    // Mock account type service
    when(accountTypeService.getAccountType(accountId))
        .thenReturn(Mono.just("CREDIT_CARD"));

    StepVerifier.create(sharedDocumentEligibilityService
            .getEligibleSharedDocuments(customerId, accountId))
        .expectNextMatches(doc ->
            "credit_card_account_only".equals(doc.getSharingScope()) ||
            "all".equals(doc.getSharingScope()))
        .verifyComplete();
}

@Test
void getEligibleSharedDocuments_CustomRule_EvaluatesRule() {
    UUID customerId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    // Mock extraction engine
    when(extractionEngine.evaluateEligibility(any(), eq(customerId), eq(accountId)))
        .thenReturn(Mono.just(true));

    StepVerifier.create(sharedDocumentEligibilityService
            .getEligibleSharedDocuments(customerId, accountId))
        .expectNextMatches(doc -> "custom_rule".equals(doc.getSharingScope()))
        .verifyComplete();
}
```

### Dependencies
- STORY-006 (Basic document enquiry)
- STORY-008, STORY-009, STORY-010, STORY-011 (Generic Extraction Engine)
- AccountTypeService (external dependency)

### Performance Considerations
- Cache shared documents (they rarely change)
- Parallel execution of custom rule evaluation
- Consider pre-computing eligibility for common scenarios

---

## EPIC-DOC-004: Generic Data Extraction Engine

---

## STORY-008: Implement Extraction Schema Parser

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 8-10 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 8

### User Story
```
As a system
I want to parse and validate data extraction schemas from templates
So that I can execute dynamic eligibility rules for shared documents
```

### Description
Implement a parser for the `data_extraction_schema` JSONB field that:
1. Parses the JSON configuration
2. Validates the schema structure
3. Extracts data source configurations
4. Builds an execution plan

### Data Extraction Schema Format
```json
{
  "rule_name": "high_balance_customer",
  "description": "Document visible to customers with balance > $10,000",
  "data_sources": [
    {
      "source_id": "account_info",
      "source_type": "REST_API",
      "endpoint_config": {
        "url": "https://api.company.com/v1/accounts/{accountId}",
        "method": "GET",
        "timeout_ms": 5000,
        "circuit_breaker": {
          "failure_threshold": 5,
          "wait_duration_ms": 30000
        }
      },
      "response_mapping": {
        "accountBalance": "$.data.balance.currentBalance",
        "accountStatus": "$.data.status",
        "customerTier": "$.data.customerTier"
      }
    },
    {
      "source_id": "transaction_history",
      "source_type": "REST_API",
      "endpoint_config": {
        "url": "https://api.company.com/v1/transactions",
        "method": "POST",
        "body_template": {
          "accountId": "{{accountId}}",
          "startDate": "{{startDate}}",
          "endDate": "{{endDate}}"
        }
      },
      "response_mapping": {
        "transactionCount": "$.data.totalCount",
        "largestTransaction": "$.data.transactions[0].amount"
      }
    }
  ],
  "transformations": [
    {
      "field": "accountBalance",
      "operations": [
        {"type": "TO_DOUBLE"},
        {"type": "ABS"}
      ]
    }
  ],
  "conditions": [
    {
      "field": "accountBalance",
      "operator": "GREATER_THAN",
      "value": 10000,
      "logical_operator": "AND"
    },
    {
      "field": "accountStatus",
      "operator": "EQUALS",
      "value": "ACTIVE",
      "logical_operator": "AND"
    }
  ],
  "eligibility_result": {
    "on_success": true,
    "on_failure": false,
    "on_error": false
  }
}
```

### Technical Details

**ExtractionSchemaParser.java**
```java
package com.company.documenthub.extraction;

import com.company.documenthub.extraction.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExtractionSchemaParser {

    private final ObjectMapper objectMapper;

    public ExtractionConfig parse(JsonNode schemaNode) throws ExtractionSchemaException {
        try {
            ExtractionConfig config = new ExtractionConfig();

            // Parse basic info
            config.setRuleName(schemaNode.get("rule_name").asText());
            config.setDescription(schemaNode.get("description").asText());

            // Parse data sources
            List<DataSource> dataSources = parseDataSources(schemaNode.get("data_sources"));
            config.setDataSources(dataSources);

            // Parse transformations
            if (schemaNode.has("transformations")) {
                List<Transformation> transformations = parseTransformations(schemaNode.get("transformations"));
                config.setTransformations(transformations);
            }

            // Parse conditions
            List<Condition> conditions = parseConditions(schemaNode.get("conditions"));
            config.setConditions(conditions);

            // Parse eligibility result
            EligibilityResult result = parseEligibilityResult(schemaNode.get("eligibility_result"));
            config.setEligibilityResult(result);

            // Validate
            validate(config);

            return config;
        } catch (Exception e) {
            throw new ExtractionSchemaException("Failed to parse extraction schema", e);
        }
    }

    private List<DataSource> parseDataSources(JsonNode dataSourcesNode) {
        List<DataSource> dataSources = new ArrayList<>();

        if (dataSourcesNode != null && dataSourcesNode.isArray()) {
            for (JsonNode sourceNode : dataSourcesNode) {
                DataSource dataSource = new DataSource();
                dataSource.setSourceId(sourceNode.get("source_id").asText());
                dataSource.setSourceType(sourceNode.get("source_type").asText());

                // Parse endpoint config
                if (sourceNode.has("endpoint_config")) {
                    EndpointConfig endpointConfig = parseEndpointConfig(sourceNode.get("endpoint_config"));
                    dataSource.setEndpointConfig(endpointConfig);
                }

                // Parse response mapping
                if (sourceNode.has("response_mapping")) {
                    ResponseMapping mapping = parseResponseMapping(sourceNode.get("response_mapping"));
                    dataSource.setResponseMapping(mapping);
                }

                dataSources.add(dataSource);
            }
        }

        return dataSources;
    }

    private EndpointConfig parseEndpointConfig(JsonNode configNode) {
        EndpointConfig config = new EndpointConfig();
        config.setUrl(configNode.get("url").asText());
        config.setMethod(configNode.get("method").asText());
        config.setTimeoutMs(configNode.has("timeout_ms") ? configNode.get("timeout_ms").asInt() : 5000);

        // Parse circuit breaker config
        if (configNode.has("circuit_breaker")) {
            CircuitBreakerConfig cbConfig = objectMapper.convertValue(
                configNode.get("circuit_breaker"),
                CircuitBreakerConfig.class
            );
            config.setCircuitBreaker(cbConfig);
        }

        // Parse retry policy
        if (configNode.has("retry_policy")) {
            RetryPolicy retryPolicy = objectMapper.convertValue(
                configNode.get("retry_policy"),
                RetryPolicy.class
            );
            config.setRetryPolicy(retryPolicy);
        }

        // Parse body template
        if (configNode.has("body_template")) {
            config.setBodyTemplate(configNode.get("body_template"));
        }

        return config;
    }

    private ResponseMapping parseResponseMapping(JsonNode mappingNode) {
        ResponseMapping mapping = new ResponseMapping();
        Map<String, String> fieldMappings = new HashMap<>();

        mappingNode.fields().forEachRemaining(entry -> {
            fieldMappings.put(entry.getKey(), entry.getValue().asText());
        });

        mapping.setFieldMappings(fieldMappings);
        return mapping;
    }

    private List<Transformation> parseTransformations(JsonNode transformationsNode) {
        List<Transformation> transformations = new ArrayList<>();

        if (transformationsNode != null && transformationsNode.isArray()) {
            for (JsonNode transformNode : transformationsNode) {
                Transformation transformation = objectMapper.convertValue(
                    transformNode,
                    Transformation.class
                );
                transformations.add(transformation);
            }
        }

        return transformations;
    }

    private List<Condition> parseConditions(JsonNode conditionsNode) {
        List<Condition> conditions = new ArrayList<>();

        if (conditionsNode != null && conditionsNode.isArray()) {
            for (JsonNode conditionNode : conditionsNode) {
                Condition condition = new Condition();
                condition.setField(conditionNode.get("field").asText());
                condition.setOperator(conditionNode.get("operator").asText());
                condition.setValue(conditionNode.get("value"));
                condition.setLogicalOperator(
                    conditionNode.has("logical_operator") ?
                    conditionNode.get("logical_operator").asText() : "AND"
                );
                conditions.add(condition);
            }
        }

        return conditions;
    }

    private EligibilityResult parseEligibilityResult(JsonNode resultNode) {
        EligibilityResult result = new EligibilityResult();
        result.setOnSuccess(resultNode.get("on_success").asBoolean());
        result.setOnFailure(resultNode.get("on_failure").asBoolean());
        result.setOnError(resultNode.get("on_error").asBoolean());
        return result;
    }

    private void validate(ExtractionConfig config) throws ExtractionSchemaException {
        // Validate required fields
        if (config.getRuleName() == null || config.getRuleName().isEmpty()) {
            throw new ExtractionSchemaException("rule_name is required");
        }

        if (config.getDataSources() == null || config.getDataSources().isEmpty()) {
            throw new ExtractionSchemaException("At least one data source is required");
        }

        // Validate data sources
        for (DataSource dataSource : config.getDataSources()) {
            if (dataSource.getSourceId() == null || dataSource.getSourceId().isEmpty()) {
                throw new ExtractionSchemaException("source_id is required for all data sources");
            }

            if (!"REST_API".equals(dataSource.getSourceType()) &&
                !"DATABASE".equals(dataSource.getSourceType()) &&
                !"CACHE".equals(dataSource.getSourceType())) {
                throw new ExtractionSchemaException("Invalid source_type: " + dataSource.getSourceType());
            }
        }

        // Validate conditions
        if (config.getConditions() != null) {
            for (Condition condition : config.getConditions()) {
                validateCondition(condition);
            }
        }
    }

    private void validateCondition(Condition condition) throws ExtractionSchemaException {
        List<String> validOperators = List.of(
            "EQUALS", "NOT_EQUALS", "GREATER_THAN", "LESS_THAN",
            "GREATER_THAN_OR_EQUALS", "LESS_THAN_OR_EQUALS",
            "CONTAINS", "NOT_CONTAINS", "IN", "NOT_IN"
        );

        if (!validOperators.contains(condition.getOperator())) {
            throw new ExtractionSchemaException("Invalid operator: " + condition.getOperator());
        }
    }
}
```

### Acceptance Criteria
- [ ] Parser successfully parses valid schemas
- [ ] Parser throws exception for invalid schemas
- [ ] All schema fields validated
- [ ] Data sources parsed correctly (REST_API, DATABASE, CACHE)
- [ ] Endpoint configurations parsed (URL, method, timeout, circuit breaker)
- [ ] Response mappings parsed with JSONPath support
- [ ] Transformations parsed correctly
- [ ] Conditions parsed with all operators
- [ ] Eligibility result parsed
- [ ] Unit tests for all schema variations (95%+ coverage)
- [ ] Error messages are descriptive and actionable

### Testing
```java
@Test
void parse_ValidSchema_ReturnsConfig() throws Exception {
    String schemaJson = """
        {
          "rule_name": "test_rule",
          "description": "Test rule",
          "data_sources": [{
            "source_id": "account",
            "source_type": "REST_API",
            "endpoint_config": {
              "url": "https://api.test.com/accounts/{accountId}",
              "method": "GET"
            },
            "response_mapping": {
              "balance": "$.data.balance"
            }
          }],
          "conditions": [{
            "field": "balance",
            "operator": "GREATER_THAN",
            "value": 1000
          }],
          "eligibility_result": {
            "on_success": true,
            "on_failure": false,
            "on_error": false
          }
        }
        """;

    JsonNode schemaNode = objectMapper.readTree(schemaJson);
    ExtractionConfig config = parser.parse(schemaNode);

    assertNotNull(config);
    assertEquals("test_rule", config.getRuleName());
    assertEquals(1, config.getDataSources().size());
    assertEquals(1, config.getConditions().size());
}

@Test
void parse_MissingRuleName_ThrowsException() {
    String schemaJson = """
        {
          "description": "Test rule",
          "data_sources": []
        }
        """;

    JsonNode schemaNode = objectMapper.readTree(schemaJson);

    assertThrows(ExtractionSchemaException.class, () -> parser.parse(schemaNode));
}
```

### Dependencies
- Jackson for JSON parsing
- JSONPath library for response mapping

---

## STORY-009: Implement Multi-Source Data Orchestration

**Type:** Story
**Priority:** High
**Story Points:** 13 SP
**Estimated Hours:** 12-15 hours
**Assignee:** Senior Backend Developer
**Sprint:** Sprint 8-9

### User Story
```
As a system
I want to execute API calls to multiple data sources in parallel
So that I can gather required data for eligibility evaluation efficiently
```

### Description
Implement the orchestration layer that:
1. Executes multiple REST API calls in parallel
2. Applies circuit breaker and retry policies
3. Extracts data from responses using JSONPath
4. Handles errors gracefully
5. Returns extracted data for rule evaluation

### Technical Details

**DataOrchestrationService.java**
```java
package com.company.documenthub.extraction;

import com.company.documenthub.extraction.model.*;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataOrchestrationService {

    private final WebClient webClient;

    public Mono<Map<String, Object>> executeDataSources(
            ExtractionConfig config,
            UUID customerId,
            UUID accountId) {

        // Execute all data sources in parallel
        List<Mono<Tuple2<String, Map<String, Object>>>> dataSourceMonos =
            config.getDataSources().stream()
                .map(dataSource -> executeDataSource(dataSource, customerId, accountId)
                    .map(data -> Tuples.of(dataSource.getSourceId(), data))
                    .onErrorResume(error -> {
                        // Log error and return empty map
                        log.error("Error executing data source: {}", dataSource.getSourceId(), error);
                        return Mono.just(Tuples.of(dataSource.getSourceId(), Map.of()));
                    })
                )
                .toList();

        // Zip all results
        return Flux.fromIterable(dataSourceMonos)
            .flatMap(mono -> mono)
            .collectMap(Tuple2::getT1, Tuple2::getT2);
    }

    private Mono<Map<String, Object>> executeDataSource(
            DataSource dataSource,
            UUID customerId,
            UUID accountId) {

        switch (dataSource.getSourceType()) {
            case "REST_API":
                return executeRestApiCall(dataSource, customerId, accountId);
            case "DATABASE":
                return executeDatabaseQuery(dataSource, customerId, accountId);
            case "CACHE":
                return executeCacheLookup(dataSource, customerId, accountId);
            default:
                return Mono.error(new UnsupportedOperationException(
                    "Unsupported source type: " + dataSource.getSourceType()));
        }
    }

    private Mono<Map<String, Object>> executeRestApiCall(
            DataSource dataSource,
            UUID customerId,
            UUID accountId) {

        EndpointConfig endpointConfig = dataSource.getEndpointConfig();

        // Replace variables in URL
        String url = replaceVariables(endpointConfig.getUrl(), customerId, accountId);

        // Build request
        WebClient.RequestBodySpec request = webClient
            .method(HttpMethod.valueOf(endpointConfig.getMethod()))
            .uri(url);

        // Add body if POST/PUT
        if (endpointConfig.getBodyTemplate() != null) {
            String body = replaceVariables(
                endpointConfig.getBodyTemplate().toString(),
                customerId,
                accountId
            );
            request.bodyValue(body);
        }

        // Execute with resilience patterns
        Mono<String> responseMono = request
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(endpointConfig.getTimeoutMs()))
            .transform(applyCircuitBreaker(endpointConfig.getCircuitBreaker(), dataSource.getSourceId()))
            .transform(applyRetryPolicy(endpointConfig.getRetryPolicy(), dataSource.getSourceId()));

        // Extract data from response
        return responseMono.map(responseBody ->
            extractDataFromResponse(responseBody, dataSource.getResponseMapping())
        );
    }

    private String replaceVariables(String template, UUID customerId, UUID accountId) {
        return template
            .replace("{customerId}", customerId.toString())
            .replace("{accountId}", accountId.toString())
            .replace("{{customerId}}", customerId.toString())
            .replace("{{accountId}}", accountId.toString())
            .replace("{{startDate}}", getStartDate())
            .replace("{{endDate}}", getEndDate());
    }

    private Map<String, Object> extractDataFromResponse(
            String responseBody,
            ResponseMapping mapping) {

        Map<String, Object> extractedData = new HashMap<>();

        if (mapping == null || mapping.getFieldMappings() == null) {
            return extractedData;
        }

        mapping.getFieldMappings().forEach((fieldName, jsonPath) -> {
            try {
                Object value = JsonPath.read(responseBody, jsonPath);
                extractedData.put(fieldName, value);
            } catch (Exception e) {
                log.warn("Failed to extract field {} using path {}: {}",
                    fieldName, jsonPath, e.getMessage());
                extractedData.put(fieldName, null);
            }
        });

        return extractedData;
    }

    private <T> TransformOperator<T> applyCircuitBreaker(
            CircuitBreakerConfig cbConfig,
            String sourceId) {

        if (cbConfig == null) {
            return mono -> mono; // No-op
        }

        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureThreshold() != null ?
                    cbConfig.getFailureThreshold() : 50)
                .waitDurationInOpenState(Duration.ofMillis(
                    cbConfig.getWaitDurationMs() != null ?
                    cbConfig.getWaitDurationMs() : 60000))
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(
            "extraction-" + sourceId,
            config
        );

        return CircuitBreakerOperator.of(circuitBreaker);
    }

    private <T> TransformOperator<T> applyRetryPolicy(
            RetryPolicy retryPolicy,
            String sourceId) {

        if (retryPolicy == null) {
            return mono -> mono; // No-op
        }

        RetryConfig config = RetryConfig.custom()
            .maxAttempts(retryPolicy.getMaxAttempts() != null ?
                retryPolicy.getMaxAttempts() : 3)
            .waitDuration(Duration.ofMillis(
                retryPolicy.getWaitDurationMs() != null ?
                retryPolicy.getWaitDurationMs() : 1000))
            .build();

        Retry retry = Retry.of("extraction-retry-" + sourceId, config);

        return RetryOperator.of(retry);
    }

    private Mono<Map<String, Object>> executeDatabaseQuery(
            DataSource dataSource,
            UUID customerId,
            UUID accountId) {
        // Implementation for database queries
        // Use DatabaseClient to execute queries
        return Mono.just(Map.of());
    }

    private Mono<Map<String, Object>> executeCacheLookup(
            DataSource dataSource,
            UUID customerId,
            UUID accountId) {
        // Implementation for cache lookups
        // Use ReactiveRedisTemplate
        return Mono.just(Map.of());
    }

    private String getStartDate() {
        // Return start date for queries (e.g., 90 days ago)
        return LocalDate.now().minusDays(90).toString();
    }

    private String getEndDate() {
        // Return end date for queries (today)
        return LocalDate.now().toString();
    }
}
```

### Acceptance Criteria
- [ ] Parallel execution of multiple data sources
- [ ] Circuit breaker applied to REST API calls
- [ ] Retry policy applied with configurable attempts and delay
- [ ] JSONPath extraction working for all response types
- [ ] Variable replacement in URLs and body templates
- [ ] Timeout handling (returns error, not blocks forever)
- [ ] Error handling returns empty data (doesn't fail entire extraction)
- [ ] Support for GET, POST, PUT methods
- [ ] Logging of all API calls and errors
- [ ] Unit tests with WireMock for API mocking (80%+ coverage)
- [ ] Integration tests with real endpoints

### Testing
```java
@Test
void executeRestApiCall_Success_ReturnsExtractedData() {
    // Mock API response
    wireMockServer.stubFor(WireMock.get(urlEqualTo("/accounts/123"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                  "data": {
                    "balance": 15000.50,
                    "status": "ACTIVE"
                  }
                }
                """)));

    DataSource dataSource = new DataSource();
    dataSource.setSourceId("account");
    dataSource.setSourceType("REST_API");

    EndpointConfig config = new EndpointConfig();
    config.setUrl("http://localhost:8089/accounts/{accountId}");
    config.setMethod("GET");
    dataSource.setEndpointConfig(config);

    ResponseMapping mapping = new ResponseMapping();
    mapping.setFieldMappings(Map.of(
        "accountBalance", "$.data.balance",
        "accountStatus", "$.data.status"
    ));
    dataSource.setResponseMapping(mapping);

    UUID accountId = UUID.fromString("00000000-0000-0000-0000-000000000123");

    Mono<Map<String, Object>> result = orchestrationService
        .executeDataSource(dataSource, UUID.randomUUID(), accountId);

    StepVerifier.create(result)
        .assertNext(data -> {
            assertEquals(15000.50, data.get("accountBalance"));
            assertEquals("ACTIVE", data.get("accountStatus"));
        })
        .verifyComplete();
}

@Test
void executeRestApiCall_WithCircuitBreaker_FailsAfterThreshold() {
    // Mock API to always fail
    wireMockServer.stubFor(WireMock.get(urlMatching("/accounts/.*"))
        .willReturn(aResponse().withStatus(500)));

    DataSource dataSource = createDataSourceWithCircuitBreaker();

    // Make multiple calls to trigger circuit breaker
    for (int i = 0; i < 10; i++) {
        orchestrationService.executeDataSource(dataSource, UUID.randomUUID(), UUID.randomUUID())
            .subscribe(
                data -> {},
                error -> log.error("Call failed", error)
            );
    }

    // Verify circuit is open
    CircuitBreaker cb = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
        .circuitBreaker("extraction-account");

    assertEquals(CircuitBreaker.State.OPEN, cb.getState());
}
```

### Dependencies
- Resilience4j (Circuit Breaker, Retry)
- JSONPath library (com.jayway.jsonpath)
- WebClient (Spring WebFlux)
- WireMock (testing)

---

Due to the length limitations, I'll create the JIRA document now with the stories we've defined so far. Let me complete the file.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Analyze existing codebase and documentation to understand completed work", "activeForm": "Analyzing existing codebase and documentation", "status": "completed"}, {"content": "Identify pending work and gaps", "activeForm": "Identifying pending work and gaps", "status": "completed"}, {"content": "Create JIRA epic and stories structure", "activeForm": "Creating JIRA epic and stories structure", "status": "completed"}, {"content": "Write detailed JIRA tickets with acceptance criteria", "activeForm": "Writing detailed JIRA tickets with acceptance criteria", "status": "in_progress"}]