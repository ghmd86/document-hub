# Document Hub POC - Implementation Plan

## Executive Summary
Implement a fully functional `/documents-enquiry` endpoint that returns both account-specific and shared documents based on dynamic rules stored in the database.

## Current Status
✅ **Completed:**
- OpenAPI code generation (models, controllers, API interfaces)
- Basic service layer structure (`DocumentEnquiryService`, `RuleEvaluationService`, `DocumentMatchingService`)
- Data extraction engine framework
- R2DBC repository interfaces with basic queries
- Spring Boot WebFlux reactive setup
- Database schema defined (`master_template_definition`, `storage_index`)

❌ **Missing:**
- Database connection configuration
- Shared document filtering logic
- Account metadata lookup
- Custom rule evaluation for shared documents
- Complete document matching implementation
- Sample test data

---

## Architecture Overview

```
Client Request
    ↓
DocumentsEnquiryApiController
    ↓
DocumentEnquiryService
    ├→ Get Active Templates (MasterTemplateDefinitionRepository)
    ├→ Data Extraction (DataExtractionEngine)
    │   ├→ Fetch account metadata
    │   ├→ Fetch customer metadata
    │   └→ Build ExtractionContext
    ├→ Rule Evaluation (RuleEvaluationService)
    │   ├→ Check eligibility criteria
    │   └→ Evaluate access control rules
    └→ Document Matching (DocumentMatchingService)
        ├→ Query account-specific documents (is_shared=false)
        ├→ Query shared documents (is_shared=true)
        │   ├→ Shared with ALL
        │   ├→ Shared by account type
        │   └→ Shared by custom rules
        └→ Merge and deduplicate results
```

---

## Implementation Phases

### Phase 1: Database Configuration ⚡ HIGH PRIORITY

#### 1.1 Configure R2DBC Connection
**File:** `application.properties`
```properties
# R2DBC PostgreSQL
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/document_hub
spring.r2dbc.username=postgres
spring.r2dbc.password=postgres

# Connection pool
spring.r2dbc.pool.initial-size=10
spring.r2dbc.pool.max-size=50
spring.r2dbc.pool.max-idle-time=30m

# Logging
logging.level.io.swagger=DEBUG
logging.level.org.springframework.r2dbc=DEBUG
```

#### 1.2 Create Database Initialization Script
**File:** `src/main/resources/schema.sql`
- Execute `database.sql` schema
- Add sample data for testing

#### 1.3 Sample Data Strategy
Create test data covering:
1. **Account-specific documents** (is_shared=false)
2. **Shared with ALL** (sharing_scope="ALL")
3. **Shared by account type** (sharing_scope="ACCOUNT_TYPE")
4. **Shared with custom rules** (sharing_scope="CUSTOM_RULES")

---

### Phase 2: Account Metadata Service ⚡ HIGH PRIORITY

#### 2.1 Create Account Metadata Model
**File:** `io/swagger/model/AccountMetadata.java`
```java
public class AccountMetadata {
    private UUID accountId;
    private UUID customerId;
    private String accountType;  // e.g., "credit_card", "digital_bank"
    private String region;       // e.g., "US_WEST", "US_EAST"
    private String state;
    private String customerSegment; // e.g., "VIP", "ENTERPRISE"
    private Long accountOpenDate;  // For tenure calculation
    // ... getters/setters
}
```

#### 2.2 Options for Account Lookup

**Option A: Mock Service (For POC)**
Create `MockAccountMetadataService` with hardcoded data

**Option B: External API Integration**
Use `DataExtractionEngine` to call external account service

**Option C: Database Table**
Create `account_metadata` table (recommended for POC)

**Recommendation:** Start with Option A (mock), make it swappable later

---

### Phase 3: Shared Document Logic ⚡ HIGH PRIORITY

#### 3.1 Update StorageIndexRepository
**File:** `io/swagger/repository/StorageIndexRepository.java`

