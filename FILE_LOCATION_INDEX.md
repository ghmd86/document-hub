# Document Hub Project - File Location Index

**Last Updated:** 2025-11-09
**Purpose:** Quick reference for all important files and folders in the Document Hub project

---

## Project Root Structure

```
C:\Users\ghmd8\Documents\AI\
├── document-hub-service/          # Main Spring Boot application
├── reactive-disclosure-extractor/  # Reactive extraction engine service
├── actual/                        # API specs and database documentation
├── docs/                          # All documentation
└── FILE_LOCATION_INDEX.md         # This file
```

---

## Main Application (document-hub-service)

### Source Code

**Location:** `C:\Users\ghmd8\Documents\AI\document-hub-service\src\main\java\com\documenthub\`

#### Controllers
- `controller\DocumentEnquiryController.java` - ❌ DISABLED (compilation errors)
- `controller\DocumentEnquiryTestController.java` - ✅ ACTIVE (working test endpoint)
  - Endpoints:
    - `GET /api/test/health`
    - `POST /api/test/documents-enquiry`

#### Model - Entities
- `model\entity\MasterTemplateDefinition.java` - Template definition entity
  - **Important:** Uses template_id as @Id, version as regular column (R2DBC composite key workaround)
  - Includes new `templateConfig` JsonNode field (added 2025-11-09)
- `model\entity\StorageIndex.java` - Document storage index entity

#### Model - DTOs
- `model\dto\DocumentEnquiryRequest.java` - Request DTO for document enquiry
  - **Fixed:** javax.validation → jakarta.validation imports (2025-11-09)

#### Services (TEMPORARILY DISABLED - Compilation Errors)
**Location:** `C:\Users\ghmd8\Documents\AI\document-hub-service\src\main\java\com\documenthub\service\`
- ❌ `DocumentEnquiryService.java` - Moved/disabled
- ❌ `DocumentMappingService.java` - Moved/disabled
- ❌ `SharedDocumentEligibilityService.java` - Moved/disabled

### Resources

**Location:** `C:\Users\ghmd8\Documents\AI\document-hub-service\src\main\resources\`

#### Configuration
- `application.yml` - Main Spring Boot configuration
  - Database: PostgreSQL R2DBC connection (localhost:5433)
  - Flyway: Database migration configuration
  - Logging configuration

#### Database Migrations
**Location:** `C:\Users\ghmd8\Documents\AI\document-hub-service\src\main\resources\db\migration\`
- `V1__initial_schema.sql` - Initial database schema
- `V2__add_template_config_column.sql` - ✅ DEPLOYED (2025-11-09)
  - Added template_config JSONB column
  - Created GIN indexes for vendor fields

### Build Configuration
- `pom.xml` - Maven build configuration
  - Added Flyway Core dependency (2025-11-09)
  - Added Resilience4j Retry dependency (2025-11-09)
  - Fixed validation imports to Jakarta

### Build Artifacts
**Location:** `C:\Users\ghmd8\Documents\AI\document-hub-service\target\`
- `document-hub-service-1.0.0-SNAPSHOT.jar` - Built application JAR
- **Status:** Application running on port 8080

---

## Reactive Disclosure Extractor Service

**Location:** `C:\Users\ghmd8\Documents\AI\reactive-disclosure-extractor\`

### Source Code
**Location:** `C:\Users\ghmd8\Documents\AI\reactive-disclosure-extractor\src\main\java\com\documenthub\disclosure\`

#### Main Application
- `DisclosureExtractorApplication.java` - Spring Boot main class

#### Controllers
- `controller\DisclosureExtractionController.java` - Extraction API controller

#### Services
- `service\DisclosureExtractionService.java` - Main extraction orchestrator
- ❌ `service\GenericExtractionEngine.java.bak` - Temporarily disabled (compilation errors)
- ❌ `service\RuleEvaluationService.java` - Disabled
- ❌ `service\TransformationService.java` - Disabled

#### Models
**Location:** `src\main\java\com\documenthub\disclosure\model\`
- `ExtractionConfig.java` - Configuration model
- `DataSource.java` - Data source configuration
- `Condition.java` - Rule condition model
- `Transform.java` - Data transformation model
- `Validation.java` - Validation rules
- `ErrorHandling.java` - Error handling configuration
- Plus many more supporting models

#### Configuration
- `config\RedisConfig.java` - Redis cache configuration
- `config\WebClientConfig.java` - WebClient for HTTP calls

---

## API Specifications

**Location:** `C:\Users\ghmd8\Documents\AI\actual\api\`

### OpenAPI Specification
- `schema.yaml` - Complete OpenAPI 3.0 specification
  - **Updated:** Added TemplateConfig schema (2025-11-09)
  - **Updated:** Added templateConfig field to CreateTemplateRequest and TemplateResponse

### Postman Collection
- `Document_Hub_API.postman_collection.json` - ✅ CREATED (2025-11-09)
  - 6 endpoint categories
  - Working test endpoints included
  - Environment variables pre-configured

---

## Database Documentation

**Location:** `C:\Users\ghmd8\Documents\AI\actual\database\`

- `database_schema.md` - ✅ UPDATED (2025-11-09)
  - Added template_config column documentation
  - Includes example JSONB structure

---

## Documentation

### Sequence Diagrams
**Location:** `C:\Users\ghmd8\Documents\AI\docs\sequence-diagrams\`
- `complete_api_sequence_diagrams.md` - ✅ UPDATED (2025-11-09)
  - Updated "Create Template" flow with templateConfig
  - Updated "Upload Document" flow with reference_key extraction logic

### Testing Documentation
**Location:** `C:\Users\ghmd8\Documents\AI\docs\testing\`

#### Test Reports
- `document_enquiry_api_test_report.md` - ✅ Database-level logic tests (2025-11-09)
  - SQL query validation
  - All scenarios tested
  - Custom rule schema validation

- `document_enquiry_rest_endpoint_test_report.md` - ✅ REST API tests (2025-11-09)
  - Live endpoint testing
  - Bug fix documentation
  - Performance metrics
  - Response validation

- `template_config_deployment_test_report.md` - ✅ template_config deployment tests (2025-11-09)
  - Database migration results
  - JSONB operation tests
  - Use case validation

- `api_spec_validation_and_pending_items.md` - ✅ API validation report (2025-11-09)
  - Request/response schema validation against OpenAPI spec
  - Field naming discrepancies (snake_case vs camelCase)
  - Missing required fields (pagination, _links)
  - 16 pending implementation tasks with effort estimates
  - 3-phase implementation plan (27.5-43 hours total)

#### Test Queries
- `document_enquiry_test_queries.sql` - SQL test queries
  - 6 test scenarios
  - Account-specific, shared, merged queries

### Guides
**Location:** `C:\Users\ghmd8\Documents\AI\docs\guides\`
- `template_config_future_enhancements.md` - Future feature ideas (18 enhancements documented)

### Project Management
**Location:** `C:\Users\ghmd8\Documents\AI\docs\project-management\`
- `JIRA_STORIES_BREAKDOWN.md` - ✅ JIRA tickets breakdown (2025-11-09)
  - 18 user stories across 4 sprints
  - Story points, effort estimates, and acceptance criteria
  - 4 priority levels: Critical (8 SP), High (24 SP), Medium (24 SP), Low (7 SP)
  - Total effort: 38-51.5 hours
  - Role allocation: Junior (2-3h), Mid (12-15h), Senior (24-33.5h)
  - Complete with technical details, testing, and dependencies

### Instructions
**Location:** `C:\Users\ghmd8\Documents\AI\actual\documentation\`
- `instructions.md` - Requirements and implementation instructions
  - Original requirement for template_config feature (lines 32-36)

---

## Database

### PostgreSQL Docker Container
- **Container Name:** documenthub-postgres
- **Port:** 5433 (host) → 5432 (container)
- **Database Name:** documenthub
- **User:** postgres
- **Password:** postgres123

### Connection String
```
R2DBC: r2dbc:postgresql://localhost:5433/documenthub
JDBC (Flyway): jdbc:postgresql://localhost:5433/documenthub
```

### Database Schema

#### Tables
1. `master_template_definition` - Template metadata
   - **Primary Key:** (template_id, version) - Composite key
   - **New Column:** template_config JSONB (added 2025-11-09)
   - **Indexes:**
     - idx_template_config_default_print_vendor (GIN)
     - idx_template_config_default_email_vendor (GIN)

2. `storage_index` - Document storage index
   - **Primary Key:** storage_index_id
   - **Foreign Key:** template_id → master_template_definition

---

## Test Data

### Test Account
- **Account ID:** 770e8400-e29b-41d4-a716-446655440001
- **Customer ID:** 880e8400-e29b-41d4-a716-446655440001

### Test Documents
- 4 account-specific documents in storage_index
- 5 shared templates (2 applicable: "all" + "credit_card_account_only")
- Expected result: 6 total documents

### Test Template with template_config
- **Template ID:** a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d
- **Template Name:** Test Credit Card Disclosure Template
- **Config:** SMARTCOMM (print), SENDGRID (email), ASSENTIS (failover)

---

## Git Repository Status

### Current Branch
- **Branch:** master
- **Main Branch:** master

### Modified Files (Not Committed)
```
M ../actual/schema.yaml
M src/main/java/com/documenthub/model/entity/StorageIndex.java
M (many compiled .class files in reactive-disclosure-extractor/target/)
```

### Untracked Files
```
?? ../actual/database_schema_denormalized.md
?? ../actual/database_schema_redesigned.md
?? ../actual/schema.yaml.backup
?? FILE_LOCATION_INDEX.md (this file)
```

### Recent Commits
1. `45e0db1` - Commiting the actual code
2. `2c24101` - Update instructions for document selection logic
3. `8beab27` - Fix formatting and enhance endpoint definitions
4. `2fea978` - Update URLs and descriptions in extractor_logic.json
5. `af38769` - Revise documents-enquiry endpoint documentation

---

## Running Services

### Spring Boot Application
- **Status:** ✅ Running
- **Port:** 8080
- **JAR:** document-hub-service-1.0.0-SNAPSHOT.jar
- **Process ID:** Check with `netstat -ano | findstr :8080`

### Available Endpoints
```
GET  http://localhost:8080/api/test/health
POST http://localhost:8080/api/test/documents-enquiry
```

---

## Disabled/Broken Files (To Be Fixed Later)

### Services with Compilation Errors
**Location:** Temporarily moved out of compilation path
- DocumentEnquiryService.java
- DocumentMappingService.java
- SharedDocumentEligibilityService.java
- GenericExtractionEngine.java.bak
- RuleEvaluationService.java
- TransformationService.java

**Reason:** Method signature mismatches, missing entity methods, type incompatibilities

**Workaround:** Created DocumentEnquiryTestController.java using DatabaseClient + raw SQL

---

## Important Decisions & Patterns

### R2DBC Composite Key Handling
- **Issue:** R2DBC doesn't support multiple @Id annotations
- **Solution:** Use template_id as @Id, version as regular column
- **Database:** Still enforces composite primary key constraint
- **Access:** Use DatabaseClient with raw SQL for full control

### Reactive Pattern
- **Framework:** Spring WebFlux with Reactor
- **Database:** R2DBC (non-blocking)
- **Types:** Mono<T> for single values, Flux<T> for streams

### Database Migration
- **Tool:** Flyway
- **Location:** src/main/resources/db/migration/
- **Naming:** V{version}__{description}.sql
- **Execution:** Automatic on application startup (baseline-on-migrate: true)

---

## Quick Reference Commands

### Build Application
```bash
cd /c/Users/ghmd8/Documents/AI/document-hub-service
mvn clean package -DskipTests
```

### Start Application
```bash
java -jar target/document-hub-service-1.0.0-SNAPSHOT.jar --server.port=8080
```

### Database Access
```bash
docker exec -it documenthub-postgres psql -U postgres -d documenthub
```

### Run Flyway Migration
```bash
docker exec -i documenthub-postgres psql -U postgres -d documenthub < V2__add_template_config_column.sql
```

### Kill Process on Port 8080
```bash
netstat -ano | findstr :8080
taskkill //F //PID <PID>
```

---

## File Creation Timeline (Recent)

### 2025-11-09
1. ✅ `V2__add_template_config_column.sql` - Database migration
2. ✅ `template_config_future_enhancements.md` - Future features documentation
3. ✅ `template_config_deployment_test_report.md` - Deployment test results
4. ✅ `document_enquiry_api_test_report.md` - Database logic tests
5. ✅ `document_enquiry_test_queries.sql` - SQL test queries
6. ✅ `DocumentEnquiryTestController.java` - Working test controller (fixed summary bug)
7. ✅ `document_enquiry_rest_endpoint_test_report.md` - REST API test results
8. ✅ `Document_Hub_API.postman_collection.json` - Postman collection
9. ✅ `FILE_LOCATION_INDEX.md` - This index file
10. ✅ `api_spec_validation_and_pending_items.md` - API validation & pending tasks report
11. ✅ `JIRA_STORIES_BREAKDOWN.md` - Complete JIRA tickets with 18 stories

---

## Notes

- All JSONB fields use Jackson's JsonNode type
- Jakarta validation (not javax) for Spring Boot 3.x
- PostgreSQL GIN indexes for efficient JSONB queries
- Flyway baseline-on-migrate enabled for existing database
- Test endpoints use DatabaseClient to bypass broken services

---

**End of Index**
