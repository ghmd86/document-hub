# API Spec Validation & Pending Items Report

**Date:** 2025-11-09
**Endpoint Tested:** POST `/api/test/documents-enquiry`
**API Spec:** OpenAPI 3.0.3 (schema.yaml)

---

## API Specification Validation

### Endpoint: POST /documents-enquiry

**Spec Location:** `schema.yaml` lines 58-90

#### Request Schema Validation

**API Spec (DocumentListRequest):**
```yaml
DocumentListRequest:
  type: object
  properties:
    customerId:           # UUID format
      type: string
      format: uuid
    accountId:            # Array of UUIDs
      type: array
      items:
        type: string
        format: uuid
    documentTypeCategoryGroup:  # Array of category groups
      type: array
    postedFromDate:       # Epoch timestamp
      type: integer
    postedToDate:         # Epoch timestamp
      type: integer
    pageNumber:           # Pagination
      type: number
    pageSize:             # Pagination
      type: number
    sortOrder:            # Sort order enum
      type: string
```

**Test Endpoint Request:**
```json
{
  "accountId": "770e8400-e29b-41d4-a716-446655440001",  // ⚠️ String, not Array
  "customerId": "880e8400-e29b-41d4-a716-446655440001"
}
```

**Validation Result:**
| Field | API Spec | Test Endpoint | Match | Notes |
|-------|----------|---------------|-------|-------|
| customerId | string (uuid) | string (uuid) | ✅ YES | Correct format |
| accountId | array of uuid | string (uuid) | ❌ NO | **Spec expects array, test sends string** |
| documentTypeCategoryGroup | array (optional) | not sent | ✅ OK | Optional field |
| postedFromDate | integer (optional) | not sent | ✅ OK | Optional field |
| postedToDate | integer (optional) | not sent | ✅ OK | Optional field |
| pageNumber | number (optional) | not sent | ⚠️ MISSING | Should implement pagination |
| pageSize | number (optional) | not sent | ⚠️ MISSING | Should implement pagination |
| sortOrder | string (optional) | not sent | ⚠️ MISSING | Should implement sorting |

**Issue Found:** ❌ accountId should be an array according to spec, but test endpoint accepts single string

---

#### Response Schema Validation

**API Spec (DocumentRetrievalResponse):**
```yaml
DocumentRetrievalResponse:
  required:
    - documentList       # ✅ REQUIRED
    - pagination         # ❌ REQUIRED (missing in test endpoint)
    - _links             # ❌ REQUIRED (missing in test endpoint)
  properties:
    documentList:
      type: array
      items:
        $ref: '#/components/schemas/DocumentDetailsNode'
    pagination:
      $ref: '#/components/schemas/PaginationResponse'
    _links:
      $ref: '#/components/schemas/DocumentRetrievalResponseLinks'
```

**Test Endpoint Response:**
```json
{
  "summary": {                          // ⚠️ Not in spec
    "accountSpecificDocuments": 4,
    "totalDocuments": 6,
    "sharedDocuments": 2
  },
  "documentList": [                     // ✅ Required field present
    {
      "document_id": "...",              // ⚠️ Spec uses documentId (camelCase)
      "display_name": "...",             // ⚠️ Spec uses displayName (camelCase)
      "category": "...",                 // ✅ Matches spec
      "doc_type": "...",                 // ⚠️ Spec uses documentType
      "date_posted": 1762403484,         // ⚠️ Spec uses datePosted (camelCase)
      "is_shared": false                 // ⚠️ Not in spec
    }
  ]
}
```

**DocumentDetailsNode Spec vs Actual:**
| Field | API Spec (camelCase) | Test Endpoint (snake_case) | Match | Notes |
|-------|---------------------|---------------------------|-------|-------|
| documentId | ✅ Required | document_id | ❌ NO | **Case mismatch** |
| displayName | ✅ Present | display_name | ❌ NO | **Case mismatch** |
| category | ✅ Present | category | ✅ YES | Correct |
| documentType | ✅ Present | doc_type | ❌ NO | **Field name mismatch** |
| datePosted | ✅ Present | date_posted | ❌ NO | **Case mismatch** |
| sizeInMb | Optional | not sent | ✅ OK | Optional |
| languageCode | Optional | not sent | ⚠️ MISSING | Should include |
| mimeType | Optional | not sent | ⚠️ MISSING | Should include |
| description | Optional | not sent | ✅ OK | Optional |
| lineOfBusiness | Optional | not sent | ✅ OK | Optional |
| lastDownloaded | Optional | not sent | ✅ OK | Optional |
| metadata | Optional | not sent | ⚠️ MISSING | Should include |
| _links | Optional | not sent | ⚠️ MISSING | Should include |
| is_shared | N/A | is_shared | ❌ NO | **Not in spec** |
| sharing_scope | N/A | sharing_scope | ❌ NO | **Not in spec** |