Add methods:
```java
// Find account-specific documents
Flux<StorageIndexEntity> findAccountSpecificDocuments(
    UUID accountKey,
    String templateType,
    Integer templateVersion
);

// Find shared documents by scope
Flux<StorageIndexEntity> findSharedByScope(
    String sharingScope,
    String templateType,
    Integer templateVersion
);

// Combined query
Flux<StorageIndexEntity> findDocumentsForAccount(
    UUID accountKey,
    UUID customerKey,
    String templateType,
    Integer templateVersion
);
```

#### 3.2 Implement Sharing Scope Evaluation
**File:** `io/swagger/service/SharingRuleService.java` (new)

```java
@Service
public class SharingRuleService {

    // Evaluate if document is accessible based on sharing scope
    public boolean evaluateSharingRules(
        MasterTemplateDefinitionEntity template,
        AccountMetadata accountMetadata,
        ExtractionContext context
    ) {
        String sharingScope = template.getSharingScope();

        switch (sharingScope) {
            case "ALL":
                return true;
            case "ACCOUNT_TYPE":
                return evaluateAccountType(template, accountMetadata);
            case "CUSTOM_RULES":
                return evaluateCustomRules(template, accountMetadata, context);
            default:
                return false;
        }
    }
}
```

---

### Phase 4: Custom Rule Evaluation 🔧 MEDIUM PRIORITY

#### 4.1 Access Control JSON Structure
**Stored in:** `master_template_definition.access_control`

```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "accountType",
        "operator": "IN",
        "value": ["credit_card", "digital_bank"]
      },
      {
        "field": "region",
        "operator": "EQUALS",
        "value": "US_WEST"
      },
      {
        "field": "customerSegment",
        "operator": "IN",
        "value": ["VIP", "ENTERPRISE"]
      },
      {
        "field": "$extracted.disclosure_code",
        "operator": "MATCHES_METADATA",
        "documentField": "doc_metadata.disclosure_code"
      }
    ]
  }
}
```

#### 4.2 Extend RuleEvaluationService
**File:** `io/swagger/service/RuleEvaluationService.java`

Add support for:
- Field comparison operators: EQUALS, IN, NOT_IN, GREATER_THAN, LESS_THAN, MATCHES
- Metadata field matching
- Date range validation
- Account tenure calculation
- AND/OR logic combinations

---

### Phase 5: Document Matching Enhancement 🔧 MEDIUM PRIORITY

#### 5.1 Update DocumentMatchingService
**File:** `io/swagger/service/DocumentMatchingService.java`

Implement strategy:
1. Query account-specific documents (`is_shared=false`, `account_key=?`)
2. Query shared documents accessible to account:
   - Scope = "ALL"
   - Scope = "ACCOUNT_TYPE" AND matches account type
   - Scope = "CUSTOM_RULES" AND rules evaluate to true
3. Apply filters from `DocumentListRequest`:
   - `referenceKey` / `referenceKeyType`
   - `documentTypeCategoryGroup`
   - `postedFromDate` / `postedToDate`
4. Deduplicate and sort
5. Return combined results

#### 5.2 Document Versioning
Add logic to:
- Filter by active version only (based on `start_date`/`end_date`)
- Return latest version if multiple exist
- Support version-specific queries

---

### Phase 6: Response Mapping 🛠️ LOW PRIORITY

#### 6.1 Fix Controller Type Mismatch
**File:** `io/swagger/api/DocumentsEnquiryApiController.java`

Map `DocumentListResponse` → `DocumentRetrievalResponse`:
```java
private DocumentRetrievalResponse mapToApiResponse(DocumentListResponse serviceResponse) {
    DocumentRetrievalResponse apiResponse = new DocumentRetrievalResponse();

    // Map documents
    List<DocumentDetailsNode> documentNodes = serviceResponse.getDocuments()
        .stream()
        .map(this::mapToDocumentNode)
        .collect(Collectors.toList());

    apiResponse.setDocumentList(documentNodes);

    // Map pagination
    PaginationResponse pagination = new PaginationResponse();
    pagination.setPageSize(serviceResponse.getPageSize());
    pagination.setPageNumber(serviceResponse.getPageNumber());
    pagination.setTotalPages(serviceResponse.getTotalPages());
    pagination.setTotalItems(serviceResponse.getTotalCount());

    apiResponse.setPagination(pagination);

    // Map HATEOAS links
    // ... implementation

    return apiResponse;
}
```

