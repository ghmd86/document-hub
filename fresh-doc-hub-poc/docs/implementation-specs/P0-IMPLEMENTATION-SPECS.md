# P0 Critical Items - Implementation Specifications

**Created:** December 2024
**Priority:** Critical (Blocking other work)
**Timeline:** Before Jan 15

This document provides detailed implementation specifications for all P0 items from the TODO backlog.

---

## Table of Contents

1. [P0-001: Add message_center_doc_flag Parameter](#p0-001-add-message_center_doc_flag-parameter)
2. [P0-002: Add communication_type Parameter](#p0-002-add-communication_type-parameter)
3. [P0-003: Implement message_center_doc_flag Filter](#p0-003-implement-message_center_doc_flag-filter)
4. [P0-004: Implement communication_type Filter](#p0-004-implement-communication_type-filter)
5. [P0-005: Verify line_of_business Default](#p0-005-verify-line_of_business-default)
6. [P0-006: Add workflow Column](#p0-006-add-workflow-column)
7. [P0-007: Add doc_creation_date Column](#p0-007-add-doc_creation_date-column)
8. [P0-008: Implement Posted Date Filter](#p0-008-implement-posted-date-filter)
9. [P0-009: Ensure Single Document Return](#p0-009-ensure-single-document-return)

---

## P0-001: Add message_center_doc_flag Parameter

### Summary
Add `messageCenterDocFlag` as an API request parameter to the `/documents-enquiry` endpoint.

### Business Context
- 90% of API calls are from message center (web portal)
- Non-message-center documents include internal documents, backend-only docs
- When `false`, return BOTH message center and non-message center documents

### Technical Specification

#### 1. OpenAPI Schema Change

**File:** `src/main/resources/doc-hub.yaml`

Add to `DocumentListRequest` schema (after `lineOfBusiness`):

```yaml
DocumentListRequest:
  type: object
  properties:
    # ... existing properties ...
    lineOfBusiness:
      # ... existing ...
    messageCenterDocFlag:
      type: boolean
      description: |
        Filter documents by message center eligibility.
        - true (default): Return only documents displayable in message center
        - false: Return BOTH message center and non-message center documents
      default: true
      example: true
      x-data-classification: public
```

#### 2. Generated Model Update

After regenerating from OpenAPI, the `DocumentListRequest` class will include:

```java
// In target/generated-sources/openapi/src/main/java/com/documenthub/model/DocumentListRequest.java
@JsonProperty("messageCenterDocFlag")
private Boolean messageCenterDocFlag = true; // Default value
```

#### 3. Service Layer Usage

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

```java
// Extract the flag with default value
Boolean messageCenterDocFlag = request.getMessageCenterDocFlag();
if (messageCenterDocFlag == null) {
    messageCenterDocFlag = true; // Default: show only message center docs
}

// Pass to repository query
```

### Database Mapping

| API Field | Database Column | Table |
|-----------|-----------------|-------|
| `messageCenterDocFlag` | `message_center_doc_flag` | `master_template_definition` |

**Note:** The column already exists in the entity at line 70:
```java
@Column("message_center_doc_flag")
private Boolean messageCenterDocFlag;
```

### Acceptance Criteria

- [ ] OpenAPI spec updated with new parameter
- [ ] Model regenerated with default value `true`
- [ ] Service reads parameter correctly
- [ ] Default behavior unchanged (only message center docs)
- [ ] When `false`, returns both types
- [ ] Unit tests cover both scenarios

### Estimated Effort
- OpenAPI change: 15 min
- Code generation: 5 min
- Service integration: 30 min (see P0-003)
- Unit tests: 30 min

---

## P0-002: Add communication_type Parameter

### Summary
Add `communicationType` as an API request parameter to filter templates by communication channel.

### Business Context
- Communication types: LETTER, EMAIL, SMS, PUSH
- Maps to `template_type` column in some contexts, but represents the delivery channel
- Default: LETTER (most common use case)

### Technical Specification

#### 1. OpenAPI Schema Change

**File:** `src/main/resources/doc-hub.yaml`

Add enum definition:

```yaml
components:
  schemas:
    CommunicationType:
      type: string
      description: |
        The communication channel for document delivery.
        - LETTER: Physical mail (default)
        - EMAIL: Electronic mail
        - SMS: Text message
        - PUSH: Push notification
      enum:
        - LETTER
        - EMAIL
        - SMS
        - PUSH
      default: LETTER
      x-data-classification: public
```

Add to `DocumentListRequest`:

```yaml
DocumentListRequest:
  type: object
  properties:
    # ... existing properties ...
    communicationType:
      $ref: "#/components/schemas/CommunicationType"
```

#### 2. Database Schema Consideration

**Option A:** Use existing `template_type` column (if it stores communication type)
**Option B:** Add new `communication_type` column to `master_template_definition`

**Current State:** The `template_type` column stores document types (e.g., "PrivacyPolicy", "Statement"), NOT communication types.

**Recommended:** Add new column `communication_type` to `master_template_definition`

#### 3. Entity Update

**File:** `src/main/java/com/documenthub/entity/MasterTemplateDefinitionEntity.java`

Add after `templateType`:

```java
@Column("communication_type")
private String communicationType;
```

#### 4. Database Migration

```sql
-- Add communication_type column to master_template_definition
ALTER TABLE document_hub.master_template_definition
ADD COLUMN communication_type VARCHAR(20) DEFAULT 'LETTER';

-- Update existing records (default to LETTER)
UPDATE document_hub.master_template_definition
SET communication_type = 'LETTER'
WHERE communication_type IS NULL;

-- Add constraint for valid values
ALTER TABLE document_hub.master_template_definition
ADD CONSTRAINT chk_communication_type
CHECK (communication_type IN ('LETTER', 'EMAIL', 'SMS', 'PUSH'));
```

### Acceptance Criteria

- [ ] OpenAPI spec includes `CommunicationType` enum
- [ ] OpenAPI spec includes parameter in `DocumentListRequest`
- [ ] Entity updated with new column
- [ ] Database migration script created
- [ ] Service reads parameter correctly
- [ ] Default behavior is LETTER
- [ ] Unit tests cover all communication types

### Estimated Effort
- OpenAPI change: 20 min
- Database migration: 15 min
- Entity update: 5 min
- Service integration: 30 min (see P0-004)
- Unit tests: 45 min

---

## P0-003: Implement message_center_doc_flag Filter

### Summary
Add filter logic to repository queries based on the `messageCenterDocFlag` API parameter.

### Dependencies
- **Depends on:** P0-001 (API parameter must exist)

### Technical Specification

#### 1. Repository Query Updates

**File:** `src/main/java/com/documenthub/repository/MasterTemplateRepository.java`

Add new query method:

```java
/**
 * Find active templates filtered by line of business and message center flag.
 *
 * @param lineOfBusiness The line of business filter
 * @param messageCenterDocFlag If true, only return message center docs. If false, return all.
 * @param currentDate Current epoch time for date range validation
 */
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
       "AND (:messageCenterDocFlag = false OR message_center_doc_flag = true) " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusinessAndMessageCenter(
    String lineOfBusiness,
    Boolean messageCenterDocFlag,
    Long currentDate
);
```

**Query Logic Explanation:**
- `:messageCenterDocFlag = false` → Returns ALL templates (no filter on message_center_doc_flag)
- `:messageCenterDocFlag = true` → Returns ONLY templates where `message_center_doc_flag = true`

#### 2. Combined Query (with template types)

```java
/**
 * Find active templates filtered by line of business, template types, and message center flag.
 */
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
       "AND template_type IN (:templateTypes) " +
       "AND (:messageCenterDocFlag = false OR message_center_doc_flag = true) " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusinessTypesAndMessageCenter(
    String lineOfBusiness,
    List<String> templateTypes,
    Boolean messageCenterDocFlag,
    Long currentDate
);
```

#### 3. Service Layer Update

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

Update `getDocuments()` method:

```java
// Extract message center flag with default
Boolean messageCenterDocFlag = request.getMessageCenterDocFlag();
if (messageCenterDocFlag == null) {
    messageCenterDocFlag = true;
}

// Use in template query
Flux<MasterTemplateDefinitionEntity> templateFlux;
if (!requestedTemplateTypes.isEmpty()) {
    templateFlux = templateRepository.findActiveTemplatesByLineOfBusinessTypesAndMessageCenter(
        lineOfBusiness,
        requestedTemplateTypes,
        messageCenterDocFlag,
        currentEpochTime
    );
} else {
    templateFlux = templateRepository.findActiveTemplatesByLineOfBusinessAndMessageCenter(
        lineOfBusiness,
        messageCenterDocFlag,
        currentEpochTime
    );
}
```

### Test Scenarios

| Scenario | Input | Expected Result |
|----------|-------|-----------------|
| Default (not provided) | `null` | Only `message_center_doc_flag = true` templates |
| Explicit true | `true` | Only `message_center_doc_flag = true` templates |
| Explicit false | `false` | ALL templates (both true and false) |
| NULL in database | `true` | Exclude (NULL != true) |
| NULL in database | `false` | Include (all returned) |

### Acceptance Criteria

- [ ] Repository queries updated
- [ ] Service uses new queries
- [ ] Handles NULL database values correctly
- [ ] Default behavior (true) returns only message center docs
- [ ] False returns both types
- [ ] Unit tests for all scenarios
- [ ] Integration test with real data

### Estimated Effort
- Repository methods: 30 min
- Service integration: 30 min
- Unit tests: 45 min
- Integration tests: 30 min

---

## P0-004: Implement communication_type Filter

### Summary
Add filter logic to repository queries based on the `communicationType` API parameter.

### Dependencies
- **Depends on:** P0-002 (API parameter and database column must exist)

### Technical Specification

#### 1. Repository Query Updates

**File:** `src/main/java/com/documenthub/repository/MasterTemplateRepository.java`

Add new query method:

```java
/**
 * Find active templates filtered by line of business and communication type.
 *
 * @param lineOfBusiness The line of business filter
 * @param communicationType The communication channel (LETTER, EMAIL, SMS, PUSH)
 * @param currentDate Current epoch time for date range validation
 */
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
       "AND communication_type = :communicationType " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusinessAndCommunicationType(
    String lineOfBusiness,
    String communicationType,
    Long currentDate
);
```

#### 2. Full Combined Query (All Filters)

```java
/**
 * Find active templates with ALL filters applied.
 * This is the primary query method for document enquiry.
 *
 * @param lineOfBusiness The line of business filter (CREDIT_CARD, DIGITAL_BANK, ENTERPRISE)
 * @param templateTypes List of template types to filter by (optional, empty = all)
 * @param messageCenterDocFlag Message center filter (true = only message center, false = all)
 * @param communicationType Communication channel filter (LETTER, EMAIL, SMS, PUSH)
 * @param currentDate Current epoch time for date range validation
 */
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
       "AND (:templateTypesEmpty = true OR template_type IN (:templateTypes)) " +
       "AND (:messageCenterDocFlag = false OR message_center_doc_flag = true) " +
       "AND communication_type = :communicationType " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplatesWithFilters(
    String lineOfBusiness,
    List<String> templateTypes,
    Boolean templateTypesEmpty,
    Boolean messageCenterDocFlag,
    String communicationType,
    Long currentDate
);
```

#### 3. Service Layer Update

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

```java
// Extract communication type with default
String communicationType = request.getCommunicationType();
if (communicationType == null || communicationType.isEmpty()) {
    communicationType = "LETTER"; // Default
}

// Use combined query
Flux<MasterTemplateDefinitionEntity> templateFlux = templateRepository.findActiveTemplatesWithFilters(
    lineOfBusiness,
    requestedTemplateTypes,
    requestedTemplateTypes.isEmpty(),
    messageCenterDocFlag,
    communicationType,
    currentEpochTime
);
```

### Vendor Validation (Future Enhancement)

Per John's feedback, vendors should be validated against communication type:
- Smartcom → LETTER only
- Salesforce → EMAIL only

This is tracked as P1-009 in the backlog.

### Acceptance Criteria

- [ ] Repository queries updated with communication_type filter
- [ ] Service extracts and applies the filter
- [ ] Default is LETTER
- [ ] All enum values work correctly
- [ ] Invalid values return appropriate error
- [ ] Unit tests cover all scenarios

### Estimated Effort
- Repository methods: 30 min
- Service integration: 20 min
- Validation logic: 20 min
- Unit tests: 45 min

---

## P0-005: Verify line_of_business Default

### Summary
Ensure `lineOfBusiness` defaults to `CREDIT_CARD` when not provided in the API request.

### Business Context
- 99% of usage is CREDIT_CARD
- If not provided, derive from account type OR default to CREDIT_CARD

### Current Implementation

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

The service already has logic to determine line of business:

```java
private Mono<String> determineLineOfBusiness(DocumentListRequest request, String firstAccountId) {
    // If explicitly provided in request, use it
    if (request.getLineOfBusiness() != null) {
        return Mono.just(request.getLineOfBusiness().getValue());
    }

    // Otherwise, derive from account metadata
    return accountMetadataService.getAccountMetadata(UUID.fromString(firstAccountId))
        .map(metadata -> {
            if (metadata.getLineOfBusiness() != null) {
                return metadata.getLineOfBusiness();
            }
            return AccountMetadataService.LOB_CREDIT_CARD; // Default
        })
        .defaultIfEmpty(AccountMetadataService.LOB_CREDIT_CARD);
}
```

### Verification Checklist

- [ ] Verify constant defined: `LOB_CREDIT_CARD = "CREDIT_CARD"`
- [ ] Verify fallback in `determineLineOfBusiness()`
- [ ] Verify OpenAPI spec has default value
- [ ] Add integration test for default scenario

### OpenAPI Verification

**File:** `src/main/resources/doc-hub.yaml`

```yaml
lineOfBusiness:
  type: string
  description: "Filter templates by line of business. If not provided, will be derived from account type. ENTERPRISE templates are always included regardless of this filter."
  enum:
    - CREDIT_CARD
    - DIGITAL_BANK
    - ENTERPRISE
  example: CREDIT_CARD
  x-data-classification: public
  # Note: No default specified - derived at runtime
```

### Recommendation

Keep current behavior (derive from account if possible), but ensure:
1. Final fallback is always `CREDIT_CARD`
2. Add logging when default is used
3. Document the derivation logic

### Acceptance Criteria

- [ ] Default confirmed as CREDIT_CARD
- [ ] Account-based derivation works correctly
- [ ] Fallback to default when account has no LOB
- [ ] Logging added for traceability
- [ ] Unit test confirms default behavior

### Estimated Effort
- Verification: 15 min
- Any fixes: 15 min
- Test verification: 15 min

---

## P0-006: Add workflow Column

### Summary
Add `workflow` column to `master_template_definition` table for WCM workflow selection.

### Business Context
- Used by Web Content Management (WCM) system
- Values: `2_EYES` (1 person approval), `4_EYES` (2 person approval)
- Required for template management workflow integration

### Technical Specification

#### 1. Database Migration

**File:** `src/main/resources/db/migration/V3__add_workflow_column.sql`

```sql
-- Add workflow column for WCM integration
ALTER TABLE document_hub.master_template_definition
ADD COLUMN workflow VARCHAR(20);

-- Add constraint for valid values
ALTER TABLE document_hub.master_template_definition
ADD CONSTRAINT chk_workflow
CHECK (workflow IN ('2_EYES', '4_EYES') OR workflow IS NULL);

COMMENT ON COLUMN document_hub.master_template_definition.workflow IS
'WCM workflow type: 2_EYES (single approver), 4_EYES (dual approver)';
```

#### 2. Entity Update

**File:** `src/main/java/com/documenthub/entity/MasterTemplateDefinitionEntity.java`

Add after `recordStatus`:

```java
@Column("workflow")
private String workflow;
```

#### 3. OpenAPI Schema Update (for Template Management API)

**File:** `src/main/resources/doc-hub.yaml`

Add enum:

```yaml
components:
  schemas:
    WorkflowType:
      type: string
      description: |
        WCM workflow type for template approval.
        - 2_EYES: Single person approval
        - 4_EYES: Dual person approval (maker-checker)
      enum:
        - 2_EYES
        - 4_EYES
      x-data-classification: public
```

#### 4. Data Migration (Existing Templates)

```sql
-- Set default workflow for existing templates (optional)
UPDATE document_hub.master_template_definition
SET workflow = '2_EYES'
WHERE workflow IS NULL AND regulatory_flag = false;

UPDATE document_hub.master_template_definition
SET workflow = '4_EYES'
WHERE workflow IS NULL AND regulatory_flag = true;
```

### Acceptance Criteria

- [ ] Database column added
- [ ] Entity updated
- [ ] Constraint validates values
- [ ] OpenAPI schema includes enum (for future template API)
- [ ] Existing templates migrated with default values
- [ ] Unit tests verify entity mapping

### Estimated Effort
- Database migration: 15 min
- Entity update: 5 min
- OpenAPI update: 10 min
- Data migration: 15 min
- Testing: 20 min

---

## P0-007: Add doc_creation_date Column

### Summary
Ensure `doc_creation_date` column exists in `storage_index` table for posted date filtering.

### Business Context
- `doc_creation_date` is when the document was CREATED (e.g., statement date)
- Different from `created_timestamp` which is when record was inserted into database
- External systems (e.g., statement processor) may send documents days after creation

### Current State

The column already exists in the entity:

**File:** `src/main/java/com/documenthub/entity/StorageIndexEntity.java` (line 60-61)

```java
@Column("doc_creation_date")
private Long docCreationDate;
```

### Verification Tasks

1. **Verify database column exists**
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'storage_index'
AND column_name = 'doc_creation_date';
```

2. **Verify sample data has values**
```sql
SELECT storage_index_id, doc_creation_date, created_timestamp
FROM document_hub.storage_index
LIMIT 5;
```

3. **Verify data.sql populates the column**
Check `src/main/resources/data.sql` for `doc_creation_date` values.

### If Column Missing

**Migration Script:**

```sql
-- Add doc_creation_date if not exists
ALTER TABLE document_hub.storage_index
ADD COLUMN IF NOT EXISTS doc_creation_date BIGINT;

-- Backfill with created_timestamp for existing records
UPDATE document_hub.storage_index
SET doc_creation_date = EXTRACT(EPOCH FROM created_timestamp)::BIGINT * 1000
WHERE doc_creation_date IS NULL;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_storage_index_doc_creation_date
ON document_hub.storage_index(doc_creation_date);

COMMENT ON COLUMN document_hub.storage_index.doc_creation_date IS
'Document creation date in epoch milliseconds. Used for posted date filtering.';
```

### Acceptance Criteria

- [ ] Column exists in database
- [ ] Column mapped in entity
- [ ] Sample data includes values
- [ ] Index created for performance
- [ ] Documentation updated

### Estimated Effort
- Verification: 15 min
- Migration (if needed): 20 min
- Testing: 15 min

---

## P0-008: Implement Posted Date Filter

### Summary
Implement `postedFromDate` and `postedToDate` filtering on the `doc_creation_date` column.

### Dependencies
- **Depends on:** P0-007 (doc_creation_date column must exist)

### Technical Specification

#### 1. Repository Query Updates

**File:** `src/main/java/com/documenthub/repository/StorageIndexRepository.java`

Add new query methods:

```java
/**
 * Find account-specific documents with date range filter.
 * Uses doc_creation_date (document creation) NOT created_timestamp (DB insert).
 */
@Query("SELECT * FROM document_hub.storage_index " +
       "WHERE account_key = :accountKey " +
       "AND shared_flag = false " +
       "AND accessible_flag = true " +
       "AND template_type = :templateType " +
       "AND template_version = :templateVersion " +
       "AND (:postedFromDate IS NULL OR doc_creation_date >= :postedFromDate) " +
       "AND (:postedToDate IS NULL OR doc_creation_date <= :postedToDate)")
Flux<StorageIndexEntity> findAccountSpecificDocumentsWithDateRange(
    @Param("accountKey") UUID accountKey,
    @Param("templateType") String templateType,
    @Param("templateVersion") Integer templateVersion,
    @Param("postedFromDate") Long postedFromDate,
    @Param("postedToDate") Long postedToDate
);

/**
 * Find shared documents with date range filter.
 */
@Query("SELECT * FROM document_hub.storage_index " +
       "WHERE shared_flag = true " +
       "AND accessible_flag = true " +
       "AND template_type = :templateType " +
       "AND template_version = :templateVersion " +
       "AND (:postedFromDate IS NULL OR doc_creation_date >= :postedFromDate) " +
       "AND (:postedToDate IS NULL OR doc_creation_date <= :postedToDate)")
Flux<StorageIndexEntity> findSharedDocumentsWithDateRange(
    @Param("templateType") String templateType,
    @Param("templateVersion") Integer templateVersion,
    @Param("postedFromDate") Long postedFromDate,
    @Param("postedToDate") Long postedToDate
);

/**
 * Find documents by reference key with date range filter.
 */
@Query("SELECT * FROM document_hub.storage_index " +
       "WHERE reference_key = :referenceKey " +
       "AND reference_key_type = :referenceKeyType " +
       "AND accessible_flag = true " +
       "AND template_type = :templateType " +
       "AND template_version = :templateVersion " +
       "AND (:postedFromDate IS NULL OR doc_creation_date >= :postedFromDate) " +
       "AND (:postedToDate IS NULL OR doc_creation_date <= :postedToDate)")
Flux<StorageIndexEntity> findByReferenceKeyAndTemplateWithDateRange(
    @Param("referenceKey") String referenceKey,
    @Param("referenceKeyType") String referenceKeyType,
    @Param("templateType") String templateType,
    @Param("templateVersion") Integer templateVersion,
    @Param("postedFromDate") Long postedFromDate,
    @Param("postedToDate") Long postedToDate
);
```

#### 2. Service Layer Update

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

```java
// Extract date filters from request (already in epoch seconds per OpenAPI)
Long postedFromDate = request.getPostedFromDate();
Long postedToDate = request.getPostedToDate();

// Pass to repository queries
return storageRepository.findAccountSpecificDocumentsWithDateRange(
    accountKey,
    templateType,
    templateVersion,
    postedFromDate,
    postedToDate
);
```

#### 3. Date Format Considerations

Per OpenAPI spec, dates are in **epoch seconds**:
```yaml
Date:
  type: integer
  description: |
    Epoch time in seconds
  format: int64
  example: 1740523843
```

If `doc_creation_date` is stored in **epoch milliseconds**, convert:
```java
Long postedFromDateMs = postedFromDate != null ? postedFromDate * 1000 : null;
Long postedToDateMs = postedToDate != null ? postedToDate * 1000 : null;
```

### Test Scenarios

| Scenario | postedFromDate | postedToDate | Expected |
|----------|----------------|--------------|----------|
| No filter | null | null | All documents |
| From date only | 1704067200 | null | Docs >= Jan 1, 2024 |
| To date only | null | 1735689599 | Docs <= Dec 31, 2024 |
| Range | 1704067200 | 1706745599 | Docs in Jan 2024 |
| Invalid range | 1735689600 | 1704067200 | Empty result (from > to) |

### Acceptance Criteria

- [ ] Repository queries updated with date filters
- [ ] Service extracts and passes date parameters
- [ ] Handles NULL dates correctly (no filter)
- [ ] Date unit conversion (seconds ↔ milliseconds) handled
- [ ] Edge cases (start of day, end of day) tested
- [ ] Performance acceptable with date index
- [ ] Unit tests for all scenarios

### Estimated Effort
- Repository methods: 45 min
- Service integration: 30 min
- Date conversion handling: 20 min
- Unit tests: 45 min

---

## P0-009: Ensure Single Document Return

### Summary
Ensure queries return only ONE document for a given template at the current time.

### Business Context
Per John: "Current queries must return only ONE document" for a template at any given time.
- Future-dated documents should not be returned until their effective date
- If multiple versions exist, return only the currently effective one

### Technical Specification

#### 1. Document Selection Logic

**Priority Order:**
1. Check `doc_creation_date` is within current time window
2. If template has validity dates (`start_date`, `end_date`), respect them
3. Return the MOST RECENT document if multiple match

#### 2. Repository Query Updates

**File:** `src/main/java/com/documenthub/repository/StorageIndexRepository.java`

```java
/**
 * Find the single most recent account-specific document.
 * Returns only ONE document - the most recent by doc_creation_date.
 */
@Query("SELECT * FROM document_hub.storage_index " +
       "WHERE account_key = :accountKey " +
       "AND shared_flag = false " +
       "AND accessible_flag = true " +
       "AND template_type = :templateType " +
       "AND template_version = :templateVersion " +
       "ORDER BY doc_creation_date DESC " +
       "LIMIT 1")
Mono<StorageIndexEntity> findLatestAccountSpecificDocument(
    @Param("accountKey") UUID accountKey,
    @Param("templateType") String templateType,
    @Param("templateVersion") Integer templateVersion
);

/**
 * Find the single most recent shared document.
 */
@Query("SELECT * FROM document_hub.storage_index " +
       "WHERE shared_flag = true " +
       "AND accessible_flag = true " +
       "AND template_type = :templateType " +
       "AND template_version = :templateVersion " +
       "ORDER BY doc_creation_date DESC " +
       "LIMIT 1")
Mono<StorageIndexEntity> findLatestSharedDocument(
    @Param("templateType") String templateType,
    @Param("templateVersion") Integer templateVersion
);
```

#### 3. Handling Multiple Documents Scenario

For templates that CAN have multiple documents (like statements), different logic applies:
- Statements: Multiple documents per account (one per period)
- Privacy Policy: Only one current document
- Disclosure: Only one per disclosure code

**Solution:** Add a flag to template definition to indicate behavior:

```sql
ALTER TABLE document_hub.master_template_definition
ADD COLUMN single_document_flag BOOLEAN DEFAULT true;

COMMENT ON COLUMN document_hub.master_template_definition.single_document_flag IS
'If true, only return the most recent document. If false, return all matching documents.';
```

#### 4. Service Layer Logic

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

```java
private Mono<List<StorageIndexEntity>> queryDocuments(
    MasterTemplateDefinitionEntity template,
    UUID accountKey,
    Long postedFromDate,
    Long postedToDate
) {
    // Check if template allows multiple documents
    Boolean singleDocumentFlag = template.getSingleDocumentFlag();
    if (singleDocumentFlag == null) {
        singleDocumentFlag = true; // Default: single document
    }

    if (singleDocumentFlag) {
        // Return only the latest document
        return storageRepository.findLatestAccountSpecificDocument(
            accountKey,
            template.getTemplateType(),
            template.getTemplateVersion()
        ).map(Collections::singletonList)
         .defaultIfEmpty(Collections.emptyList());
    } else {
        // Return all matching documents (e.g., all statements)
        return storageRepository.findAccountSpecificDocumentsWithDateRange(
            accountKey,
            template.getTemplateType(),
            template.getTemplateVersion(),
            postedFromDate,
            postedToDate
        ).collectList();
    }
}
```

### Alternative Approach (Without DB Change)

Use template configuration JSON to control behavior:

```json
{
  "template_config": {
    "document_selection": {
      "mode": "LATEST",  // or "ALL"
      "max_results": 1
    }
  }
}
```

### Test Scenarios

| Template Type | single_document_flag | Multiple Docs Exist | Result |
|---------------|---------------------|---------------------|--------|
| Privacy Policy | true | Yes (v1, v2) | Return latest only |
| Statement | false | Yes (Jan, Feb, Mar) | Return all |
| Disclosure | true | Yes (different codes) | Return latest per code |

### Acceptance Criteria

- [ ] Default behavior returns single document
- [ ] `LIMIT 1` queries implemented
- [ ] `ORDER BY doc_creation_date DESC` for latest
- [ ] Flag added for templates allowing multiple docs
- [ ] Service respects the flag
- [ ] Unit tests verify single vs multiple behavior
- [ ] Integration tests with real data

### Estimated Effort
- Repository methods: 30 min
- Entity/Schema update: 20 min
- Service logic: 45 min
- Configuration approach: 30 min
- Unit tests: 45 min

---

## Implementation Order

Based on dependencies:

```
Phase 1 (Can run in parallel):
├── P0-001: Add message_center_doc_flag parameter (API)
├── P0-002: Add communication_type parameter (API)
├── P0-005: Verify line_of_business default
├── P0-006: Add workflow column
└── P0-007: Add/verify doc_creation_date column

Phase 2 (Depends on Phase 1):
├── P0-003: Implement message_center_doc_flag filter (depends on P0-001)
├── P0-004: Implement communication_type filter (depends on P0-002)
└── P0-008: Implement posted date filter (depends on P0-007)

Phase 3:
└── P0-009: Ensure single document return (can be done independently but logically last)
```

---

## Files to Modify Summary

| File | Changes |
|------|---------|
| `doc-hub.yaml` | Add messageCenterDocFlag, communicationType, CommunicationType enum, WorkflowType enum |
| `MasterTemplateDefinitionEntity.java` | Add communicationType, workflow, singleDocumentFlag fields |
| `StorageIndexEntity.java` | Verify doc_creation_date exists |
| `MasterTemplateRepository.java` | Add queries with new filters |
| `StorageIndexRepository.java` | Add date range and single document queries |
| `DocumentEnquiryService.java` | Extract new parameters, use new queries |
| `schema.sql` | Add new columns |
| `data.sql` | Update sample data with new column values |

---

## Testing Checklist

### Unit Tests

- [ ] P0-001: Request deserialization with/without flag
- [ ] P0-002: CommunicationType enum parsing
- [ ] P0-003: Repository query with message center filter
- [ ] P0-004: Repository query with communication type filter
- [ ] P0-005: Default line of business derivation
- [ ] P0-008: Date range filtering
- [ ] P0-009: Single vs multiple document return

### Integration Tests

- [ ] Full request with all filters
- [ ] Default values applied correctly
- [ ] Empty results handled gracefully
- [ ] Pagination works with new filters

---

## Document History

| Date | Author | Changes |
|------|--------|---------|
| Dec 2024 | Team | Initial creation |