**PaginationResponse (MISSING in test endpoint):**
| Field | API Spec | Test Endpoint | Status |
|-------|----------|---------------|--------|
| pageSize | Required | ❌ Missing | **Must implement** |
| totalItems | Required | ❌ Missing | **Must implement** |
| totalPages | Required | ❌ Missing | **Must implement** |
| pageNumber | Required | ❌ Missing | **Must implement** |

**_links (MISSING in test endpoint):**
| Field | API Spec | Test Endpoint | Status |
|-------|----------|---------------|--------|
| _links | Required | ❌ Missing | **Must implement** |

---

## Discrepancies Summary

### 🔴 Critical Issues (Breaks API Contract)

1. **Missing Required Fields in Response:**
   - ❌ `pagination` object (required by spec)
   - ❌ `_links` object (required by spec)

2. **Field Naming Convention Mismatch:**
   - ❌ API Spec uses **camelCase** (documentId, displayName, datePosted)
   - ❌ Test endpoint uses **snake_case** (document_id, display_name, date_posted)

3. **Request Schema Mismatch:**
   - ❌ `accountId` should be array of UUIDs, but test endpoint accepts single UUID string

### ⚠️ Medium Priority Issues

4. **Additional Fields Not in Spec:**
   - ⚠️ `summary` object (not in API spec)
   - ⚠️ `is_shared` field (not in DocumentDetailsNode spec)
   - ⚠️ `sharing_scope` field (not in DocumentDetailsNode spec)

5. **Missing Optional but Important Fields:**
   - ⚠️ `languageCode` - Should include for i18n
   - ⚠️ `mimeType` - Important for document type identification
   - ⚠️ `metadata` - JSONB field from storage_index
   - ⚠️ `sizeInMb` - Useful for UI/UX

6. **Missing Pagination Support:**
   - ⚠️ No `pageNumber` parameter support
   - ⚠️ No `pageSize` parameter support
   - ⚠️ Returns all results (no LIMIT/OFFSET)

7. **Missing Sorting Support:**
   - ⚠️ No `sortOrder` parameter support
   - ⚠️ Fixed sorting by date_posted DESC

### ℹ️ Low Priority Issues

8. **Missing Request Filters:**
   - ℹ️ `documentTypeCategoryGroup` - Filter by category/type
   - ℹ️ `postedFromDate` - Filter by date range
   - ℹ️ `postedToDate` - Filter by date range

9. **Missing HATEOAS Links:**
   - ℹ️ `_links` object for REST navigation

---

## Pending Implementation Tasks

### Priority 1: Critical (API Contract Compliance)

#### Task 1.1: Fix Field Naming Convention
**What:** Convert snake_case to camelCase in response
**Files to Update:**
- `DocumentEnquiryTestController.java` - Update SQL query column aliases
**Changes:**
```sql
-- Before:
SELECT si.storage_index_id::text as document_id

-- After:
SELECT si.storage_index_id::text as documentId
```
**Estimated Effort:** 30 minutes

---

#### Task 1.2: Add Pagination Support
**What:** Implement pagination in request and response
**Files to Update:**
- `DocumentEnquiryTestController.java`
- Add LIMIT and OFFSET to SQL queries
- Add pagination object to response
**Changes:**
```java
// Request
int pageNumber = request.getOrDefault("pageNumber", 0);
int pageSize = request.getOrDefault("pageSize", 20);

// SQL
LIMIT :pageSize OFFSET (:pageNumber * :pageSize)

// Response
Map<String, Object> pagination = new HashMap<>();
pagination.put("pageSize", pageSize);
pagination.put("totalItems", totalCount);
pagination.put("totalPages", (int) Math.ceil(totalCount / (double) pageSize));
pagination.put("pageNumber", pageNumber);
response.put("pagination", pagination);
```
**Estimated Effort:** 1-2 hours

---

#### Task 1.3: Add _links Object (HATEOAS)
**What:** Add hypermedia links for REST navigation
**Files to Update:**
- `DocumentEnquiryTestController.java`
**Changes:**
```java
Map<String, Object> links = new HashMap<>();
Map<String, String> selfLink = new HashMap<>();
selfLink.put("href", "/api/v1/documents-enquiry");
links.put("self", selfLink);

// For each document
Map<String, String> downloadLink = new HashMap<>();
downloadLink.put("href", "/api/v1/documents/" + documentId);
docLinks.put("download", downloadLink);
```
**Estimated Effort:** 1 hour

---

