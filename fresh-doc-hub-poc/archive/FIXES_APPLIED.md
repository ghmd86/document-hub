# Database Fixes Applied - Summary

**Date**: December 4, 2025
**Status**: Fixes Created, Awaiting Database Update

---

## Critical Issues Discovered

### Issue 1: Database Schema Type Mismatch (BLOCKING)

**Problem**: `active_flag` and `shared_document_flag` columns are BIT(1) type instead of BOOLEAN
**Error**: `operator does not exist: boolean = bit`
**Impact**: Application cannot query templates at all - complete failure

**Root Cause**: When you changed the template name to "CardholderAgreement", the database schema wasn't updated to use BOOLEAN types.

### Issue 2: JSONPath Returns Array Instead of String

**Problem**: JSONPath `$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId` returns array `["PRC-12345"]` instead of string `"PRC-12345"`
**Impact**: Second API call fails with 404 because URL becomes `/prices/%5B%22PRC-12345%22%5D`

---

## Fixes Created

### File: `fix-all-issues.sql`

This comprehensive script includes:

#### Part 1: Schema Fixes
```sql
ALTER TABLE document_hub.master_template_definition
  ALTER COLUMN active_flag TYPE BOOLEAN USING (active_flag::int::boolean);

ALTER TABLE document_hub.master_template_definition
  ALTER COLUMN shared_document_flag TYPE BOOLEAN USING (shared_document_flag::int::boolean);

ALTER TABLE document_hub.storage_index
  ALTER COLUMN shared_document_flag TYPE BOOLEAN USING (shared_document_flag::int::boolean);
```

#### Part 2: JSONPath Fix
```sql
UPDATE document_hub.master_template_definition
SET data_extraction_config = replace(
    data_extraction_config::text,
    '").domainId"',
    '")[0].domainId"'
)::jsonb
WHERE template_type = 'CardholderAgreement';
```

#### Part 3: Verification Queries
Includes queries to verify both fixes were applied correctly.

---

## How to Apply Fixes

### Option 1: Using psql (Recommended)
```bash
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d document_hub -f "C:\Users\ghmd8\Documents\AI\fresh-doc-hub-poc\src\main\resources\fix-all-issues.sql"
```

### Option 2: Using pgAdmin
1. Open pgAdmin
2. Connect to document_hub database
3. Open Query Tool
4. Copy contents of `fix-all-issues.sql`
5. Execute the script

### Option 3: Manual Commands
Run each command separately in psql or pgAdmin:

```sql
-- Fix schema
ALTER TABLE document_hub.master_template_definition ALTER COLUMN active_flag TYPE BOOLEAN USING (active_flag::int::boolean);
ALTER TABLE document_hub.master_template_definition ALTER COLUMN shared_document_flag TYPE BOOLEAN USING (shared_document_flag::int::boolean);
ALTER TABLE document_hub.storage_index ALTER COLUMN shared_document_flag TYPE BOOLEAN USING (shared_document_flag::int::boolean);

-- Fix JSONPath
UPDATE document_hub.master_template_definition SET data_extraction_config = replace(data_extraction_config::text, '").domainId"', '")[0].domainId"')::jsonb WHERE template_type = 'CardholderAgreement';
```

---

## After Applying Fixes

### 1. Restart the Application
The application needs to be restarted to pick up the schema changes.

### 2. Test Scenario 1 (D164)
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-d164-fixed" \
  -H "X-requestor-id: aaaa0000-0000-0000-0000-000000000001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000001"]}'
```

**Expected**: Document with disclosure code D164

### 3. Test Scenario 2 (D166)
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-d166-fixed" \
  -H "X-requestor-id: aaaa0000-0000-0000-0000-000000000002" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000002"]}'
```

**Expected**: Document with disclosure code D166

---

## Verification

After running the fixes, verify they were applied:

### Check Schema Types
```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'document_hub'
  AND table_name = 'master_template_definition'
  AND column_name IN ('active_flag', 'shared_document_flag');
```

**Expected Output**:
```
 column_name           | data_type
-----------------------+-----------
 active_flag           | boolean
 shared_document_flag  | boolean
```

### Check JSONPath Fix
```sql
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
```

**Expected Output**:
```
 template_type      | pricingId_path
--------------------+----------------------------------------------------------------
 CardholderAgreement| $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")][0].domainId
```

Note the `[0]` before `.domainId` - this extracts the first element.

---

## Files Modified in This Session

1. `src/main/resources/test-data-disclosure-example-postgres.sql:61` - JSONPath fixed
2. `src/main/resources/fix-all-issues.sql` - NEW - Comprehensive fix script
3. `src/main/resources/fix-jsonpath.sql` - Individual JSONPath fix
4. `src/main/resources/update-jsonpath-quick.sql` - Alternative JSONPath fix
5. `FIXES_APPLIED.md` - THIS FILE - Documentation

---

## Timeline

1. **Issue Discovered**: Template query failing with "boolean = bit" error
2. **Root Cause Identified**: Database schema uses BIT instead of BOOLEAN
3. **JSONPath Issue Found**: Returns array instead of string
4. **Fixes Created**: Comprehensive `fix-all-issues.sql` script
5. **Next Step**: Apply fixes and test

---

## Success Criteria

Once fixes are applied and tested, you should see:

1. No more "boolean = bit" errors in logs
2. Templates query successfully
3. Data extraction completes for both API calls
4. Disclosure code is extracted correctly (as string, not array)
5. Documents are matched by `reference_key = disclosureCode`
6. Correct documents returned (D164 for Account 1, D166 for Account 2)

---

**Status**: Ready to apply - run `fix-all-issues.sql` then restart application
