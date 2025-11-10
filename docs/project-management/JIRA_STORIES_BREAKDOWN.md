# Document Hub API - JIRA Stories Breakdown

**Project:** Document Hub Service
**Epic:** Document Enquiry API Implementation
**Version:** 1.0
**Created:** 2025-11-09
**Total Stories:** 18
**Total Estimated Effort:** 27.5-43 hours

---

## Epic Structure

```
EPIC-001: Document Enquiry API Spec Compliance
├── EPIC-001.1: API Contract Compliance (Sprint 1)
├── EPIC-001.2: Feature Completeness (Sprint 2)
├── EPIC-001.3: Service Layer Fixes (Sprint 3)
└── EPIC-001.4: Production Readiness (Sprint 4)
```

---

# Sprint 1: API Contract Compliance (Week 1)

**Sprint Goal:** Make test endpoint compliant with OpenAPI specification
**Sprint Duration:** 1 week (5 days)
**Total Story Points:** 8 SP
**Total Estimated Effort:** 3.5-4.5 hours

---

## STORY-101: Fix Response Field Naming Convention

**Type:** Bug
**Priority:** Critical
**Story Points:** 2 SP
**Estimated Hours:** 0.5 hours
**Assignee:** Backend Developer (Junior/Mid)

### User Story
```
As an API consumer
I want response fields in camelCase format
So that the API response matches the OpenAPI specification
```

### Description
The current test endpoint returns fields in snake_case format (document_id, display_name, date_posted), but the OpenAPI spec requires camelCase (documentId, displayName, datePosted).

### Current Behavior
```json
{
  "document_id": "660e8400-...",
  "display_name": "Monthly Statement",
  "doc_type": "monthly_statement",
  "date_posted": 1762403484,
  "is_shared": false
}
```

### Expected Behavior
```json
{
  "documentId": "660e8400-...",
  "displayName": "Monthly Statement",
  "documentType": "monthly_statement",
  "datePosted": 1762403484
}
```

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java` (line 33-47)

**SQL Query Changes:**
```sql
-- Before:
SELECT
    si.storage_index_id::text as document_id,
    mt.template_name as display_name,
    si.doc_type,
    si.doc_creation_date as date_posted

-- After:
SELECT
    si.storage_index_id::text as documentId,
    mt.template_name as displayName,
    si.doc_type as documentType,
    EXTRACT(EPOCH FROM si.doc_creation_date)::bigint as datePosted
```

**Shared Documents Query Changes (line 51-63):**
```sql
-- Update all field aliases to camelCase
SELECT
    template_id::text as documentId,
    template_name as displayName,
    category,
    doc_type as documentType,
    EXTRACT(EPOCH FROM effective_date)::bigint as datePosted
```

### Acceptance Criteria
- [ ] All response fields use camelCase naming
- [ ] Field names match OpenAPI schema exactly:
  - [ ] documentId (not document_id)
  - [ ] displayName (not display_name)
  - [ ] documentType (not doc_type)
  - [ ] datePosted (not date_posted)
- [ ] Test endpoint returns valid JSON with correct field names
- [ ] Existing tests updated to use new field names
- [ ] Postman collection updated with new field names

### Testing
```bash
# Test command
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{"accountId": "770e8400-e29b-41d4-a716-446655440001", "customerId": "880e8400-e29b-41d4-a716-446655440001"}'