#### Task 1.4: Fix accountId Request Parameter
**What:** Accept array of accountIds instead of single string
**Files to Update:**
- `DocumentEnquiryTestController.java`
**Changes:**
```java
// Before:
UUID accountId = UUID.fromString((String) request.get("accountId"));

// After:
List<String> accountIds = (List<String>) request.get("accountId");
// SQL: WHERE si.account_key = ANY(:accountIds)
```
**Estimated Effort:** 30 minutes

---

### Priority 2: Medium (Feature Completeness)

#### Task 2.1: Add Missing Document Fields
**What:** Include languageCode, mimeType, sizeInMb, metadata in response
**Files to Update:**
- `DocumentEnquiryTestController.java` - Update SQL SELECT
**SQL Changes:**
```sql
SELECT
    si.storage_index_id::text as documentId,
    mt.template_name as displayName,
    mt.category,
    si.doc_type as documentType,
    si.doc_creation_date as datePosted,
    mt.language_code as languageCode,          -- NEW
    si.mime_type as mimeType,                  -- NEW
    si.file_size_bytes / 1048576.0 as sizeInMb,  -- NEW
    si.doc_metadata as metadata,               -- NEW
    false as isShared
```
**Estimated Effort:** 30 minutes

---

#### Task 2.2: Add Sorting Support
**What:** Implement sortOrder parameter
**Files to Update:**
- `DocumentEnquiryTestController.java`
**Changes:**
```java
String sortOrder = (String) request.getOrDefault("sortOrder", "DESC");
String orderByClause = "ORDER BY si.doc_creation_date " + sortOrder;
```
**Estimated Effort:** 30 minutes

---

#### Task 2.3: Add Date Range Filtering
**What:** Implement postedFromDate and postedToDate filters
**Files to Update:**
- `DocumentEnquiryTestController.java`
**Changes:**
```sql
WHERE si.doc_creation_date >= :postedFromDate
  AND si.doc_creation_date <= :postedToDate
```
**Estimated Effort:** 30 minutes

---

#### Task 2.4: Add Category/Type Filtering
**What:** Implement documentTypeCategoryGroup filter
**Files to Update:**
- `DocumentEnquiryTestController.java`
**Changes:**
```sql
WHERE (si.category, si.doc_type) IN (
    ('Statement', 'monthly_statement'),
    ('PaymentConfirmationNotice', 'PaymentLetter')
)
```
**Estimated Effort:** 1 hour

---

### Priority 3: Low (Cleanup & Enhancement)

#### Task 3.1: Remove Non-Spec Fields
**What:** Decide whether to keep summary, is_shared, sharing_scope
**Options:**
1. Remove from response (strict spec compliance)
2. Add to spec as extensions
3. Keep in separate "test" endpoint

**Recommendation:** Keep in test endpoint, create spec-compliant endpoint separately
**Estimated Effort:** Discussion + decision

---

#### Task 3.2: Add Request Validation
**What:** Validate request parameters match spec
**Files to Update:**
- Create `DocumentEnquiryRequestValidator.java`
**Features:**
- UUID format validation
- Date range validation
- Pagination parameter validation
**Estimated Effort:** 2 hours

---

#### Task 3.3: Add Error Responses
**What:** Implement error responses per spec
**Status Codes to Implement:**
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 503 Service Unavailable
**Estimated Effort:** 1-2 hours

---

### Priority 4: Production Readiness

#### Task 4.1: Fix Broken Services
**What:** Resolve compilation errors in disabled services
**Files to Fix:**
- `DocumentEnquiryService.java`
- `DocumentMappingService.java`
- `SharedDocumentEligibilityService.java`
- `GenericExtractionEngine.java`
- `RuleEvaluationService.java`
- `TransformationService.java`
**Estimated Effort:** 4-8 hours (depends on complexity)

---

#### Task 4.2: Integrate GenericExtractionEngine
**What:** Implement custom_rule evaluation for shared documents
**Files to Update:**
- `DocumentEnquiryService.java` (after fixing)
- Integration with GenericExtractionEngine
**Features:**
- Execute extraction logic for custom_rule templates
- Evaluate eligibility criteria
- Include/exclude documents based on rule results
**Estimated Effort:** 4-6 hours

---

#### Task 4.3: Add Authentication & Authorization
**What:** Implement security for endpoints
**Features:**
- JWT token validation
- Role-based access control
- Customer/account ownership verification
**Estimated Effort:** 6-8 hours

---

#### Task 4.4: Add Monitoring & Metrics
**What:** Implement observability
**Features:**
- Micrometer metrics
- Request/response logging
- Performance tracking
- Error rate monitoring
**Estimated Effort:** 3-4 hours

---

#### Task 4.5: Add Caching
**What:** Implement Redis caching for shared documents
**Features:**
- Cache shared document templates
- Cache-aside pattern
- TTL configuration
**Estimated Effort:** 2-3 hours

