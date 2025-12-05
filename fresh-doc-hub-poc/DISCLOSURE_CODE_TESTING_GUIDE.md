# Disclosure Code Extraction Testing Guide

## Overview

This scenario demonstrates **real-world disclosure code extraction** based on the document-hub-service Example 1. It shows how to:

1. Extract `pricingId` from account arrangements
2. Use `pricingId` to get `disclosureCode` from pricing service
3. Match documents using `disclosureCode` as `reference_key`

## Architecture

```
Client Request (accountId, customerId)
         ↓
Load Template: CardholderAgreement
         ↓
ConfigurableDataExtractionService
         ↓
LEVEL 1: GET /creditcard/accounts/{accountId}/arrangements
         ├→ Response: content[] array with multiple domains
         └→ Extract: pricingId from PRICING domain where status=ACTIVE
         ↓
LEVEL 2: GET /pricing-service/prices/{pricingId}
         ├→ Response: Pricing details
         └→ Extract: disclosureCode (cardholderAgreementsTncCode)
         ↓
Document Matching
         ├→ Match by: reference_key
         ├→ Where: reference_key = disclosureCode
         └→ And: reference_key_type = "DISCLOSURE_CODE"
         ↓
Return matched documents
```

## Data Setup

### Load Test Data

```bash
# Load the disclosure code test data
psql -U postgres -d document_hub -f src/main/resources/test-data-disclosure-example-postgres.sql
```

### Verify Data Loaded

```sql
-- Check template
SELECT template_type, template_name, is_shared_document, sharing_scope
FROM master_template_definition
WHERE template_type = 'CardholderAgreement';

-- Check documents with disclosure codes
SELECT reference_key, reference_key_type, file_name, doc_metadata->>'cardTier'
FROM storage_index
WHERE template_type = 'CardholderAgreement'
ORDER BY reference_key;
```

Expected output:
```
 reference_key | reference_key_type |            file_name              | card_tier
---------------+--------------------+-----------------------------------+-----------
 D164          | DISCLOSURE_CODE    | Credit_Card_Terms_D164_v1.pdf     | NULL
 D165          | DISCLOSURE_CODE    | Credit_Card_Terms_D165_v1.pdf     | NULL
 D166          | DISCLOSURE_CODE    | Premium_Credit_Card_Terms_D166... | PREMIUM
```

## Test Scenarios

### Test Scenario 1: Standard Credit Card → Disclosure Code D164

**Account ID**: `550e8400-e29b-41d4-a716-446655440000`

**Expected Flow**:

1. **Call Arrangements API**:
   ```bash
   curl http://localhost:8080/mock-api/creditcard/accounts/550e8400-e29b-41d4-a716-446655440000/arrangements
   ```

   **Response**:
   ```json
   {
     "content": [
       {
         "domain": "PRICING",
         "domainId": "PRC-12345",
         "status": "ACTIVE",
         "effectiveDate": "2024-01-01"
       },
       {
         "domain": "REWARDS",
         "domainId": "RWD-001",
         "status": "ACTIVE"
       }
     ],
     "accountId": "550e8400-e29b-41d4-a716-446655440000",
     "totalItems": 2
   }
   ```

   **Extracted**: `pricingId = "PRC-12345"`

2. **Call Pricing API**:
   ```bash
   curl http://localhost:8080/mock-api/pricing-service/prices/PRC-12345
   ```

   **Response**:
   ```json
   {
     "pricingId": "PRC-12345",
     "cardholderAgreementsTncCode": "D164",
     "productType": "STANDARD_CREDIT_CARD",
     "annualFee": 0,
     "interestRate": 18.99,
     "effectiveDate": "2024-01-01",
     "currency": "USD"
   }
   ```

   **Extracted**: `disclosureCode = "D164"`

3. **Document Matching**:
   - Match documents where `reference_key = "D164"` AND `reference_key_type = "DISCLOSURE_CODE"`
   - **Expected Document**: `Credit_Card_Terms_D164_v1.pdf`

**Full Test Request**:
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

---

### Test Scenario 2: Premium Credit Card → Disclosure Code D166

**Account ID**: `770e8400-e29b-41d4-a716-446655440002`

**Expected Flow**:

1. **Call Arrangements API**:
   ```bash
   curl http://localhost:8080/mock-api/creditcard/accounts/770e8400-e29b-41d4-a716-446655440002/arrangements
   ```

   **Response**:
   ```json
   {
     "content": [
       {
         "domain": "PRICING",
         "domainId": "PRC-67890",
         "status": "ACTIVE"
       },
       {
         "domain": "REWARDS",
         "domainId": "RWD-PREMIUM-001",
         "status": "ACTIVE"
       }
     ]
   }
   ```

   **Extracted**: `pricingId = "PRC-67890"`

2. **Call Pricing API**:
   ```bash
   curl http://localhost:8080/mock-api/pricing-service/prices/PRC-67890
   ```

   **Response**:
   ```json
   {
     "pricingId": "PRC-67890",
     "cardholderAgreementsTncCode": "D166",
     "productType": "PREMIUM_CREDIT_CARD",
     "annualFee": 95,
     "interestRate": 15.99,
     "rewardsMultiplier": 2.0,
     "effectiveDate": "2024-01-01"
   }
   ```

   **Extracted**: `disclosureCode = "D166"`

3. **Document Matching**:
   - Match documents where `reference_key = "D166"`
   - **Expected Document**: `Premium_Credit_Card_Terms_D166_v1.pdf`