# Expected: All fields in camelCase
```

### Dependencies
- None

### Related Stories
- STORY-102 (Remove non-spec fields)

---

## STORY-102: Add Pagination Support to Response

**Type:** Story
**Priority:** Critical
**Story Points:** 3 SP
**Estimated Hours:** 1.5 hours
**Assignee:** Backend Developer (Mid)

### User Story
```
As an API consumer
I want paginated responses with metadata
So that I can efficiently retrieve large document lists in chunks
```

### Description
Add pagination object to response as required by OpenAPI spec. Currently returns all results without pagination metadata.

### Current Behavior
```json
{
  "documentList": [...],
  "summary": {...}
}
```

### Expected Behavior
```json
{
  "documentList": [...],
  "pagination": {
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8,
    "pageNumber": 0
  },
  "_links": {...}
}
```

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**Step 1: Add pagination parameters to request handling**
```java
@PostMapping("/documents-enquiry")
public Mono<Map<String, Object>> getDocuments(@RequestBody Map<String, Object> request) {
    // Extract pagination params with defaults
    int pageNumber = (Integer) request.getOrDefault("pageNumber", 0);
    int pageSize = (Integer) request.getOrDefault("pageSize", 20);

    // Validate
    if (pageSize > 100) pageSize = 100; // Max page size
    if (pageNumber < 0) pageNumber = 0;
```

**Step 2: Add count query**
```java
// Count total account-specific documents
String countAccountDocsQuery = """
    SELECT COUNT(*)
    FROM storage_index si
    WHERE si.account_key = :accountId
      AND si.customer_key = :customerId
      AND si.archive_indicator = false
      AND si.is_accessible = true
    """;

// Count total shared documents
String countSharedDocsQuery = """
    SELECT COUNT(*)
    FROM master_template_definition
    WHERE is_shared_document = true
      AND archive_indicator = false
      AND sharing_scope IN ('all', 'credit_card_account_only')
    """;
```

**Step 3: Add LIMIT/OFFSET to queries**
```sql
-- Account docs query
ORDER BY si.doc_creation_date DESC
LIMIT :pageSize OFFSET :offset

-- Calculate offset
int offset = pageNumber * pageSize;
```

**Step 4: Build pagination response**
```java
Map<String, Object> pagination = new HashMap<>();
pagination.put("pageSize", pageSize);
pagination.put("totalItems", totalCount);
pagination.put("totalPages", (int) Math.ceil(totalCount / (double) pageSize));
pagination.put("pageNumber", pageNumber);
response.put("pagination", pagination);
```

### Acceptance Criteria
- [ ] Request accepts optional pageNumber parameter (default: 0)
- [ ] Request accepts optional pageSize parameter (default: 20, max: 100)
- [ ] Response includes pagination object with all 4 required fields
- [ ] Total count query executes correctly
- [ ] LIMIT/OFFSET applied correctly to data query
- [ ] Edge cases handled:
  - [ ] pageNumber < 0 defaults to 0
  - [ ] pageSize > 100 caps at 100
  - [ ] Empty result sets return totalItems: 0
- [ ] Performance: Count query executes in < 10ms

### Testing
```bash
# Test page 0
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{"accountId": "...", "customerId": "...", "pageNumber": 0, "pageSize": 2}'

# Expected: 2 documents + pagination metadata

# Test page 1
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{"accountId": "...", "customerId": "...", "pageNumber": 1, "pageSize": 2}'

# Expected: Next 2 documents + pagination metadata
```

### Dependencies
- None

### Related Stories
- STORY-105 (Add sorting support)

---

## STORY-103: Add HATEOAS Links (_links) to Response

**Type:** Story
**Priority:** Critical
**Story Points:** 2 SP
**Estimated Hours:** 1 hour
**Assignee:** Backend Developer (Mid)

### User Story
```
As an API consumer
I want hypermedia links in responses
So that I can navigate the API without hardcoding URLs
```

### Description
Add _links object to response as required by OpenAPI spec for RESTful navigation.

### Expected Behavior
```json
{
  "documentList": [...],
  "pagination": {...},
  "_links": {
    "self": {
      "href": "/api/v1/documents-enquiry"
    },
    "next": {
      "href": "/api/v1/documents-enquiry?pageNumber=1&pageSize=20"
    },
    "prev": {
      "href": "/api/v1/documents-enquiry?pageNumber=0&pageSize=20"
    }
  }
}
```

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**Implementation:**
```java
// Build _links object
Map<String, Object> links = new HashMap<>();

// Self link
Map<String, String> selfLink = new HashMap<>();
selfLink.put("href", "/api/v1/documents-enquiry");
links.put("self", selfLink);

// Next link (if not last page)
if (pageNumber < totalPages - 1) {
    Map<String, String> nextLink = new HashMap<>();
    nextLink.put("href", String.format(
        "/api/v1/documents-enquiry?pageNumber=%d&pageSize=%d",
        pageNumber + 1, pageSize
    ));
    links.put("next", nextLink);
}

// Previous link (if not first page)
if (pageNumber > 0) {
    Map<String, String> prevLink = new HashMap<>();
    prevLink.put("href", String.format(
        "/api/v1/documents-enquiry?pageNumber=%d&pageSize=%d",
        pageNumber - 1, pageSize
    ));
    links.put("prev", prevLink);
}

response.put("_links", links);
```

**Add download links for each document:**
```java
// Inside document mapping loop
for (Map<String, Object> doc : documentList) {
    Map<String, Object> docLinks = new HashMap<>();
    Map<String, String> downloadLink = new HashMap<>();
    downloadLink.put("href", "/api/v1/documents/" + doc.get("documentId"));
    docLinks.put("download", downloadLink);
    doc.put("_links", docLinks);
}
```

### Acceptance Criteria
- [ ] Response includes _links object at root level
- [ ] Self link always present
- [ ] Next link present when not on last page
- [ ] Previous link present when not on first page
- [ ] Each document includes _links.download
- [ ] Links use correct URL format
- [ ] Query parameters properly encoded

### Testing
```bash
# Test first page (should have next, no prev)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": "...", "pageNumber": 0, "pageSize": 2}'

# Test middle page (should have both next and prev)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": "...", "pageNumber": 1, "pageSize": 2}'

# Test last page (should have prev, no next)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": "...", "pageNumber": 2, "pageSize": 2}'
```

### Dependencies
- STORY-102 (Pagination support required for next/prev links)

---

## STORY-104: Fix accountId Request Parameter Type

**Type:** Bug
**Priority:** High
**Story Points:** 1 SP
**Estimated Hours:** 0.5 hours
**Assignee:** Backend Developer (Junior)

### User Story
```
As an API consumer
I want to filter by multiple account IDs
So that I can retrieve documents for multiple accounts in one request
```

### Description
OpenAPI spec defines accountId as array of UUIDs, but test endpoint accepts single UUID string.

### Current Behavior
```json
{
  "accountId": "770e8400-e29b-41d4-a716-446655440001"
}
```

### Expected Behavior
```json
{
  "accountId": ["770e8400-e29b-41d4-a716-446655440001", "880e8400-e29b-41d4-a716-446655440002"]
}
```

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**Changes:**
```java
// Before:
UUID accountId = UUID.fromString((String) request.get("accountId"));

// After:
List<String> accountIds = (List<String>) request.get("accountId");
UUID[] accountUuids = accountIds.stream()
    .map(UUID::fromString)
    .toArray(UUID[]::new);
```

**SQL Query Update:**
```sql
-- Before:
WHERE si.account_key = :accountId

-- After:
WHERE si.account_key = ANY(:accountIds)
```

**R2DBC Binding:**
```java
.bind("accountIds", accountUuids)
```

### Acceptance Criteria
- [ ] Request accepts accountId as array of strings
- [ ] Each accountId validated as UUID format
- [ ] SQL query uses ANY() for array filtering
- [ ] Works with single account ID in array
- [ ] Works with multiple account IDs
- [ ] Returns documents for all specified accounts
- [ ] Invalid UUID format returns 400 error

### Testing
```bash
# Single account
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["770e8400-e29b-41d4-a716-446655440001"], "customerId": "..."}'

# Multiple accounts
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["770e8400-...", "880e8400-..."], "customerId": "..."}'

# Invalid UUID (should return 400)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["invalid-uuid"], "customerId": "..."}'
```

### Dependencies
- None

---

# Sprint 2: Feature Completeness (Week 2)

**Sprint Goal:** Add missing fields and filtering capabilities
**Sprint Duration:** 1 week (5 days)
**Total Story Points:** 13 SP
**Total Estimated Effort:** 5-6 hours

---

## STORY-201: Add Missing Document Fields to Response

**Type:** Story
**Priority:** Medium
**Story Points:** 2 SP
**Estimated Hours:** 0.5 hours
**Assignee:** Backend Developer (Junior/Mid)

### User Story
```
As an API consumer
I want complete document metadata in responses
So that I can display rich document information without additional API calls
```

### Description
Add optional but useful fields from OpenAPI spec: languageCode, mimeType, sizeInMb, metadata.

### Current Missing Fields
- languageCode (from master_template_definition)
- mimeType (from storage_index)
- sizeInMb (calculated from file_size_bytes)
- metadata (JSONB from storage_index.doc_metadata)

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**SQL Query Update (Account Docs):**
```sql
SELECT
    si.storage_index_id::text as documentId,
    mt.template_name as displayName,
    mt.category,
    si.doc_type as documentType,
    EXTRACT(EPOCH FROM si.doc_creation_date)::bigint as datePosted,
    mt.language_code as languageCode,                    -- NEW
    si.mime_type as mimeType,                            -- NEW
    (si.file_size_bytes::float / 1048576.0) as sizeInMb, -- NEW (bytes to MB)
    si.doc_metadata as metadata                          -- NEW (JSONB)
FROM storage_index si
JOIN master_template_definition mt ON si.template_id = mt.template_id
```

**SQL Query Update (Shared Docs):**
```sql
SELECT
    template_id::text as documentId,
    template_name as displayName,
    category,
    doc_type as documentType,
    EXTRACT(EPOCH FROM effective_date)::bigint as datePosted,
    language_code as languageCode,        -- NEW
    'application/pdf' as mimeType,        -- NEW (default for templates)
    NULL as sizeInMb,                     -- NEW (N/A for templates)
    NULL as metadata                      -- NEW (N/A for templates)
