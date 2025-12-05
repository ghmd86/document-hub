# Document Hub POC - File Reference Guide

**Last Updated**: December 4, 2025

---

## Project Structure Overview

```
fresh-doc-hub-poc/
├── src/main/java/com/documenthub/
│   ├── controller/          # REST Controllers
│   ├── entity/              # Database Entities
│   ├── model/               # DTOs and Config Models
│   ├── repository/          # R2DBC Repositories
│   └── service/             # Business Logic
├── src/main/resources/      # SQL and Config Files
├── src/test/java/           # Unit Tests
└── archive/                 # Obsolete fix scripts
```

---

## Key Files by Category

### 1. Service Layer (Business Logic)

| File | Location | Purpose |
|------|----------|---------|
| **DocumentEnquiryService.java** | `src/main/java/com/documenthub/service/` | Main document retrieval logic, template processing, document matching |
| **ConfigurableDataExtractionService.java** | `src/main/java/com/documenthub/service/` | API chain execution, JSONPath extraction, array unwrapping |
| **AccountMetadataService.java** | `src/main/java/com/documenthub/service/` | Mock account metadata (VIP, STANDARD segments) |
| **RuleEvaluationService.java** | `src/main/java/com/documenthub/service/` | Access control rule evaluation |

### 2. Repository Layer (Database Access)

| File | Location | Purpose |
|------|----------|---------|
| **MasterTemplateRepository.java** | `src/main/java/com/documenthub/repository/` | Template queries, findActiveTemplates() |
| **StorageIndexRepository.java** | `src/main/java/com/documenthub/repository/` | Document queries, findByReferenceKeyAndTemplate() |

### 3. Entity Layer (Database Models)

| File | Location | Purpose |
|------|----------|---------|
| **MasterTemplateDefinitionEntity.java** | `src/main/java/com/documenthub/entity/` | Template definition with data_extraction_config |
| **StorageIndexEntity.java** | `src/main/java/com/documenthub/entity/` | Document storage with reference_key matching |

### 4. Controller Layer (API Endpoints)

| File | Location | Purpose |
|------|----------|---------|
| **DocumentEnquiryController.java** | `src/main/java/com/documenthub/controller/` | POST /documents-enquiry endpoint |
| **MockApiController.java** | `src/main/java/com/documenthub/controller/` | Mock APIs for arrangements, pricing |

### 5. Configuration Models

| File | Location | Purpose |
|------|----------|---------|
| **DataExtractionConfig.java** | `src/main/java/com/documenthub/model/extraction/` | JSON config model for data extraction |
| **FieldSourceConfig.java** | `src/main/java/com/documenthub/model/extraction/` | Field extraction path config |
| **DataSourceConfig.java** | `src/main/java/com/documenthub/model/extraction/` | API endpoint configuration |

### 6. SQL Files (Active)

| File | Location | Purpose |
|------|----------|---------|
| **schema-postgres.sql** | `src/main/resources/` | PostgreSQL schema definition |
| **test-data-postgres.sql** | `src/main/resources/` | Base test data (3 templates) |
| **test-data-disclosure-example-postgres.sql** | `src/main/resources/` | CardholderAgreement template with disclosure code matching |
| **fix-api-paths.sql** | `src/main/resources/` | Adds /api/v1 to mock API URLs |
| **data.sql** | `src/main/resources/` | Auto-loaded test data |

### 7. Unit Tests

| File | Location | Purpose |
|------|----------|---------|
| **JsonPathExtractionTest.java** | `src/test/java/com/documenthub/service/` | Tests JSONPath expressions, documents filter behavior |
| **ArrayUnwrappingTest.java** | `src/test/java/com/documenthub/service/` | Tests single-element array unwrapping |

### 8. Configuration

| File | Location | Purpose |
|------|----------|---------|
| **application.properties** | `src/main/resources/` | DB connection, logging config |
| **pom.xml** | Project root | Maven dependencies |

---

## Shared Document with Custom Config Flow

### Database Configuration (CardholderAgreement Template)

The template is stored in `master_template_definition` with:
- `template_type`: 'CardholderAgreement'
- `sharing_scope`: 'CUSTOM_RULES'
- `shared_document_flag`: true
- `data_extraction_config`: JSON with API chain and documentMatching

### Key JSONPath Expression

```
$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId
```
- Returns: `["PRC-12345"]` (JSONArray)
- Code automatically unwraps to: `"PRC-12345"` (String)

### Document Matching Configuration

```json
{
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "disclosureCode",
    "referenceKeyType": "DISCLOSURE_CODE"
  }
}
```

---

## Test Account Reference

| Account ID | Segment | Pricing ID | Disclosure Code | Document |
|------------|---------|------------|-----------------|----------|
| aaaa0000-...-000000000001 | VIP | PRC-12345 | D164 | Credit_Card_Terms_D164_v1.pdf |
| aaaa0000-...-000000000002 | STANDARD | PRC-67890 | D166 | Premium_Credit_Card_Terms_D166_v1.pdf |

---

## Test Commands

### Start Application
```bash
cd fresh-doc-hub-poc
mvn spring-boot:run
```

### Test D164 (VIP Account)
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000001"]}'
```

### Test D166 (Standard Account)
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000002"]}'
```

### Run Unit Tests
```bash
mvn test -Dtest=JsonPathExtractionTest,ArrayUnwrappingTest
```

---

## Archived Files

The following obsolete fix scripts were moved to `archive/`:
- fix-jsonpath.sql
- fix-jsonpath-correct.sql
- fix-jsonpath-final.sql
- fix-jsonpath-simple.sql
- fix-jsonpath-extraction.sql
- update-to-filter-syntax.sql
- update-jsonpath-quick.sql
- fix-all-issues.sql
- fix-bit-to-boolean.sql
- update-flags.sql
- CURRENT_STATUS.md
- FIXES_APPLIED.md
- ISSUES_FOUND.md

---

## Issues Resolved

### 1. doc_metadata Type Mismatch
- **File**: `StorageIndexEntity.java:66-67`
- **Fix**: Changed `JsonNode` to `io.r2dbc.postgresql.codec.Json`

### 2. JSONPath Filter Returns Empty Array
- **Problem**: `$.content[?(@.domain == "PRICING")].domainId[0]` returned `[]`
- **Fix**: Removed `[0]` from path; code handles array unwrapping automatically

### 3. API URL Missing Context Path
- **Problem**: URLs missing `/api/v1` prefix
- **Fix**: `fix-api-paths.sql` updates database URLs

---

## Key Code Locations

### Array Unwrapping Logic
`ConfigurableDataExtractionService.java` lines 415-421:
```java
if (value instanceof List) {
    List<?> list = (List<?>) value;
    if (list.size() == 1) {
        value = list.get(0);
    }
}
```

### Document Matching Logic
`DocumentEnquiryService.java` lines 336-367:
- Parses `documentMatching` config
- Extracts `disclosureCode` from extracted data
- Queries `findByReferenceKeyAndTemplate()`

### Debug Logging
`DocumentEnquiryService.java` lines 68-78, 180-192, 320-358:
- Template loading summary
- CUSTOM_RULES check details
- Document matching query details
