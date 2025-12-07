# Document Hub POC - Current Status & Next Steps

**Last Updated**: December 4, 2025, 7:36 PM PST
**Status**: In Progress - API Path Fix Applied, Ready for Testing

---

## 🎯 Current Objective

Implementing and testing the **disclosure code document matching flow** where:
1. Account arrangements API returns a `pricingId`
2. Pricing API uses that `pricingId` to return a `disclosureCode`
3. System queries documents where `reference_key = disclosureCode`
4. User receives the matched disclosure document (e.g., D164 or D166)

---

## ✅ What Has Been Completed

### 1. Repository Layer - Storage Query by Reference Key
**File**: `StorageIndexRepository.java` (lines 71-81)
**Change**: Added new query method to find documents by reference key and template

```java
@Query("SELECT * FROM document_hub.storage_index " +
       "WHERE reference_key = :referenceKey " +
       "AND reference_key_type = :referenceKeyType " +
       "AND template_type = :templateType " +
       "AND template_version = :templateVersion")
Flux<StorageIndexEntity> findByReferenceKeyAndTemplate(
    @Param("referenceKey") String referenceKey,
    @Param("referenceKeyType") String referenceKeyType,
    @Param("templateType") String templateType,
    @Param("templateVersion") Integer templateVersion
);
```

### 2. Service Layer - Document Matching Integration
**File**: `DocumentEnquiryService.java`
**Changes**:
- **Line 201**: Modified to pass extracted data to queryDocuments for CUSTOM_RULES templates
- **Line 219**: Modified to pass null for standard (non-CUSTOM_RULES) templates
- **Lines 287-349**: Enhanced `queryDocuments()` method with document matching logic

**Key Logic** (lines 293-332):
```java
if (template.getDataExtractionConfig() != null && extractedData != null) {
    // Parse documentMatching configuration
    JsonNode configNode = objectMapper.readTree(template.getDataExtractionConfig().asString());

    if (configNode.has("documentMatching")) {
        JsonNode matchingNode = configNode.get("documentMatching");
        String matchBy = matchingNode.get("matchBy").asText();

        if ("reference_key".equals(matchBy)) {
            String referenceKeyField = matchingNode.get("referenceKeyField").asText();
            String referenceKeyType = matchingNode.get("referenceKeyType").asText();

            // Get the reference key value from extracted data
            Object referenceKeyValue = extractedData.get(referenceKeyField);

            if (referenceKeyValue != null) {
                // Query by reference key
                return storageRepository.findByReferenceKeyAndTemplate(
                    referenceKeyValue.toString(),
                    referenceKeyType,
                    template.getTemplateType(),
                    template.getTemplateVersion()
                ).collectList();
            }
        }
    }
}
// Fallback to standard queries if no match config or extraction failed
```

### 3. Mock API Controller - Account ID Mappings
**File**: `MockApiController.java` (lines 384-413)
**Change**: Added two new account ID mappings for mock service UUIDs

```java
} else if ("aaaa0000-0000-0000-0000-000000000001".equals(accountId)) {
    // Mock service Account 1 - VIP Credit Card (maps to D164)
    pricingArrangement.put("domainId", "PRC-12345");
    // ... returns PRC-12345 which maps to D164

} else if ("aaaa0000-0000-0000-0000-000000000002".equals(accountId)) {
    // Mock service Account 2 - Standard Credit Card (maps to D166)
    pricingArrangement.put("domainId", "PRC-67890");
    // ... returns PRC-67890 which maps to D166
}
```

**Mapping Chain**:
- Account `aaaa0000-0000-0000-0000-000000000001` → `PRC-12345` → `D164`
- Account `aaaa0000-0000-0000-0000-000000000002` → `PRC-67890` → `D166`

### 4. Database Fix - API Path Correction
**File**: `fix-api-paths.sql` (created)
**Purpose**: Updates database URLs to include `/api/v1` context path
**Status**: ✅ Script has been run