```

### Acceptance Criteria
- [ ] languageCode included in response (e.g., "en", "es", "fr")
- [ ] mimeType included (e.g., "application/pdf", "image/png")
- [ ] sizeInMb calculated correctly (bytes / 1048576)
- [ ] sizeInMb formatted with 2 decimal places
- [ ] metadata JSONB field returned as JSON object
- [ ] Null values handled gracefully for optional fields
- [ ] Response size increase acceptable (< 20% increase)

### Testing
```bash
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["770e8400-..."], "customerId": "..."}'

# Verify response includes:
# "languageCode": "en"
# "mimeType": "application/pdf"
# "sizeInMb": 1.25
# "metadata": {"key": "value"}
```

### Dependencies
- STORY-101 (Field naming convention)

---

## STORY-202: Add Sorting Support

**Type:** Story
**Priority:** Medium
**Story Points:** 2 SP
**Estimated Hours:** 0.5 hours
**Assignee:** Backend Developer (Mid)

### User Story
```
As an API consumer
I want to sort documents by different criteria
So that I can view documents in my preferred order
```

### Description
Add sortOrder parameter to allow sorting by date in ascending or descending order.

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**Request Parameter:**
```java
String sortOrder = (String) request.getOrDefault("sortOrder", "DESC");

// Validate
if (!sortOrder.matches("^(ASC|DESC)$")) {
    sortOrder = "DESC";
}
```

**SQL Query Update:**
```sql
-- Dynamic order by clause
ORDER BY si.doc_creation_date ${sortOrder}
```

**Using String Formatting:**
```java
String accountDocsQuery = String.format("""
    SELECT ...
    ORDER BY si.doc_creation_date %s
    LIMIT :pageSize OFFSET :offset
    """, sortOrder);
```

### Acceptance Criteria
- [ ] Request accepts optional sortOrder parameter
- [ ] Valid values: "ASC", "DESC"
- [ ] Default value: "DESC"
- [ ] Invalid values default to "DESC"
- [ ] Case-insensitive validation
- [ ] SQL injection prevented (whitelist validation)
- [ ] Sorting applied to both account and shared documents

### Testing
```bash
# Descending order (newest first)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "sortOrder": "DESC"}'

# Ascending order (oldest first)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "sortOrder": "ASC"}'

# Invalid value (should default to DESC)
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "sortOrder": "INVALID"}'
```

### Dependencies
- None

---

## STORY-203: Add Date Range Filtering

**Type:** Story
**Priority:** Medium
**Story Points:** 3 SP
**Estimated Hours:** 1 hour
**Assignee:** Backend Developer (Mid)

### User Story
```
As an API consumer
I want to filter documents by date range
So that I can retrieve documents from specific time periods
```

### Description
Add postedFromDate and postedToDate filtering as specified in OpenAPI schema.

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**Request Parameters:**
```java
// Extract optional date range (epoch seconds)
Long postedFromDate = request.get("postedFromDate") != null
    ? ((Number) request.get("postedFromDate")).longValue()
    : null;

Long postedToDate = request.get("postedToDate") != null
    ? ((Number) request.get("postedToDate")).longValue()
    : null;
```

**SQL Query Update:**
```sql
SELECT ...
FROM storage_index si
WHERE si.account_key = ANY(:accountIds)
  AND si.customer_key = :customerId
  AND si.archive_indicator = false
  AND si.is_accessible = true
  -- NEW: Date range filter
  AND (:postedFromDate IS NULL OR EXTRACT(EPOCH FROM si.doc_creation_date) >= :postedFromDate)
  AND (:postedToDate IS NULL OR EXTRACT(EPOCH FROM si.doc_creation_date) <= :postedToDate)
ORDER BY si.doc_creation_date DESC
```

**Binding:**
```java
DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(accountDocsQuery)
    .bind("accountIds", accountUuids)
    .bind("customerId", customerId);

if (postedFromDate != null) {
    spec = spec.bind("postedFromDate", postedFromDate);
}
if (postedToDate != null) {
    spec = spec.bind("postedToDate", postedToDate);
}
```

### Acceptance Criteria
- [ ] postedFromDate parameter filters correctly (inclusive)
- [ ] postedToDate parameter filters correctly (inclusive)
- [ ] Both parameters optional
- [ ] Works with only postedFromDate
- [ ] Works with only postedToDate
- [ ] Works with both parameters
- [ ] Epoch timestamp format (seconds since 1970-01-01)
- [ ] Returns empty list if no documents in range
- [ ] Validates date range (from <= to)

### Testing
```bash
# Filter from date
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "postedFromDate": 1757219484}'

# Filter to date
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "postedToDate": 1762403484}'

# Filter date range
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "postedFromDate": 1757219484, "postedToDate": 1762403484}'

# Invalid range (from > to) - should return 400
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "postedFromDate": 1762403484, "postedToDate": 1757219484}'
```

### Dependencies
- None

---

## STORY-204: Add Category and Document Type Filtering

**Type:** Story
**Priority:** Medium
**Story Points:** 3 SP
**Estimated Hours:** 1.5 hours
**Assignee:** Backend Developer (Mid/Senior)

### User Story
```
As an API consumer
I want to filter documents by category and document type
So that I can retrieve only specific types of documents
```

### Description
Add documentTypeCategoryGroup filtering as specified in OpenAPI schema. Allows filtering by category and multiple document types per category.

### Request Schema
```json
{
  "documentTypeCategoryGroup": [
    {
      "category": "Statement",
      "documentTypes": ["monthly_statement", "annual_statement"]
    },
    {
      "category": "PaymentConfirmationNotice",
      "documentTypes": ["PaymentLetter"]
    }
  ]
}
```

### Technical Details
**File to Update:** `DocumentEnquiryTestController.java`

**Request Parameter Parsing:**
```java
List<Map<String, Object>> categoryGroups =
    (List<Map<String, Object>>) request.get("documentTypeCategoryGroup");

// Build filter conditions
if (categoryGroups != null && !categoryGroups.isEmpty()) {
    List<String> conditions = new ArrayList<>();

    for (Map<String, Object> group : categoryGroups) {
        String category = (String) group.get("category");
        List<String> docTypes = (List<String>) group.get("documentTypes");

        if (docTypes == null || docTypes.isEmpty()) {
            // Filter by category only
            conditions.add(String.format("si.category = '%s'", category));
        } else {
            // Filter by category and types
            String typesIn = docTypes.stream()
                .map(t -> "'" + t + "'")
                .collect(Collectors.joining(", "));
            conditions.add(String.format(
                "(si.category = '%s' AND si.doc_type IN (%s))",
                category, typesIn
            ));
        }
    }

    String categoryFilter = " AND (" + String.join(" OR ", conditions) + ")";
}
```

**SQL Query Update:**
```sql
WHERE si.account_key = ANY(:accountIds)
  AND si.customer_key = :customerId
  AND si.archive_indicator = false
  ${categoryFilter}  -- Dynamic category/type filter