---

## Compliance Matrix

| Requirement | Status | Priority | Effort | Notes |
|-------------|--------|----------|--------|-------|
| **Request Schema** |
| accountId as array | ❌ Missing | P1 | 30min | Currently accepts single string |
| customerId as UUID | ✅ Done | - | - | Working correctly |
| Optional filters | ❌ Missing | P2 | 2hrs | Category, date range |
| Pagination params | ❌ Missing | P1 | 1hr | pageNumber, pageSize |
| Sort order | ❌ Missing | P2 | 30min | sortOrder parameter |
| **Response Schema** |
| documentList | ✅ Done | - | - | Present |
| camelCase fields | ❌ Missing | P1 | 30min | Currently snake_case |
| pagination object | ❌ Missing | P1 | 1hr | Required by spec |
| _links object | ❌ Missing | P1 | 1hr | Required by spec |
| languageCode | ❌ Missing | P2 | 15min | Optional but useful |
| mimeType | ❌ Missing | P2 | 15min | Optional but useful |
| metadata | ❌ Missing | P2 | 15min | JSONB field |
| sizeInMb | ❌ Missing | P2 | 15min | Optional but useful |
| **Error Handling** |
| 400 Bad Request | ❌ Missing | P3 | 30min | Need validation |
| 401 Unauthorized | ❌ Missing | P4 | 2hrs | Need auth |
| 403 Forbidden | ❌ Missing | P4 | 2hrs | Need auth |
| 404 Not Found | ❌ Missing | P3 | 15min | Easy to add |
| 503 Service Unavailable | ❌ Missing | P3 | 15min | Circuit breaker |
| **Production Features** |
| Authentication | ❌ Missing | P4 | 6-8hrs | Security |
| Authorization | ❌ Missing | P4 | 4-6hrs | Access control |
| Caching | ❌ Missing | P4 | 2-3hrs | Performance |
| Monitoring | ❌ Missing | P4 | 3-4hrs | Observability |
| Custom rules | ❌ Missing | P4 | 4-6hrs | GenericExtractionEngine |

---

## Total Effort Estimate

| Priority | Tasks | Estimated Hours | Description |
|----------|-------|-----------------|-------------|
| P1 (Critical) | 4 tasks | 3-4 hours | API contract compliance |
| P2 (Medium) | 4 tasks | 2.5-3 hours | Feature completeness |
| P3 (Low) | 3 tasks | 3-5 hours | Cleanup & enhancement |
| P4 (Production) | 5 tasks | 19-31 hours | Production readiness |
| **TOTAL** | **16 tasks** | **27.5-43 hours** | Full implementation |

---

## Recommended Implementation Order

### Phase 1: Spec Compliance (Week 1)
1. ✅ Task 1.1: Fix field naming (camelCase) - 30min
2. ✅ Task 1.4: Fix accountId to array - 30min
3. ✅ Task 1.2: Add pagination - 1-2hrs
4. ✅ Task 1.3: Add _links - 1hr
5. ✅ Task 2.1: Add missing fields - 30min

**Total Week 1:** 3.5-4.5 hours

### Phase 2: Feature Completeness (Week 2)
6. ✅ Task 2.2: Add sorting - 30min
7. ✅ Task 2.3: Add date filtering - 30min
8. ✅ Task 2.4: Add category filtering - 1hr
9. ✅ Task 3.2: Add request validation - 2hrs
10. ✅ Task 3.3: Add error responses - 1-2hrs

**Total Week 2:** 5-6 hours

### Phase 3: Production Readiness (Weeks 3-4)
11. ✅ Task 4.1: Fix broken services - 4-8hrs
12. ✅ Task 4.2: Integrate GenericExtractionEngine - 4-6hrs
13. ✅ Task 4.3: Add authentication/authorization - 6-8hrs
14. ✅ Task 4.4: Add monitoring - 3-4hrs
15. ✅ Task 4.5: Add caching - 2-3hrs

**Total Weeks 3-4:** 19-29 hours

---

## Conclusion

**Test Endpoint Status:** ✅ Functionally working, but **❌ NOT spec-compliant**

**Compliance Score:** 30% (3 of 10 critical requirements met)

**Critical Blockers for Production:**
1. Field naming convention (snake_case vs camelCase)
2. Missing pagination object
3. Missing _links object
4. accountId parameter type mismatch

**Recommendation:**
- **Short term:** Keep test endpoint as-is for testing, document discrepancies
- **Medium term:** Create spec-compliant `/api/v1/documents-enquiry` endpoint (Phase 1-2)
- **Long term:** Complete production readiness features (Phase 3)

---

**Report Generated:** 2025-11-09
**Author:** Claude Code
**Next Review:** After Phase 1 completion