```sql
UPDATE document_hub.master_template_definition
SET data_extraction_config = REPLACE(
    data_extraction_config::text,
    'http://localhost:8080/mock-api/',
    'http://localhost:8080/api/v1/mock-api/'
)::jsonb
WHERE template_type = 'CardholderAgreement';
```

### 5. Application Build & Deployment
- ✅ Compiled successfully with `mvn clean compile`
- ✅ Application running on `http://localhost:8080/api/v1`
- ✅ Tomcat started on port 8080

---

## 🔍 Issues Found & Resolved

### Issue 1: Account ID Mismatch
**Problem**: Test data used different UUIDs than mock service
**Solution**: Used correct account IDs from `AccountMetadataService.java`:
- Account 1: `aaaa0000-0000-0000-0000-000000000001` (VIP, Credit Card, US_WEST)
- Account 2: `aaaa0000-0000-0000-0000-000000000002` (STANDARD, Credit Card, US_WEST)

### Issue 2: Missing Context Path in API URLs
**Problem**: Database had `http://localhost:8080/mock-api/...` but app runs at `/api/v1/mock-api/...`
**Root Cause**: Test data file `test-data-disclosure-example-postgres.sql` (line 79, 100) had incorrect URLs
**Solution**: Created and ran `fix-api-paths.sql` to update database

---

## 📋 Next Steps (When You Resume)

### Step 1: Verify Database Update ✅
Check that the API paths were updated correctly:

```bash
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d document_hub -c "SELECT template_type, data_extraction_config->'dataSources'->'accountArrangementsApi'->'endpoint'->>'url' as url FROM document_hub.master_template_definition WHERE template_type = 'CardholderAgreement';"
```

**Expected Output**: URL should be `http://localhost:8080/api/v1/mock-api/creditcard/accounts/${accountId}/arrangements`

### Step 2: Test Scenario 1 - D164 Disclosure Code
Test with Account 1 (VIP, should get D164 document):

```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-d164-final" \
  -H "X-requestor-id: aaaa0000-0000-0000-0000-000000000001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000001"]}'
```

**Expected Flow**:
1. API calls `/api/v1/mock-api/creditcard/accounts/aaaa0000-0000-0000-0000-000000000001/arrangements`
2. Returns `pricingId: PRC-12345`
3. API calls `/api/v1/mock-api/pricing-service/prices/PRC-12345`
4. Returns `disclosureCode: D164`
5. Queries database: `WHERE reference_key = 'D164' AND reference_key_type = 'DISCLOSURE_CODE'`
6. Returns document: `Credit_Card_Terms_D164_v1.pdf`

**Expected Response**: Should contain a document with `displayName` containing "D164"

### Step 3: Test Scenario 2 - D166 Disclosure Code
Test with Account 2 (STANDARD, should get D166 document):

```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-d166-final" \
  -H "X-requestor-id: aaaa0000-0000-0000-0000-000000000002" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000002"]}'
```

**Expected Response**: Should contain a document with `displayName` containing "D166"

### Step 4: Check Application Logs
Monitor the logs for successful data extraction:

```bash
# Look for these key log entries:
# - "Template CardholderAgreement uses CUSTOM_RULES - extracting additional data"
# - "API call succeeded" (for both accountArrangementsApi and pricingApi)
# - "Data extraction completed for template CardholderAgreement - X fields extracted"
# - "Using document matching: reference_key=disclosureCode, type=DISCLOSURE_CODE, value=D164"
```

### Step 5: If Tests Pass
Update the todo list and mark both scenarios as completed:

```
✅ Test end-to-end disclosure code flow - Scenario 1 (D164)
✅ Verify Scenario 2 (D166)
```

---

## 🐛 If Tests Fail - Debugging Guide

### Check 1: Verify Application is Running
```bash
curl http://localhost:8080/api/v1/mock-api/creditcard/accounts/aaaa0000-0000-0000-0000-000000000001/arrangements
```
Expected: JSON response with `PRC-12345`

### Check 2: Verify Pricing API
```bash
curl http://localhost:8080/api/v1/mock-api/pricing-service/prices/PRC-12345
```
Expected: JSON response with `cardholderAgreementsTncCode: D164`