```

### Acceptance Criteria
- [ ] Accepts array of category groups
- [ ] Each group can specify category + documentTypes
- [ ] Empty documentTypes array filters by category only
- [ ] Multiple categories supported (OR logic)
- [ ] Multiple document types per category supported
- [ ] SQL injection prevented (parameterized queries)
- [ ] Returns empty list if no matches
- [ ] Works in combination with other filters

### Testing
```bash
# Filter by category only
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{
    "accountId": ["..."],
    "customerId": "...",
    "documentTypeCategoryGroup": [
      {"category": "Statement"}
    ]
  }'

# Filter by category and types
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{
    "accountId": ["..."],
    "customerId": "...",
    "documentTypeCategoryGroup": [
      {
        "category": "Statement",
        "documentTypes": ["monthly_statement"]
      }
    ]
  }'

# Multiple categories
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{
    "accountId": ["..."],
    "customerId": "...",
    "documentTypeCategoryGroup": [
      {"category": "Statement", "documentTypes": ["monthly_statement"]},
      {"category": "PaymentConfirmationNotice", "documentTypes": ["PaymentLetter"]}
    ]
  }'
```

### Dependencies
- None

### Security Considerations
- ⚠️ SQL injection risk - MUST use parameterized queries or prepared statements
- Input validation required for category and documentType values

---

## STORY-205: Add Request Validation Layer

**Type:** Story
**Priority:** Medium
**Story Points:** 3 SP
**Estimated Hours:** 2 hours
**Assignee:** Backend Developer (Mid/Senior)

### User Story
```
As a system administrator
I want invalid requests rejected with clear error messages
So that API consumers can quickly identify and fix request issues
```

### Description
Create validation layer for DocumentEnquiryRequest with proper error messages.

### Technical Details
**New Class:** `DocumentEnquiryRequestValidator.java`

**Location:** `src/main/java/com/documenthub/validator/`

**Implementation:**
```java
@Component
public class DocumentEnquiryRequestValidator {

    public void validate(Map<String, Object> request) {
        List<String> errors = new ArrayList<>();

        // Validate customerId
        if (!request.containsKey("customerId")) {
            errors.add("customerId is required");
        } else {
            try {
                UUID.fromString((String) request.get("customerId"));
            } catch (IllegalArgumentException e) {
                errors.add("customerId must be valid UUID");
            }
        }

        // Validate accountId array
        if (request.containsKey("accountId")) {
            Object accountIdObj = request.get("accountId");
            if (!(accountIdObj instanceof List)) {
                errors.add("accountId must be an array");
            } else {
                List<String> accountIds = (List<String>) accountIdObj;
                if (accountIds.isEmpty()) {
                    errors.add("accountId array cannot be empty");
                }
                for (int i = 0; i < accountIds.size(); i++) {
                    try {
                        UUID.fromString(accountIds.get(i));
                    } catch (IllegalArgumentException e) {
                        errors.add(String.format(
                            "accountId[%d] must be valid UUID: %s",
                            i, accountIds.get(i)
                        ));
                    }
                }
            }
        }

        // Validate pagination
        if (request.containsKey("pageNumber")) {
            int pageNumber = (Integer) request.get("pageNumber");
            if (pageNumber < 0) {
                errors.add("pageNumber must be >= 0");
            }
        }

        if (request.containsKey("pageSize")) {
            int pageSize = (Integer) request.get("pageSize");
            if (pageSize < 1 || pageSize > 100) {
                errors.add("pageSize must be between 1 and 100");
            }
        }

        // Validate date range
        if (request.containsKey("postedFromDate") &&
            request.containsKey("postedToDate")) {
            long from = ((Number) request.get("postedFromDate")).longValue();
            long to = ((Number) request.get("postedToDate")).longValue();
            if (from > to) {
                errors.add("postedFromDate must be <= postedToDate");
            }
        }

        // Validate sortOrder
        if (request.containsKey("sortOrder")) {
            String sortOrder = (String) request.get("sortOrder");
            if (!sortOrder.matches("^(ASC|DESC)$")) {
                errors.add("sortOrder must be 'ASC' or 'DESC'");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}

public class ValidationException extends RuntimeException {
    private List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
```

**Update Controller:**
```java
@RestController
@RequiredArgsConstructor
public class DocumentEnquiryTestController {

    private final DatabaseClient databaseClient;
    private final DocumentEnquiryRequestValidator validator;

    @PostMapping("/documents-enquiry")
    public Mono<Map<String, Object>> getDocuments(@RequestBody Map<String, Object> request) {
        // Validate request
        try {
            validator.validate(request);
        } catch (ValidationException e) {
            return Mono.error(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
            ));
        }

        // Process request...
    }
}
```

### Acceptance Criteria
- [ ] Validator class created and tested
- [ ] All required fields validated
- [ ] UUID format validation
- [ ] Pagination bounds validated
- [ ] Date range logic validated
- [ ] Multiple errors collected and returned
- [ ] Returns 400 Bad Request with error details
- [ ] Error response format matches spec
- [ ] Unit tests for all validation rules (>80% coverage)

### Testing
```bash
# Missing customerId
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["770e8400-..."]}'
# Expected: 400 with "customerId is required"

# Invalid UUID format
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["invalid"], "customerId": "..."}'
# Expected: 400 with "accountId[0] must be valid UUID"

# Invalid page size
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "pageSize": 500}'
# Expected: 400 with "pageSize must be between 1 and 100"

# Invalid date range
curl -X POST http://localhost:8080/api/test/documents-enquiry \
  -d '{"accountId": ["..."], "customerId": "...", "postedFromDate": 2000, "postedToDate": 1000}'
# Expected: 400 with "postedFromDate must be <= postedToDate"
```

### Dependencies
- None

---

# Sprint 3: Service Layer Fixes (Weeks 3-4)

**Sprint Goal:** Fix broken services and integrate extraction engine
**Sprint Duration:** 2 weeks (10 days)
**Total Story Points:** 21 SP
**Total Estimated Effort:** 12-18 hours

---

## STORY-301: Fix DocumentEnquiryService Compilation Errors

**Type:** Bug
**Priority:** High
**Story Points:** 5 SP
**Estimated Hours:** 3-4 hours
**Assignee:** Backend Developer (Senior)

### User Story
```
As a developer
I want DocumentEnquiryService to compile successfully
So that we can use proper service layer instead of test controller
```

### Description
Fix compilation errors in DocumentEnquiryService.java, DocumentMappingService.java, and SharedDocumentEligibilityService.java.

### Known Issues
1. Method signature mismatches
2. Missing entity methods
3. Type incompatibilities with R2DBC
4. Reactive type usage errors

### Technical Details
**Files to Fix:**
- `service/DocumentEnquiryService.java`
- `service/DocumentMappingService.java`
- `service/SharedDocumentEligibilityService.java`

**Analysis Required:**
1. Review all compilation errors
2. Check entity method signatures
3. Verify R2DBC Mono/Flux usage
4. Update method return types

**Potential Fixes:**
```java
// Example fix for reactive types
// Before:
public DocumentDTO findById(UUID id) {
    return repository.findById(id);
}

