# Disclosure Code Extraction - Implementation Summary

## What Was Added

Complete implementation of **disclosure code extraction** based on the document-hub-service Example 1, demonstrating real-world document matching using dynamically extracted metadata.

## Files Created/Modified

### 1. **test-data-disclosure-example-postgres.sql** (NEW)
PostgreSQL test data with:
- 1 new template: `CREDIT_CARD_TERMS_CONDITIONS`
- 3 documents with different disclosure codes (D164, D165, D166)
- Complete `data_extraction_config` JSON with 2-step extraction

### 2. **MockApiController.java** (MODIFIED)
Added 2 new mock API endpoints:
- `GET /mock-api/creditcard/accounts/{accountId}/arrangements`
- `GET /mock-api/pricing-service/prices/{pricingId}`

### 3. **DISCLOSURE_CODE_TESTING_GUIDE.md** (NEW)
Comprehensive testing guide with:
- 2 complete test scenarios
- JSONPath extraction examples
- Document matching explanation
- Troubleshooting tips

### 4. **DISCLOSURE_CODE_SUMMARY.md** (NEW)
This summary document

## How It Works

### Step-by-Step Flow

```
1. Client Request
   ├─ accountId: 550e8400-e29b-41d4-a716-446655440000
   └─ customerId: 123e4567-e89b-12d3-a456-426614174000

2. Load Template: CREDIT_CARD_TERMS_CONDITIONS
   └─ Read data_extraction_config from database

3. LEVEL 1: Extract pricingId
   ├─ Call: GET /creditcard/accounts/{accountId}/arrangements
   ├─ Response: Array with multiple arrangements (PRICING, REWARDS, etc.)
   ├─ JSONPath: $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId
   └─ Extract: pricingId = "PRC-12345"

4. LEVEL 2: Extract disclosureCode
   ├─ Call: GET /pricing-service/prices/PRC-12345
   ├─ Response: Pricing details with cardholderAgreementsTncCode
   ├─ JSONPath: $.cardholderAgreementsTncCode
   └─ Extract: disclosureCode = "D164"

5. Document Matching
   ├─ Match By: reference_key
   ├─ Where: reference_key = "D164"
   ├─ And: reference_key_type = "DISCLOSURE_CODE"
   └─ Found: Credit_Card_Terms_D164_v1.pdf

6. Return Document
```

## Key Features Demonstrated

### 1. Complex JSONPath with Filtering
```json
{
  "extractionPath": "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId"
}
```

Filters array to find:
- Items where `domain == "PRICING"`
- AND `status == "ACTIVE"`
- Extracts `domainId` field

### 2. Document Matching by Reference Key
```json
{
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "disclosureCode",
    "referenceKeyType": "DISCLOSURE_CODE"
  }
}
```

Uses extracted data to match documents dynamically.

### 3. 2-Step API Chaining
```
Arrangements API → pricingId → Pricing API → disclosureCode
```

Second API depends on result from first API.

### 4. Shared Documents with Dynamic Matching
Documents are shared (`is_shared = true`) but matched per account based on extracted disclosure code.

## Test Accounts

| Account ID | Pricing ID | Disclosure Code | Document Matched |
|------------|------------|-----------------|------------------|
| `550e8400-e29b-41d4-a716-446655440000` | PRC-12345 | D164 | Credit_Card_Terms_D164_v1.pdf |
| `770e8400-e29b-41d4-a716-446655440002` | PRC-67890 | D166 | Premium_Credit_Card_Terms_D166_v1.pdf |

## Test Commands

### Test Scenario 1: Standard Card
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Expected**: Returns `Credit_Card_Terms_D164_v1.pdf`

### Test Scenario 2: Premium Card
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["770e8400-e29b-41d4-a716-446655440002"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Expected**: Returns `Premium_Credit_Card_Terms_D166_v1.pdf`

### Test Individual APIs

**Arrangements API**:
```bash
curl http://localhost:8080/mock-api/creditcard/accounts/550e8400-e29b-41d4-a716-446655440000/arrangements
```

**Pricing API**:
```bash
curl http://localhost:8080/mock-api/pricing-service/prices/PRC-12345
```

## Real-World Use Cases

This pattern is used for:

1. **Terms & Conditions Documents**
   - Different T&C based on pricing tier
   - Matched by disclosure code
   - Updated when pricing changes

2. **Regulatory Disclosures**
   - Product-specific disclosures
   - Region-specific requirements
   - Version control via disclosure codes

3. **Fee Schedules**
   - Pricing tier-specific fees
   - Promotional pricing documents
   - Matched dynamically per account

4. **Product Documentation**
   - Feature-specific guides
   - Tier-based manuals
   - Dynamic based on account configuration

## Benefits

✅ **Dynamic Document Selection** - Documents matched based on real-time account data
✅ **No Code Changes** - Add new disclosure codes via database configuration
✅ **Version Control** - Track document versions via disclosure codes
✅ **Reusable Pattern** - Apply to any document type needing dynamic matching
✅ **Production-Ready** - Based on actual document-hub-service implementation

## Integration Notes

This example integrates with the existing `ConfigurableDataExtractionService`:

1. **No code changes needed** - Service already supports:
   - Sequential API chaining
   - Complex JSONPath with filtering
   - Placeholder resolution

2. **Document matching requires enhancement**:
   - Add support for `documentMatching` configuration
   - Implement reference_key matching in document query
   - Filter by reference_key_type

3. **Query enhancement needed**:
```java
// In DocumentEnquiryService or similar
if (config.getDocumentMatching() != null) {
    String matchBy = config.getDocumentMatching().get("matchBy");

    if ("reference_key".equals(matchBy)) {
        String referenceKeyField = config.getDocumentMatching().get("referenceKeyField");
        String referenceKeyType = config.getDocumentMatching().get("referenceKeyType");
        String referenceKeyValue = extractedData.get(referenceKeyField);

        return storageRepository.findByReferenceKeyAndType(
            referenceKeyValue,
            referenceKeyType,
            templateType
        );
    }
}
```

## Next Steps

1. ✅ **Test Data Created** - Load with SQL script
2. ✅ **Mock APIs Created** - Arrangements and Pricing endpoints
3. ✅ **Documentation Created** - Complete testing guide
4. ⏳ **Integration** - Connect to DocumentEnquiryService
5. ⏳ **Repository Method** - Add findByReferenceKeyAndType
6. ⏳ **End-to-End Test** - Verify full flow

## Summary

This implementation demonstrates a **production-ready pattern** for:
- Multi-step API extraction
- Complex JSONPath filtering
- Dynamic document matching
- Shared document selection based on extracted metadata

All based on the real document-hub-service Example 1! 🎉