**Full Test Request**:
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["770e8400-e29b-41d4-a716-446655440002"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

---

## JSONPath Extraction Details

### Extract pricingId from Arrangements

**JSONPath**: `$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId`

**Explanation**:
- `$.content[]` - Get content array
- `?(@.domain == "PRICING")` - Filter where domain is PRICING
- `&& @.status == "ACTIVE"` - AND status is ACTIVE
- `.domainId` - Extract the domainId field

**Example Array**:
```json
{
  "content": [
    {"domain": "PRICING", "domainId": "PRC-12345", "status": "ACTIVE"},
    {"domain": "REWARDS", "domainId": "RWD-001", "status": "ACTIVE"},
    {"domain": "PRICING", "domainId": "PRC-OLD", "status": "INACTIVE"}
  ]
}
```

**Result**: `"PRC-12345"` (only active PRICING)

### Extract disclosureCode from Pricing

**JSONPath**: `$.cardholderAgreementsTncCode`

**Example Response**:
```json
{
  "pricingId": "PRC-12345",
  "cardholderAgreementsTncCode": "D164",
  "productType": "STANDARD_CREDIT_CARD"
}
```

**Result**: `"D164"`

---

## Document Matching Strategy

The `documentMatching` configuration in the template tells the system how to use extracted data:

```json
{
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "disclosureCode",
    "referenceKeyType": "DISCLOSURE_CODE"
  }
}
```

This means:
1. Use the extracted `disclosureCode` field
2. Match documents where `storage_index.reference_key = disclosureCode`
3. AND `storage_index.reference_key_type = "DISCLOSURE_CODE"`

### SQL Query Equivalent

```sql
SELECT *
FROM storage_index
WHERE reference_key = '${extracted_disclosureCode}'
  AND reference_key_type = 'DISCLOSURE_CODE'
  AND template_type = 'CardholderAgreement'
  AND is_accessible = 1
  AND archive_indicator = false;
```

---

## Testing Checklist

### ✅ Scenario 1 (Standard Card - D164)
- [ ] Arrangements API called with accountId
- [ ] pricingId extracted: `PRC-12345`
- [ ] Pricing API called with pricingId
- [ ] disclosureCode extracted: `D164`
- [ ] Document matched: `Credit_Card_Terms_D164_v1.pdf`
- [ ] Total execution time < 3 seconds

### ✅ Scenario 2 (Premium Card - D166)
- [ ] Arrangements API called with accountId
- [ ] pricingId extracted: `PRC-67890`
- [ ] Pricing API called with pricingId
- [ ] disclosureCode extracted: `D166`
- [ ] Document matched: `Premium_Credit_Card_Terms_D166_v1.pdf`
- [ ] Total execution time < 3 seconds

### ✅ Data Extraction Verification
- [ ] Sequential execution (Level 1 → Level 2)
- [ ] Placeholder resolution works (${pricingId})
- [ ] JSONPath filters work (domain == "PRICING")
- [ ] Reference key matching works
- [ ] Shared document filtering works

---

## Logs to Check

Look for these log entries:

```
INFO  ConfigurableDataExtractionService - Starting data extraction for 2 required fields
DEBUG ConfigurableDataExtractionService - Planning extraction for fields: [pricingId, disclosureCode]
DEBUG ConfigurableDataExtractionService - Added API 'accountArrangementsApi' to plan (provides: [pricingId])
DEBUG ConfigurableDataExtractionService - Added API 'pricingApi' to plan (provides: [disclosureCode])
INFO  ConfigurableDataExtractionService - Execution plan: 2 API calls
DEBUG ConfigurableDataExtractionService - Calling API: GET http://localhost:8080/mock-api/creditcard/accounts/550e8400.../arrangements
DEBUG ConfigurableDataExtractionService - Extracted pricingId: PRC-12345
DEBUG ConfigurableDataExtractionService - Calling API: GET http://localhost:8080/mock-api/pricing-service/prices/PRC-12345
DEBUG ConfigurableDataExtractionService - Extracted disclosureCode: D164
INFO  ConfigurableDataExtractionService - Data extraction completed. Extracted 2 fields
```

---

## Key Differences from Other Scenarios

### 1. JSONPath with Filtering
Unlike simple extraction like `$.account.productCode`, this uses filtering:
```
$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId
```

### 2. Document Matching by Reference Key
Most scenarios match by template only. This scenario matches by:
- Template type: `CardholderAgreement`
- **Plus** reference key: `disclosureCode` value
- **Plus** reference key type: `DISCLOSURE_CODE`

### 3. Shared Documents with Dynamic Matching
Documents are shared (`is_shared = true`) but matched dynamically based on extracted data, not account-specific.

### 4. Real-World API Pattern
Mimics actual microservices:
- Arrangements API: Returns multiple domains
- Pricing API: Returns detailed pricing info
- Document Hub: Matches based on pricing metadata

---

## Troubleshooting

### Issue: "pricingId not extracted"
**Check**:
- JSONPath filter syntax: `?(@.domain == "PRICING")`
- Response has `status = "ACTIVE"`
- Array notation is correct

### Issue: "No documents matched"
**Check**:
- Documents loaded with correct `reference_key`
- `reference_key_type = "DISCLOSURE_CODE"`
- Extracted `disclosureCode` matches document `reference_key`

### Issue: "Wrong disclosure code extracted"
**Check**:
- Correct account ID used in test
- Arrangements API returns expected pricingId
- Pricing API maps pricingId correctly

---

## Summary

This scenario demonstrates:

✅ **Complex JSONPath extraction** with filtering
✅ **2-step API chaining** (arrangements → pricing)
✅ **Dynamic document matching** by reference key
✅ **Real-world pattern** used in production systems
✅ **Shared documents** matched dynamically per account

This pattern is ideal for:
- Terms & Conditions documents
- Pricing disclosures
- Regulatory documents
- Product-specific agreements
- Any document matched by extracted metadata