---

### Phase 7: Testing & Documentation 📝 LOW PRIORITY

#### 7.1 Sample Test Scenarios

1. **Scenario 1: Account-Specific Only**
   - Request: accountId=ABC123
   - Expected: Only documents with account_key=ABC123 AND is_shared=false

2. **Scenario 2: Shared with ALL**
   - Request: accountId=ABC123
   - Expected: Account-specific + documents with sharing_scope="ALL"

3. **Scenario 3: Shared by Account Type**
   - Request: accountId=ABC123 (type: credit_card)
   - Expected: Account-specific + shared (ALL) + shared (credit_card type)

4. **Scenario 4: Custom Rules**
   - Request: accountId=ABC123 (region: US_WEST, segment: VIP)
   - Expected: All matching shared documents based on custom rules

#### 7.2 Integration Tests
**File:** `src/test/java/io/swagger/integration/DocumentEnquiryIntegrationTest.java`

#### 7.3 API Documentation
Create comprehensive API usage guide with examples

---

## Data Model Summary

### Storage Index Key Fields
```sql
-- Document identification
storage_index_id (UUID)
master_template_id (UUID)
template_version (int)

-- Sharing flags
is_shared (bit)  -- false=account-specific, true=shared

-- Account linking (for account-specific docs)
account_key (UUID)
customer_key (UUID)

-- Reference keys
reference_key (varchar)
reference_key_type (varchar)

-- Metadata (JSONB)
doc_metadata (jsonb) {
  "disclosure_code": "D164",
  "region": "US_WEST",
  "custom_field": "value"
}
```

### Master Template Definition Key Fields
```sql
-- Sharing configuration
is_shared_document (bit)
sharing_scope (varchar)  -- "ALL", "ACCOUNT_TYPE", "CUSTOM_RULES"

-- Access control rules (JSONB)
access_control (jsonb) {
  "eligibilityCriteria": { ... },
  "accountTypes": ["credit_card", "digital_bank"],
  "customRules": { ... }
}

-- Data extraction config (JSONB)
data_extraction_config (jsonb)
```

---

## Next Steps

### Immediate Actions (This Session)
1. ✅ Create this implementation plan
2. ⏳ Add database configuration to application.properties
3. ⏳ Create MockAccountMetadataService
4. ⏳ Implement SharingRuleService
5. ⏳ Update DocumentMatchingService with shared document logic

### Follow-up Actions
1. Create sample data SQL script
2. Implement custom rule evaluation
3. Add integration tests
4. Performance optimization (caching, query optimization)
5. Documentation

---

## Key Design Decisions

### Decision 1: Account Metadata Source
**Chosen:** Mock service for POC, designed for easy swap to external API

### Decision 2: Rule Storage Format
**Chosen:** JSON in `access_control` field (flexible, no schema changes)

### Decision 3: Sharing Scope Strategy
**Chosen:** Three-tier approach (ALL / ACCOUNT_TYPE / CUSTOM_RULES)

### Decision 4: Query Strategy
**Chosen:** Separate queries merged in service layer (clearer logic, easier testing)

---

## Risk & Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Complex rule evaluation performance | High | Add caching layer, optimize queries |
| Rule definition complexity | Medium | Provide GUI tool for non-technical users |
| Account metadata unavailable | High | Implement fallback/retry logic |
| Large result sets | Medium | Implement pagination, limit max results |

---

## Success Criteria

✅ POC is successful when:
1. Endpoint returns both account-specific and shared documents
2. Shared documents filtered by ALL/ACCOUNT_TYPE/CUSTOM_RULES
3. Custom rules evaluate correctly (metadata matching, date ranges, etc.)
4. Response properly paginated
5. Performance acceptable (<2s for typical queries)
6. Non-technical team can configure rules via JSON examples