### Check 3: Verify Database Has Test Data
```sql
-- Check if template exists
SELECT template_type, template_name, sharing_scope
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';

-- Check if documents exist
SELECT reference_key, reference_key_type, file_name
FROM document_hub.storage_index
WHERE reference_key_type = 'DISCLOSURE_CODE';
```

### Check 4: Review Application Logs
Look for specific error messages:
- **404 Not Found**: API path issue (check context path)
- **Reference key field not found**: Data extraction didn't return disclosureCode
- **Could not read property doc_metadata**: Database schema issue (non-blocking)

---

## 📁 Key Files Modified

| File | Lines Changed | Purpose |
|------|--------------|---------|
| `StorageIndexRepository.java` | 71-81 | Added findByReferenceKeyAndTemplate query |
| `DocumentEnquiryService.java` | 201, 219, 287-349 | Integrated document matching logic |
| `MockApiController.java` | 384-413 | Added account ID mappings |
| `fix-api-paths.sql` | NEW | Database URL correction script |

---

## 🔗 Related Files (Reference Only)

- **Test Data**: `test-data-disclosure-example-postgres.sql` (defines CardholderAgreement template)
- **Account Metadata**: `AccountMetadataService.java` (mock account data)
- **Data Extraction**: `ConfigurableDataExtractionService.java` (orchestrates API calls)
- **Entity**: `StorageIndexEntity.java` (document storage model)

---

## 💡 Key Concepts

### Document Matching Configuration (JSONB in database)
```json
{
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "disclosureCode",
    "referenceKeyType": "DISCLOSURE_CODE"
  }
}
```

This tells the system:
1. Use extracted field called `disclosureCode`
2. Query documents where `reference_key = <extracted value>`
3. Filter by `reference_key_type = 'DISCLOSURE_CODE'`

### Data Extraction Flow
```
Request → Template (CUSTOM_RULES + data_extraction_config)
       → ConfigurableDataExtractionService
       → API Chain: arrangements → pricingId → pricing → disclosureCode
       → DocumentEnquiryService.queryDocuments()
       → Parse documentMatching config
       → Call findByReferenceKeyAndTemplate(D164, DISCLOSURE_CODE, ...)
       → Return matched documents
```

---

## 📊 Test Account Reference

| Account ID | Customer Segment | Account Type | Region | Maps to Pricing | Maps to Disclosure |
|------------|-----------------|--------------|--------|-----------------|-------------------|
| aaaa0000-0000-0000-0000-000000000001 | VIP | credit_card | US_WEST | PRC-12345 | D164 |
| aaaa0000-0000-0000-0000-000000000002 | STANDARD | credit_card | US_WEST | PRC-67890 | D166 |
| aaaa0000-0000-0000-0000-000000000003 | ENTERPRISE | digital_bank | US_EAST | N/A | N/A |
| aaaa0000-0000-0000-0000-000000000004 | STANDARD | savings | US_EAST | N/A | N/A |

---

## 🚀 Quick Start Commands (When You Resume)

```bash
# 1. Check if app is still running
curl http://localhost:8080/api/v1/mock-api/creditcard/accounts/aaaa0000-0000-0000-0000-000000000001/arrangements

# 2. If not running, restart:
cd C:\Users\ghmd8\Documents\AI\fresh-doc-hub-poc
mvn spring-boot:run

# 3. Run Scenario 1 test (copy full curl command from Step 2 above)

# 4. Run Scenario 2 test (copy full curl command from Step 3 above)

# 5. If both pass, mark tasks complete!
```

---

## ❓ Questions to Investigate (If Time Permits)

1. **doc_metadata error**: Non-blocking but appears in logs - investigate R2DBC mapping for JSONB columns
2. **Performance**: Consider caching for data extraction results (already in config but not implemented)
3. **Error handling**: What happens if pricing API returns unexpected format?

---

**Status**: Ready for final end-to-end testing
**Next Action**: Run Step 2 (Scenario 1 test) when you resume

Good luck! 🎉