// After:
public Mono<DocumentDTO> findById(UUID id) {
    return repository.findById(id)
        .map(this::toDTO);
}
```

### Acceptance Criteria
- [ ] All compilation errors resolved
- [ ] Services compile successfully
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] No regression in existing functionality
- [ ] Code review approved
- [ ] Documentation updated

### Testing
```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Verify no errors
echo $?  # Should be 0
```

### Dependencies
- None

### Estimated Breakdown
- Analysis: 1 hour
- Fixes: 2-3 hours
- Testing: 30 minutes

---

## STORY-302: Fix GenericExtractionEngine and Rule Services

**Type:** Bug
**Priority:** High
**Story Points:** 5 SP
**Estimated Hours:** 3-4 hours
**Assignee:** Backend Developer (Senior)

### User Story
```
As a developer
I want GenericExtractionEngine to compile and work correctly
So that custom_rule evaluation can be integrated
```

### Description
Fix compilation errors in GenericExtractionEngine.java, RuleEvaluationService.java, and TransformationService.java.

### Known Issues
1. Missing dependencies (Resilience4j already added)
2. Method signature issues
3. WebClient integration errors
4. Redis cache integration errors

### Technical Details
**Files to Fix:**
- `service/GenericExtractionEngine.java.bak` (rename to .java)
- `service/RuleEvaluationService.java`
- `service/TransformationService.java`

**Dependencies Already Added:**
- ✅ Resilience4j Retry
- ✅ Spring Data Redis (reactive)
- ✅ WebClient

**Checklist:**
- [ ] Rename .bak files
- [ ] Fix import statements
- [ ] Fix method signatures
- [ ] Test WebClient integration
- [ ] Test Redis cache
- [ ] Test circuit breaker
- [ ] Test retry logic

### Acceptance Criteria
- [ ] All files compile successfully
- [ ] Unit tests pass (>80% coverage)
- [ ] Integration tests pass
- [ ] WebClient makes successful HTTP calls
- [ ] Redis caching works
- [ ] Circuit breaker triggers correctly
- [ ] Retry logic works as expected
- [ ] No performance degradation

### Testing
```bash
# Compile
mvn clean compile

# Run extraction engine tests
mvn test -Dtest=GenericExtractionEngineTest

# Integration test
curl -X POST http://localhost:8080/api/extract \
  -d '{"templateId": "550e8400-...", "accountId": "...", "customerId": "..."}'
```

### Dependencies
- STORY-301 (Service layer fixes may be needed first)

### Estimated Breakdown
- Analysis: 1 hour
- Fixes: 2 hours
- Testing: 1 hour

---

## STORY-303: Integrate GenericExtractionEngine with Document Enquiry

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 4-6 hours
**Assignee:** Backend Developer (Senior)

### User Story
```
As an API consumer
I want shared documents with custom_rule scope evaluated dynamically
So that I see only documents I'm eligible for based on real-time data
```

### Description
Integrate GenericExtractionEngine into document enquiry flow to evaluate custom_rule templates and determine eligibility.

### Current Behavior
Shared documents with scope custom_rule are not included in results.

### Expected Behavior
```
1. Query shared templates with sharing_scope = 'custom_rule'
2. For each template, extract data_extraction_config JSONB
3. Execute GenericExtractionEngine with config
4. Evaluate eligibility criteria
5. Include template in results if eligible
```

### Technical Details
**File to Update:** `DocumentEnquiryService.java`

**New Method:**
```java
public Mono<List<TemplateDTO>> evaluateCustomRuleTemplates(
    UUID customerId,
    List<UUID> accountIds
) {
    // Step 1: Get custom_rule templates
    return templateRepository
        .findByIsSharedDocumentTrueAndSharingScope("custom_rule")
        .flatMap(template -> {
            // Step 2: Execute extraction engine
            return extractionEngine.execute(
                template.getDataExtractionConfig(),
                Map.of(
                    "customerId", customerId,
                    "accountIds", accountIds
                )
            )
            .flatMap(result -> {
                // Step 3: Evaluate eligibility
                if (result.isEligible()) {
                    return Mono.just(template);
                } else {
                    return Mono.empty();
                }
            })
            .onErrorResume(error -> {
                // Step 4: Handle errors based on config
                ErrorHandling errorConfig = template
                    .getDataExtractionConfig()
                    .getErrorHandling();

                if ("exclude".equals(errorConfig.getOnExtractionFailure())) {
                    log.warn("Excluding template {} due to error: {}",
                        template.getTemplateId(), error.getMessage());
                    return Mono.empty();
                } else {
                    return Mono.error(error);
                }
            });
        })
        .collectList();
}
```

**Update Document Enquiry Flow:**
```java
public Mono<DocumentRetrievalResponse> getDocuments(
    DocumentListRequest request
) {
    Mono<List<DocumentDTO>> accountDocs = getAccountDocuments(request);
    Mono<List<DocumentDTO>> sharedDocs = getSharedDocuments(request);
    Mono<List<TemplateDTO>> customRuleDocs = evaluateCustomRuleTemplates(
        request.getCustomerId(),
        request.getAccountIds()
    );

    return Mono.zip(accountDocs, sharedDocs, customRuleDocs)
        .map(tuple -> {
            List<DocumentDTO> allDocs = new ArrayList<>();
            allDocs.addAll(tuple.getT1());  // Account docs
            allDocs.addAll(tuple.getT2());  // Shared docs
            allDocs.addAll(tuple.getT3());  // Custom rule docs

            return buildResponse(allDocs, request);
        });
}
```

### Acceptance Criteria
- [ ] GenericExtractionEngine called for custom_rule templates
- [ ] Extraction config parsed from JSONB correctly
- [ ] HTTP calls to external services work
- [ ] Eligibility criteria evaluated correctly
- [ ] Eligible templates included in response
- [ ] Ineligible templates excluded
- [ ] Error handling per template config
- [ ] Circuit breaker prevents cascading failures
- [ ] Retry logic works for transient failures
- [ ] Response time < 500ms (p95) with cache
- [ ] Response time < 2s (p95) without cache
- [ ] Integration tests pass
- [ ] Load testing shows acceptable performance

### Testing
**Unit Tests:**
```java
@Test
void shouldIncludeEligibleCustomRuleTemplate() {
    // Given: Template with balance < 5000 rule
    // When: Customer has balance $3000
    // Then: Template included in results
}

@Test
void shouldExcludeIneligibleCustomRuleTemplate() {
    // Given: Template with tenure > 5 years rule
    // When: Customer has 2 years tenure
    // Then: Template excluded from results
}

