# Document Hub - Use Cases & Design Review

This document provides a comprehensive analysis of all use cases supported by the Document Hub service, along with a review of the database schema, data extraction logic, and API design.

## Table of Contents
1. [Use Case Summary](#1-use-case-summary)
2. [Detailed Use Cases](#2-detailed-use-cases)
3. [Database Schema Review](#3-database-schema-review)
4. [Data Extraction Logic Review](#4-data-extraction-logic-review)
5. [API Specification Review](#5-api-specification-review)
6. [Use Case Fulfillment Matrix](#6-use-case-fulfillment-matrix)
7. [Recommendations](#7-recommendations)

---

## 1. Use Case Summary

The Document Hub service supports **10 primary use cases**:

| # | Use Case | Sharing Scope | Description |
|---|----------|---------------|-------------|
| 1 | Account-Specific Documents | `shared_flag=false` | Customer's own documents (statements, invoices) |
| 2 | All-Customers Documents | `ALL` | Universal docs (privacy policy, T&C) |
| 3 | Account-Type Based Sharing | `ACCOUNT_TYPE` | Documents for specific products |
| 4 | Custom Rules (Static) | `CUSTOM_RULES` | Multi-criteria eligibility (VIP + Region) |
| 5 | Custom Rules (Dynamic) | `CUSTOM_RULES` + extraction | Extract data from APIs, then evaluate |
| 6 | Reference Key Matching | `CUSTOM_RULES` | Match by extracted codes |
| 7 | Validity Period Enforcement | All | Filter by valid_from/valid_until |
| 8 | Multi-Account Retrieval | All | Query across multiple accounts |
| 9 | Pagination | All | Page through large result sets |
| 10 | Multi-Requestor Support | All | CUSTOMER, AGENT, SYSTEM access |

---

## 2. Detailed Use Cases

### UC-1: Account-Specific Documents

**Actor:** Customer/Account Holder

**Description:** Customer requests documents tied to their specific account (statements, invoices, transaction history).

**Configuration:**
```sql
-- Template
shared_document_flag = false
sharing_scope = NULL

-- Document
shared_flag = false
account_key = <customer's account UUID>
```

**Flow:**
```
Request → Find templates where shared_flag=false
        → Query storage_index by account_key
        → Return only matching account's documents
```

**Example Documents:**
- Monthly credit card statements
- Account-specific tax documents
- Personalized transaction reports

---

### UC-2: All-Customers Shared Documents

**Actor:** Any Customer

**Description:** Universal documents available to everyone without restrictions.

**Configuration:**
```sql
-- Template
shared_document_flag = true
sharing_scope = 'ALL'
template_config = NULL  -- No eligibility criteria needed
```

**Flow:**
```
Request → Find templates where sharing_scope='ALL'
        → Query all shared documents for template
        → No eligibility check needed
        → Return documents
```

**Example Documents:**
- Privacy Policy
- Terms and Conditions
- General regulatory disclosures

---

### UC-3: Account-Type Based Sharing

**Actor:** Customer with specific account type

**Description:** Documents targeted to specific product types (credit cards, savings, etc.).

**Configuration:**
```sql
-- Template
shared_document_flag = true
sharing_scope = 'ACCOUNT_TYPE'
template_config = '{
  "eligibility_criteria": {
    "operator": "AND",
    "rules": [
      {"field": "accountType", "operator": "IN", "value": ["credit_card"]}
    ]
  }
}'
```

**Flow:**
```
Request → Find templates where sharing_scope='ACCOUNT_TYPE'
        → Evaluate: request.accountType matches rule?
        → If yes, return shared documents
        → If no, skip template
```

**Example Documents:**
- Balance transfer offers (credit card only)
- Savings account bonus materials
- Product-specific feature guides

---

### UC-4: Custom Rules (Static)

**Actor:** Customer matching complex eligibility criteria

**Description:** Documents with multiple conditions based on known customer attributes.

**Configuration:**
```sql
-- Template
shared_document_flag = true
sharing_scope = 'CUSTOM_RULES'
template_config = '{
  "eligibility_criteria": {
    "operator": "AND",
    "rules": [
      {"field": "customerSegment", "operator": "EQUALS", "value": "VIP"},
      {"field": "region", "operator": "EQUALS", "value": "US_WEST"}
    ]
  }
}'
data_extraction_config = NULL  -- No external API calls
```

**Flow:**
```
Request → Find templates where sharing_scope='CUSTOM_RULES'
        → No extraction config, use request context directly
        → Evaluate eligibility_criteria against AccountMetadata
        → If pass, return shared documents
```

**Example Documents:**
- VIP exclusive offers
- Region-specific promotions
- Enterprise-tier feature announcements

---

### UC-5: Custom Rules (Dynamic Extraction)

**Actor:** Customer whose eligibility depends on dynamically extracted data

**Description:** Documents matched based on real-time data fetched from external APIs.

**Configuration:**
```sql
-- Template
shared_document_flag = true
sharing_scope = 'CUSTOM_RULES'
template_config = '{
  "eligibility_criteria": {
    "operator": "AND",
    "rules": [
      {"field": "zipcode", "operator": "IN", "value": ["94102", "94103", "94104"]}
    ]
  }
}'
data_extraction_config = '{
  "fieldsToExtract": ["zipcode"],
  "fieldSources": {
    "zipcode": {
      "sourceApi": "customerApi",
      "extractionPath": "$.customer.address.zipCode",
      "requiredInputs": ["customerId"]
    }
  },
  "dataSources": {
    "customerApi": {
      "endpoint": {
        "url": "http://api/customers/${customerId}",
        "method": "GET"
      }
    }
  }
}'
```

**Flow:**
```
Request → Find templates with data_extraction_config
        → Call external API to fetch customer data
        → Extract zipcode via JSONPath
        → Merge into evaluation context
        → Evaluate eligibility_criteria
        → If pass, return documents
```

---

### UC-6: Reference Key Matching

**Actor:** Customer requesting documents with specific reference criteria

**Description:** Match documents by extracted reference keys (disclosure codes, pricing tiers).

**Configuration:**
```sql
-- Template
data_extraction_config = '{
  "fieldsToExtract": ["disclosureCode"],
  "fieldSources": {...},
  "dataSources": {...},
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "disclosureCode",
    "referenceKeyType": "DISCLOSURE_CODE"
  }
}'

-- Document
reference_key = 'D164'
reference_key_type = 'DISCLOSURE_CODE'
```

**Flow:**
```
Request → Extract disclosureCode from external APIs
        → disclosureCode = "D164"
        → Query: WHERE reference_key = 'D164'
                 AND reference_key_type = 'DISCLOSURE_CODE'
        → Return matching document
```

---

### UC-7: Validity Period Enforcement

**Actor:** System (automatic)

**Description:** Automatically filter documents based on validity periods.

**Configuration:**
```sql
-- Document metadata
doc_metadata = '{
  "valid_from": "2024-01-01",
  "valid_until": "2024-12-31"
}'
```

**Flow:**
```
After retrieving documents → Check doc_metadata
        → If valid_from > today, exclude (future document)
        → If valid_until < today, exclude (expired)
        → Return only currently valid documents
```

**Supported Date Formats:**
- ISO: `"2024-01-01"`
- US: `"01/01/2024"`
- Epoch milliseconds: `1704067200000`

---

### UC-8: Multi-Account Retrieval

**Actor:** Customer with multiple accounts

**Description:** Retrieve documents across all customer accounts in a single request.

**Request:**
```json
{
  "customerId": "customer-uuid",
  "accountId": [
    "account-1-uuid",
    "account-2-uuid",
    "account-3-uuid"
  ]
}
```

**Flow:**
```
Request with multiple accountIds
        → For each accountId:
            → Process all templates
            → Collect account-specific + eligible shared docs
        → Aggregate results
        → Deduplicate shared documents
        → Paginate and return
```

---

### UC-9: Pagination

**Actor:** API Consumer

**Description:** Retrieve large document lists with pagination support.

**Request:**
```json
{
  "customerId": "...",
  "accountId": ["..."],
  "pageNumber": 2,
  "pageSize": 20
}
```

**Response:**
```json
{
  "documentList": [...],
  "pagination": {
    "pageNumber": 2,
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8
  },
  "_links": {
    "self": {...},
    "next": {...},
    "prev": {...}
  }
}
```

**Constraints:**
- Default page size: 20
- Max page size: 100
- Invalid page numbers default to 0

---

### UC-10: Multi-Requestor Support

**Actor:** Different user types

**Description:** Support different access patterns based on requestor type.

**Request Headers:**
```
X-version: 1
X-correlation-id: "trace-uuid"
X-requestor-id: "user-uuid"
X-requestor-type: CUSTOMER | AGENT | SYSTEM
```

**Use Cases:**
- `CUSTOMER`: End user accessing their own documents
- `AGENT`: Support staff accessing on behalf of customer
- `SYSTEM`: Backend service for integrations

---

## 3. Database Schema Review

### Tables

| Table | Purpose |
|-------|---------|
| `master_template_definition` | Template metadata, rules, extraction config |
| `storage_index` | Document instances linked to templates |
| `template_vendor_mapping` | Vendor-specific template mappings |

### Key Columns - master_template_definition

| Column | Type | Purpose | Status |
|--------|------|---------|--------|
| `master_template_id` | UUID | Primary key (with version) | OK |
| `template_version` | INTEGER | Version number | OK |
| `shared_document_flag` | BOOLEAN | Shared vs account-specific | OK |
| `sharing_scope` | VARCHAR | ALL, ACCOUNT_TYPE, CUSTOM_RULES | OK |
| `template_config` | JSONB | Contains eligibility_criteria | OK |
| `data_extraction_config` | JSONB | API extraction definitions | OK |
| `access_control` | JSONB | Role-based access (future use) | OK |
| `line_of_business` | VARCHAR | LOB filtering | OK |
| `template_type` | VARCHAR | Document type classification | OK |
| `active_flag` | BOOLEAN | Active/inactive status | OK |

### Key Columns - storage_index

| Column | Type | Purpose | Status |
|--------|------|---------|--------|
| `storage_index_id` | UUID | Primary key | OK |
| `master_template_id` | UUID | FK to template | OK |
| `account_key` | UUID | Account ownership | OK |
| `customer_key` | UUID | Customer ownership | OK |
| `shared_flag` | BOOLEAN | Shared document indicator | OK |
| `reference_key` | VARCHAR | Matching key (e.g., D164) | OK |
| `reference_key_type` | VARCHAR | Type of reference | OK |
| `doc_metadata` | JSONB | Validity dates, custom data | OK |
| `accessible_flag` | BOOLEAN | Soft delete/visibility | OK |

### Indexes

```sql
idx_storage_index_template    -- (master_template_id, template_version)
idx_storage_index_reference_key -- (reference_key, reference_key_type)
idx_storage_index_account     -- (account_key)
idx_template_type            -- (template_type)
```

### Identified Gaps

| Issue | Impact | Priority |
|-------|--------|----------|
| No document version column | Can't track doc revisions | Medium |
| No audit/access log table | No access tracking | Low |
| Metadata schema not enforced | Inconsistent field names | Low |

---

## 4. Data Extraction Logic Review

### Capabilities Matrix

| Feature | Status | Notes |
|---------|--------|-------|
| Multi-step API chains | Implemented | Step 1 → Step 2 |
| JSONPath extraction | Implemented | Complex filters supported |
| Placeholder resolution | Implemented | `${accountId}`, etc. |
| Retry logic | Implemented | Configurable per API |
| Caching config | Partial | Config exists, verify wiring |
| Parallel execution | Implemented | Independent APIs |
| Sequential execution | Implemented | Dependent chains |
| Error handling | Implemented | `continueOnError` flag |
| Default values | Implemented | Fallback on failure |

### Extraction Configuration Structure

```json
{
  "fieldsToExtract": ["field1", "field2"],
  "fieldSources": {
    "field1": {
      "sourceApi": "apiId",
      "extractionPath": "$.json.path",
      "requiredInputs": ["accountId"],
      "defaultValue": null
    }
  },
  "dataSources": {
    "apiId": {
      "endpoint": {
        "url": "http://api/${accountId}/data",
        "method": "GET",
        "timeout": 5000,
        "headers": {}
      },
      "cache": { "enabled": true, "ttlSeconds": 3600 },
      "retry": { "maxAttempts": 3, "delayMs": 100 }
    }
  },
  "executionStrategy": {
    "mode": "auto",
    "continueOnError": false,
    "timeout": 15000
  },
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "field1",
    "referenceKeyType": "TYPE"
  }
}
```

---

## 5. API Specification Review

### Endpoints

| Endpoint | Method | Status |
|----------|--------|--------|
| `/documents-enquiry` | POST | Implemented |
| `/documents/{id}` | GET | Spec only |
| `/documents/{id}` | DELETE | Spec only |
| `/documents/{id}/metadata` | GET | Spec only |
| `/documents` | POST | Spec only |

### Request Schema - DocumentListRequest

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `customerId` | UUID | Yes | Customer identifier |
| `accountId` | UUID[] | Yes | Array of accounts |
| `referenceKey` | String | No | Filter by ref key |
| `referenceKeyType` | String | No | Type of ref key |
| `documentTypeCategoryGroup` | Array | No | Category/type filter |
| `postedFromDate` | Epoch | No | Date range start |
| `postedToDate` | Epoch | No | Date range end |
| `pageNumber` | Number | No | Page index |
| `pageSize` | Number | No | Items per page |
| `sortOrder` | Array | No | Sort configuration |

### Response Schema - DocumentRetrievalResponse

```json
{
  "documentList": [
    {
      "documentId": "...",
      "displayName": "...",
      "description": "...",
      "lineOfBusiness": "...",
      "category": "...",
      "documentType": "...",
      "datePosted": 1234567890,
      "metadata": [...],
      "_links": {
        "download": {...},
        "delete": {...}
      }
    }
  ],
  "pagination": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalItems": 100,
    "totalPages": 5
  },
  "_links": {
    "self": {...},
    "next": {...},
    "prev": {...}
  }
}
```

---

## 6. Use Case Fulfillment Matrix

| Use Case | Database | Extraction | API | Overall |
|----------|----------|------------|-----|---------|
| UC-1: Account-specific | OK | N/A | OK | **READY** |
| UC-2: ALL shared | OK | N/A | OK | **READY** |
| UC-3: Account-type | OK | N/A | OK | **READY** |
| UC-4: Custom rules (static) | OK | N/A | OK | **READY** |
| UC-5: Custom rules (dynamic) | OK | OK | OK | **READY** |
| UC-6: Reference key match | OK | OK | OK | **READY** |
| UC-7: Validity enforcement | OK | N/A | OK | **READY** |
| UC-8: Multi-account | OK | N/A | OK | **READY** |
| UC-9: Pagination | OK | N/A | OK | **READY** |
| UC-10: Multi-requestor | OK | N/A | OK | **READY** |
| Document download | OK | N/A | Spec | **PENDING** |
| Document upload | OK | N/A | Spec | **PENDING** |
| Document delete | OK | N/A | Spec | **PENDING** |

---

## 7. Recommendations

### High Priority

1. **Implement download/upload/delete endpoints**
   - Currently only OpenAPI spec exists
   - Required for full document lifecycle

2. **Add circuit breaker for external APIs**
   - Prevent cascading failures
   - Use resilience4j or similar

3. **Verify caching implementation**
   - Ensure API responses are cached
   - Consider Redis for distributed cache

### Medium Priority

4. **Add `lineOfBusiness` filter to request**
   - Exists in template but not queryable
   - Would improve query efficiency

5. **Standardize validity date fields**
   - Current: `valid_from`, `validFrom`, `effective_date`
   - Should enforce single convention

6. **Add language filter**
   - `language_code` exists but not filterable
   - Multi-language support needed

### Low Priority

7. **Document versioning**
   - Track multiple versions of same document
   - Add `document_version` column

8. **Audit logging**
   - Track document access for compliance
   - New `document_access_log` table

9. **Observability**
   - Add metrics for API call timing
   - Track extraction success rates
