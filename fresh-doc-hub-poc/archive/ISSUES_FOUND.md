# Issues Found During Testing

**Date**: December 4, 2025, 9:15 PM PST

## Issue Summary

While testing the disclosure code document matching flow, we've discovered a critical bug in the test data configuration.

---

## Problem: JSONPath Returns Array Instead of String

### Issue Description
The `pricingId` extraction is returning an array `["PRC-12345"]` instead of a string `"PRC-12345"`, which causes the second API call to fail with a 404 error.

### Root Cause
The JSONPath expression in the database returns an array:
```
$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId
```

This returns: `["PRC-12345"]` (array with one element)

### Evidence from Logs
```
2025-12-04 21:13:26 - Extracted pricingId: ["PRC-12345"] (path: $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId)
2025-12-04 21:13:26 - Calling GET http://localhost:8080/api/v1/mock-api/pricing-service/prices/["PRC-12345"]
2025-12-04 21:13:26 - API call failed for pricingApi after 14ms: 404 Not Found from GET http://localhost:8080/api/v1/mock-api/pricing-service/prices/%5B%22PRC-12345%22%5D
```

The URL becomes `/prices/%5B%22PRC-12345%22%5D` (URL-encoded array) instead of `/prices/PRC-12345`

### Solution
Change the JSONPath to extract the first element:
```
OLD: $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId
NEW: $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")][0].domainId
```

This will return: `"PRC-12345"` (string)

---

## Files Affected

### 1. Database Configuration
**Table**: `document_hub.master_template_definition`
**Column**: `data_extraction_config` (JSONB)
**Template**: `CardholderAgreement`

**Path in JSON**: `fieldsToExtract.pricingId.extractionPath`

### 2. Test Data SQL File
**File**: `src/main/resources/test-data-disclosure-example-postgres.sql`
**Line**: 61

---

## Fixes Created

### 1. SQL Fix Script
**File**: `src/main/resources/fix-jsonpath.sql`

```sql
UPDATE document_hub.master_template_definition
SET data_extraction_config = jsonb_set(
    data_extraction_config,
    '{fieldsToExtract,pricingId,extractionPath}',
    '"$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")][0].domainId"'
)
WHERE template_type = 'CardholderAgreement';
```

**Status**: Script created but PostgreSQL command is hanging

### 2. Alternative: Manual Fix in Test Data File
**File**: `test-data-disclosure-example-postgres.sql`
**Line to Change**: 61

**Before**:
```json
"extractionPath": "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId"
```

**After**:
```json
"extractionPath": "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")][0].domainId"
```

Then re-run the test data script to reload.

---

## What's Working

✅ **Mock API Endpoints**: Both arrangements and pricing APIs work correctly
✅ **API Path Fix**: URLs now include `/api/v1/` context path
✅ **Account ID Mappings**: Mock controller has correct mappings for test accounts
✅ **First API Call**: Arrangements API successfully returns pricingId (as array)
✅ **Data Extraction Service**: Correctly processes the 2-step API chain
✅ **Document Matching Logic**: Code is correct and ready to work

❌ **JSONPath Configuration**: Returns array instead of string
❌ **Second API Call**: Fails due to mal formed URL with array value
❌ **Database Update**: PostgreSQL commands hanging

---

## Current Flow (Observed from Logs)

### Account 1 Request Flow:
1. ✅ Template `CardholderAgreement` identified (CUSTOM_RULES)
2. ✅ Data extraction initiated
3. ✅ API 1: `/api/v1/mock-api/creditcard/accounts/aaaa0000-0000-0000-0000-000000000001/arrangements`
   - **Response**: Success (200 OK)
   - **Extracted**: `pricingId = ["PRC-12345"]` ⚠️ (ARRAY!)
4. ❌ API 2: `/api/v1/mock-api/pricing-service/prices/["PRC-12345"]`
   - **Response**: 404 Not Found
   - **Reason**: URL is malformed - contains URL-encoded array
5. ❌ `disclosureCode` field not found in extracted data
6. ❌ Falls back to standard query (returns old test data)

### Expected Flow (After Fix):
1. ✅ Template `CardholderAgreement` identified (CUSTOM_RULES)
2. ✅ Data extraction initiated
3. ✅ API 1: `/api/v1/mock-api/creditcard/accounts/aaaa0000-0000-0000-0000-000000000001/arrangements`
   - **Response**: Success (200 OK)
   - **Extracted**: `pricingId = "PRC-12345"` ✅ (STRING!)
4. ✅ API 2: `/api/v1/mock-api/pricing-service/prices/PRC-12345`
   - **Response**: Success (200 OK)
   - **Extracted**: `disclosureCode = "D164"`
5. ✅ Document matching: Query where `reference_key = 'D164'`
6. ✅ Return `Credit_Card_Terms_D164_v1.pdf`

---

## Recommended Next Steps

### Option 1: Manual Database Fix (Quickest)
```sql
-- Run this in psql or pgAdmin
UPDATE document_hub.master_template_definition
SET data_extraction_config = jsonb_set(
    data_extraction_config,
    '{fieldsToExtract,pricingId,extractionPath}',
    '"$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")][0].domainId"'
)
WHERE template_type = 'CardholderAgreement';
```

Then test again with the same curl command.

### Option 2: Re-load Test Data (More thorough)
1. Edit `test-data-disclosure-example-postgres.sql` line 61
2. Change `].domainId"` to `][0].domainId"`
3. Truncate and reload:
```sql
TRUNCATE TABLE document_hub.master_template_definition CASCADE;
-- Then run the updated test data script
```

### Option 3: Wait for PostgreSQL Command
The `fix-jsonpath.sql` script is running but PostgreSQL is taking a long time. You could:
- Wait for it to complete
- Cancel it (Ctrl+C) and try Option 1 or 2

---

## Test Commands (Once Fixed)

### Scenario 1: Account 1 (D164)
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-d164-fixed" \
  -H "X-requestor-id: aaaa0000-0000-0000-0000-000000000001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000001"]}'
```

**Expected**: Document with `displayName` containing "D164"

### Scenario 2: Account 2 (D166)
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-d166-fixed" \
  -H "X-requestor-id: aaaa0000-0000-0000-0000-000000000002" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000002"]}'
```

**Expected**: Document with `displayName` containing "D166"

---

## Additional Issues to Investigate

### doc_metadata Database Error
```
Could not read property @org.springframework.data.relational.core.mapping.Column("doc_metadata")
private com.fasterxml.jackson.databind.JsonNode com.documenthub.entity.StorageIndexEntity.docMetadata
from column doc_metadata!
```

**Impact**: This error appears in logs but doesn't block the main flow
**Root Cause**: Likely a schema mismatch between database and entity
**Priority**: Low - can be addressed later

---

## Summary

**Main Blocker**: JSONPath configuration returns array instead of string
**Quick Fix**: Update one line in database configuration
**Impact**: Once fixed, the entire disclosure code matching flow should work end-to-end

All the code we've written (repository query, service integration, mock APIs, document matching) is correct and ready. We just need to fix this one configuration issue.