@Test
void shouldHandleExtractionError() {
    // Given: Template with extraction config
    // When: External service fails
    // Then: Template excluded if errorHandling = "exclude"
}
```

**Integration Tests:**
```bash
# Test custom rule evaluation
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -d '{
    "accountId": ["770e8400-..."],
    "customerId": "880e8400-..."
  }'

# Expected: Custom rule templates evaluated and included/excluded correctly
```

### Dependencies
- STORY-301 (DocumentEnquiryService fixed)
- STORY-302 (GenericExtractionEngine fixed)

### Performance Considerations
- Cache extraction results in Redis (5 min TTL)
- Use circuit breaker for external API calls
- Implement timeout (3s max per extraction)
- Parallel execution for multiple templates

### Estimated Breakdown
- Implementation: 3 hours
- Testing: 2 hours
- Performance tuning: 1 hour

---

## STORY-304: Create Spec-Compliant Production Endpoint

**Type:** Story
**Priority:** High
**Story Points:** 3 SP
**Estimated Hours:** 2 hours
**Assignee:** Backend Developer (Mid)

### User Story
```
As an API consumer
I want a production-ready endpoint at /api/v1/documents-enquiry
So that I can use the API in production systems
```

### Description
Create new controller at /api/v1/documents-enquiry that uses fixed services and is 100% spec-compliant.

### Technical Details
**New File:** `DocumentEnquiryController.java`

**Location:** `src/main/java/com/documenthub/controller/`

**Implementation:**
```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DocumentEnquiryController {

    private final DocumentEnquiryService documentEnquiryService;
    private final DocumentEnquiryRequestValidator validator;

    @PostMapping("/documents-enquiry")
    public Mono<DocumentRetrievalResponse> getDocuments(
        @RequestHeader(value = "X-Version", required = false) String version,
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
        @RequestHeader(value = "X-Requestor-Id", required = false) String requestorId,
        @RequestHeader(value = "X-Requestor-Type", required = false) String requestorType,
        @Valid @RequestBody DocumentListRequest request
    ) {
        log.info("Document enquiry request received: correlationId={}, customerId={}",
            correlationId, request.getCustomerId());

        // Validate request
        validator.validate(request);

        // Process via service layer
        return documentEnquiryService.getDocuments(request)
            .doOnSuccess(response ->
                log.info("Document enquiry completed: total={}, correlationId={}",
                    response.getPagination().getTotalItems(), correlationId)
            )
            .doOnError(error ->
                log.error("Document enquiry failed: correlationId={}, error={}",
                    correlationId, error.getMessage())
            );
    }
}
```

**DTO Classes:**
```java
@Data
public class DocumentListRequest {
    @NotNull
    private UUID customerId;

    private List<UUID> accountId;

    private List<DocumentCategoryGroup> documentTypeCategoryGroup;

    private Long postedFromDate;  // Epoch seconds

    private Long postedToDate;

    @Min(0)
    private Integer pageNumber = 0;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    @Pattern(regexp = "^(ASC|DESC)$")
    private String sortOrder = "DESC";
}

@Data
public class DocumentRetrievalResponse {
    @NotNull
    private List<DocumentDetailsNode> documentList;

    @NotNull
    private PaginationResponse pagination;

    @NotNull
    private Map<String, Object> _links;
}
```

### Acceptance Criteria
- [ ] Endpoint at /api/v1/documents-enquiry
- [ ] Uses proper DTOs with validation
- [ ] Uses DocumentEnquiryService (not DatabaseClient)
- [ ] Returns spec-compliant response
- [ ] All fields in camelCase
- [ ] Includes pagination object
- [ ] Includes _links object
- [ ] Supports all request parameters
- [ ] Returns proper error responses
- [ ] Logging for correlation ID tracking
- [ ] Integration tests pass
- [ ] Swagger/OpenAPI documentation

### Testing
```bash
# Test production endpoint
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "X-Correlation-Id: abc-123" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "880e8400-...",
    "accountId": ["770e8400-..."],
    "pageNumber": 0,
    "pageSize": 20
  }'

# Verify response matches spec exactly
```

### Dependencies
- STORY-301 (DocumentEnquiryService fixed)
- STORY-205 (Validator created)
- All Sprint 1 & 2 stories

---

# Sprint 4: Production Readiness (Weeks 5-6)

**Sprint Goal:** Add security, monitoring, and production features
**Sprint Duration:** 2 weeks (10 days)
**Total Story Points:** 21 SP
**Total Estimated Effort:** 17-23 hours

---

## STORY-401: Add Authentication & Authorization

**Type:** Story
**Priority:** High
**Story Points:** 8 SP
**Estimated Hours:** 6-8 hours
**Assignee:** Backend Developer (Senior)

### User Story
```
As a system administrator
I want API endpoints secured with authentication
So that only authorized users can access documents
```

### Description
Implement JWT-based authentication and role-based authorization.

### Technical Details
**New Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
```

**Security Configuration:**
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http
    ) {
        return http
            .csrf().disable()
            .authorizeExchange()
                .pathMatchers("/api/test/**").permitAll()
                .pathMatchers("/api/v1/**").authenticated()
                .anyExchange().permitAll()
            .and()
            .oauth2ResourceServer()
                .jwt()
            .and()
            .build();
    }
}
```

**JWT Validation:**
```java
@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
            .cast(BearerTokenAuthenticationToken.class)
            .flatMap(this::validateToken)
            .map(this::createAuthentication);
    }
}
```

**Customer/Account Ownership Check:**
```java
@Service
public class DocumentAccessService {

    public Mono<Boolean> canAccessDocument(
        String userId,
        UUID customerId,
        List<UUID> accountIds
    ) {
        // Verify user owns customer
        return customerService.isCustomerOwnedByUser(userId, customerId)
            .flatMap(ownsCustomer -> {
                if (!ownsCustomer) {
                    return Mono.just(false);
                }

                // Verify customer owns accounts
                return accountService
                    .areAccountsOwnedByCustomer(customerId, accountIds);
            });
    }
}
```

### Acceptance Criteria
- [ ] JWT authentication working
- [ ] Invalid tokens return 401
- [ ] Missing tokens return 401
- [ ] Expired tokens return 401
- [ ] Customer ownership verified
- [ ] Account ownership verified
- [ ] Unauthorized access returns 403
- [ ] Test endpoint remains open (no auth)
- [ ] Production endpoint requires auth
- [ ] Unit tests for security (>80% coverage)
- [ ] Integration tests with auth tokens

### Testing
```bash
# No token - should return 401
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -d '{"customerId": "...", "accountId": ["..."]}'

# Invalid token - should return 401
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Authorization: Bearer invalid-token" \
  -d '{"customerId": "...", "accountId": ["..."]}'

# Valid token, unauthorized customer - should return 403
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Authorization: Bearer <valid-token-for-different-customer>" \
  -d '{"customerId": "...", "accountId": ["..."]}'

# Valid token, authorized - should return 200
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Authorization: Bearer <valid-token>" \
  -d '{"customerId": "...", "accountId": ["..."]}'
```

### Dependencies
- STORY-304 (Production endpoint)

### Security Considerations
- JWT signing key must be in secure config (not in code)
- Token expiry: 15 minutes
- Refresh token support required
- Rate limiting per user

---

## STORY-402: Add Redis Caching for Shared Documents

**Type:** Story
**Priority:** Medium
**Story Points:** 3 SP
**Estimated Hours:** 2-3 hours
**Assignee:** Backend Developer (Mid)

### User Story
```
As a system operator
I want shared documents cached
So that API response times are faster
```

### Description
Implement Redis caching for shared document templates to reduce database load.

### Technical Details
**Cache Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
        ReactiveRedisConnectionFactory connectionFactory
    ) {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

**Service Update:**
```java
@Service
public class SharedDocumentService {

    @Cacheable(value = "shared-documents", key = "#scope")
    public Mono<List<TemplateDTO>> getSharedDocuments(String scope) {
        return templateRepository
            .findByIsSharedDocumentTrueAndSharingScope(scope)
            .collectList();
    }

    @CacheEvict(value = "shared-documents", allEntries = true)
    public Mono<Void> invalidateCache() {
        return Mono.empty();
    }
}
```

### Acceptance Criteria
- [ ] Redis cache configured
- [ ] Shared documents cached (5 min TTL)
- [ ] Cache hit improves response time by >50%
- [ ] Cache miss falls back to database
- [ ] Cache invalidation on template update
- [ ] Metrics for cache hit/miss ratio
- [ ] Unit tests for caching behavior
- [ ] Load tests show performance improvement

### Testing
```bash
# First request (cache miss)
time curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -d '{"customerId": "...", "accountId": ["..."]}'
# Expected: ~100ms

# Second request (cache hit)
time curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -d '{"customerId": "...", "accountId": ["..."]}'
# Expected: ~20ms (80% faster)

# Verify Redis
redis-cli
> KEYS shared-documents::*
> TTL shared-documents::all
```

### Dependencies
- Redis server running

---

## STORY-403: Add Monitoring and Metrics

**Type:** Story
**Priority:** Medium
**Story Points:** 5 SP
**Estimated Hours:** 3-4 hours
**Assignee:** Backend Developer (Mid/Senior)

### User Story
```
As a system operator
I want API metrics and monitoring
So that I can track performance and troubleshoot issues
```

### Description
Implement Micrometer metrics, request logging, and health checks.

### Technical Details
**Dependencies:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Metrics Configuration:**
```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "document-hub");
    }
}
```

**Custom Metrics:**
```java
@Service
@RequiredArgsConstructor
public class DocumentEnquiryService {

    private final MeterRegistry meterRegistry;

    public Mono<DocumentRetrievalResponse> getDocuments(
        DocumentListRequest request
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);

        return processRequest(request)
            .doOnSuccess(response -> {
                sample.stop(Timer.builder("document.enquiry.duration")
                    .tag("status", "success")
                    .register(meterRegistry));

                meterRegistry.counter("document.enquiry.count",
                    "status", "success").increment();

                meterRegistry.gauge("document.enquiry.result.size",
                    response.getDocumentList().size());
            })
            .doOnError(error -> {
                sample.stop(Timer.builder("document.enquiry.duration")
                    .tag("status", "error")
                    .register(meterRegistry));

                meterRegistry.counter("document.enquiry.count",
                    "status", "error",
                    "error.type", error.getClass().getSimpleName()
                ).increment();
            });
    }
}
```

**Health Checks:**
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Override
    public Mono<Health> health() {
        return databaseClient.sql("SELECT 1")
            .fetch()
            .first()
            .map(row -> Health.up().build())
            .onErrorResume(error ->
                Mono.just(Health.down(error).build())
            );
    }
}
```

**Request Logging:**
```java
@Component
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(
        ServerWebExchange exchange,
        WebFilterChain chain
    ) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
            .doFinally(signalType -> {
                long duration = System.currentTimeMillis() - startTime;

                log.info("Request: method={}, path={}, status={}, duration={}ms",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(),
                    exchange.getResponse().getStatusCode(),
                    duration
                );
            });
    }
}
```

### Acceptance Criteria
- [ ] Prometheus metrics endpoint exposed (/actuator/prometheus)
- [ ] Custom metrics tracked:
  - [ ] Request duration (histogram)
  - [ ] Request count (counter)
  - [ ] Error count by type (counter)
  - [ ] Result size (gauge)
  - [ ] Cache hit ratio (gauge)
- [ ] Health checks implemented:
  - [ ] Database connectivity
  - [ ] Redis connectivity
  - [ ] Disk space
- [ ] Request/response logging with correlation ID
- [ ] Structured logging (JSON format)
- [ ] Grafana dashboard created
- [ ] Alerts configured for error rate > 5%

### Testing
```bash
# View metrics
curl http://localhost:8080/actuator/prometheus | grep document_enquiry

# View health
curl http://localhost:8080/actuator/health

# Generate load and view metrics
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/v1/documents-enquiry \
    -d '{"customerId": "...", "accountId": ["..."]}'
done

# Check Prometheus
curl http://localhost:8080/actuator/prometheus | grep -E "(document_enquiry|http_server)"
```

### Dependencies
- Prometheus server
- Grafana server

---

## STORY-404: Add Error Responses per Spec

**Type:** Story
**Priority:** Medium
**Story Points:** 3 SP
**Estimated Hours:** 2 hours
**Assignee:** Backend Developer (Mid)

### User Story
```
As an API consumer
I want standardized error responses
So that I can handle errors consistently
```

### Description
Implement error responses matching OpenAPI spec: 400, 401, 403, 404, 503.

### Technical Details
**Error Response DTO:**
```java
@Data
@Builder
public class ErrorResponse {
    private String error;
    private String message;
    private int status;
    private long timestamp;
    private String path;
    private String correlationId;
    private List<String> errors;  // For validation errors
}
```

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
        ValidationException ex,
        ServerWebExchange exchange
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .error("Bad Request")
            .message("Validation failed")
            .status(400)
            .timestamp(System.currentTimeMillis())
            .path(exchange.getRequest().getPath().value())
            .errors(ex.getErrors())
            .build();

        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnauthorized(
        UnauthorizedException ex,
        ServerWebExchange exchange
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .error("Unauthorized")
            .message(ex.getMessage())
            .status(401)
            .timestamp(System.currentTimeMillis())
            .path(exchange.getRequest().getPath().value())
            .build();

        return Mono.just(ResponseEntity.status(401).body(error));
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleForbidden(
        ForbiddenException ex,
        ServerWebExchange exchange
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .error("Forbidden")
            .message(ex.getMessage())
            .status(403)
            .timestamp(System.currentTimeMillis())
            .path(exchange.getRequest().getPath().value())
            .build();

        return Mono.just(ResponseEntity.status(403).body(error));
    }

    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(
        NotFoundException ex,
        ServerWebExchange exchange
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .error("Not Found")
            .message(ex.getMessage())
            .status(404)
            .timestamp(System.currentTimeMillis())
            .path(exchange.getRequest().getPath().value())
            .build();

        return Mono.just(ResponseEntity.status(404).body(error));
    }

    @ExceptionHandler(TimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailable(
        TimeoutException ex,
        ServerWebExchange exchange
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .error("Service Unavailable")
            .message("Service temporarily unavailable. Please try again later.")
            .status(503)
            .timestamp(System.currentTimeMillis())
            .path(exchange.getRequest().getPath().value())
            .build();

        return Mono.just(ResponseEntity.status(503).body(error));
    }
}
```

### Acceptance Criteria
- [ ] 400 Bad Request for validation errors
- [ ] 401 Unauthorized for missing/invalid auth
- [ ] 403 Forbidden for insufficient permissions
- [ ] 404 Not Found for missing resources
- [ ] 503 Service Unavailable for timeouts
- [ ] Error responses match spec format
- [ ] Correlation ID included in errors
- [ ] Error details logged
- [ ] Unit tests for each error type

### Testing
```bash
# 400 - Validation error
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -d '{"invalidField": "value"}'

# 401 - Unauthorized
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -d '{"customerId": "...", "accountId": ["..."]}'

# 403 - Forbidden
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Authorization: Bearer <token-for-different-customer>" \
  -d '{"customerId": "...", "accountId": ["..."]}'

# 404 - Not found (if document service returns 404)

# 503 - Service unavailable (simulate timeout)
```

### Dependencies
- None

---

## STORY-405: Performance Testing and Optimization

**Type:** Task
**Priority:** Medium
**Story Points:** 2 SP
**Estimated Hours:** 2-3 hours
**Assignee:** Backend Developer (Senior) + QA

### User Story
```
As a system operator
I want to know the API can handle production load
So that I can deploy with confidence
```

### Description
Perform load testing and optimize based on results.

### Technical Details
**Load Testing Tool:** JMeter or Gatling

**Test Scenarios:**
1. **Baseline Test:**
   - 10 concurrent users
   - 100 requests
   - Expected: < 100ms p95

2. **Load Test:**
   - 100 concurrent users
   - 1000 requests
   - Expected: < 500ms p95

3. **Stress Test:**
   - 500 concurrent users
   - 5000 requests
   - Find breaking point

4. **Soak Test:**
   - 50 concurrent users
   - 1 hour duration
   - Check for memory leaks

**JMeter Script:**
```xml
<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8080</stringProp>
  <stringProp name="HTTPSampler.path">/api/v1/documents-enquiry</stringProp>
  <stringProp name="HTTPSampler.method">POST</stringProp>
  <stringProp name="HTTPSampler.postBody">
    {
      "customerId": "880e8400-e29b-41d4-a716-446655440001",
      "accountId": ["770e8400-e29b-41d4-a716-446655440001"],
      "pageSize": 20
    }
  </stringProp>
</HTTPSamplerProxy>
```

### Acceptance Criteria
- [ ] Baseline test passes (< 100ms p95)
- [ ] Load test passes (< 500ms p95)
- [ ] Stress test identifies capacity limits
- [ ] Soak test shows no memory leaks
- [ ] Test report created with graphs
- [ ] Bottlenecks identified and documented
- [ ] Optimization recommendations provided
- [ ] Database query optimization if needed
- [ ] Connection pool tuning if needed

### Performance Targets
| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| p50 response time | < 50ms | TBD | ⏳ |
| p95 response time | < 100ms | TBD | ⏳ |
| p99 response time | < 500ms | TBD | ⏳ |
| Throughput | > 100 req/s | TBD | ⏳ |
| Error rate | < 1% | TBD | ⏳ |
| CPU usage | < 70% | TBD | ⏳ |
| Memory usage | < 80% | TBD | ⏳ |

### Dependencies
- STORY-304 (Production endpoint)
- STORY-402 (Caching)
- STORY-403 (Metrics)

---

# Additional Stories (Optional/Future)

## STORY-501: API Documentation with Swagger UI

**Type:** Story
**Priority:** Low
**Story Points:** 2 SP
**Estimated Hours:** 1-2 hours

### Description
Add Swagger UI for interactive API documentation.

### Technical Details
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-webflux-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

### Acceptance Criteria
- [ ] Swagger UI accessible at /swagger-ui.html
- [ ] All endpoints documented
- [ ] Request/response examples included
- [ ] Try-it-out functionality working

---

## STORY-502: API Rate Limiting

**Type:** Story
**Priority:** Low
**Story Points:** 3 SP
**Estimated Hours:** 2-3 hours

### Description
Implement rate limiting per user/IP.

### Technical Details
Use Bucket4j or Spring Cloud Gateway rate limiter.

**Limits:**
- 100 requests per minute per user
- 1000 requests per hour per user

### Acceptance Criteria
- [ ] Rate limiting enforced
- [ ] 429 Too Many Requests returned
- [ ] Rate limit headers included
- [ ] Redis-backed rate limit storage

---

# Summary Tables

## Sprint Summary

| Sprint | Goal | Stories | Story Points | Hours | Key Deliverables |
|--------|------|---------|--------------|-------|------------------|
| Sprint 1 | API Contract Compliance | 4 | 8 SP | 3.5-4.5h | Spec-compliant test endpoint |
| Sprint 2 | Feature Completeness | 5 | 13 SP | 5-6h | Full filtering, validation |
| Sprint 3 | Service Layer Fixes | 4 | 21 SP | 12-18h | Production services, custom rules |
| Sprint 4 | Production Readiness | 5 | 21 SP | 17-23h | Auth, caching, monitoring |
| **TOTAL** | **Full Implementation** | **18** | **63 SP** | **38-51.5h** | **Production-ready API** |

## Story Priority Matrix

| Priority | Count | Story Points | Hours |
|----------|-------|--------------|-------|
| Critical | 4 | 8 SP | 3.5-4.5h |
| High | 6 | 24 SP | 17-24h |
| Medium | 7 | 24 SP | 14-17h |
| Low | 1 | 7 SP | 6-6h |

## Assignee Allocation

| Role | Stories | Story Points | Hours | Skillset Required |
|------|---------|--------------|-------|-------------------|
| Junior Dev | 2-3 | 5 SP | 2-3h | Basic Spring, SQL |
| Mid Dev | 7-8 | 25 SP | 12-15h | Spring WebFlux, R2DBC, Testing |
| Senior Dev | 7-8 | 33 SP | 24-33.5h | Architecture, Security, Performance |

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Next Review:** After Sprint 1 completion
