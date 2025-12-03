# Testing Guide - Multi-Step Data Extraction

## Setup

### 1. Load Test Data
```sql
-- Run the test data script
psql -U postgres -d document_hub -f src/main/resources/test-data.sql

-- Or for H2 in-memory database, it will auto-load from classpath
```

### 2. Start the Application
```bash
mvn spring-boot:run
```

The mock API endpoints will be available at `http://localhost:8080/mock-api/*`

## Test Scenarios

### Test Scenario 1: Simple 3-Step Chain

**Template Type**: `ACCOUNT_STATEMENT`

**Dependency Chain**:
```
accountId → productCode → productCategory → disclosureRequirements
```

**Test Request**:
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Expected Extraction Flow**:
1. **Call 1**: `GET /mock-api/accounts/550e8400-e29b-41d4-a716-446655440000/details`
   - Extract: `productCode = "CC-PREMIUM-001"`

2. **Call 2**: `GET /mock-api/products/CC-PREMIUM-001`
   - Extract: `productCategory = "CREDIT_CARD"`

3. **Call 3**: `GET /mock-api/regulatory/requirements?category=CREDIT_CARD`
   - Extract: `disclosureRequirements = ["TILA", "CARD_ACT", "SCHUMER_BOX"]`

**Expected Result**:
```json
{
  "productCode": "CC-PREMIUM-001",
  "productCategory": "CREDIT_CARD",
  "disclosureRequirements": ["TILA", "CARD_ACT", "SCHUMER_BOX"]
}
```

**Documents Returned**:
- Statement_January_2024.pdf
- Statement_February_2024.pdf

---

### Test Scenario 2: Parallel + Sequential (Diamond Pattern)

**Template Type**: `REGULATORY_DISCLOSURE`

**Dependency Graph**:
```
       accountId              customerId
           ↓                      ↓
      productCode          customerLocation
           ↓                      ↓
    productCategory        regulatoryRegion
           ↘                    ↙
           finalDisclosures
```

**Test Request**:
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Expected Extraction Flow**:

**Level 1 (Parallel)**:
1. `GET /mock-api/accounts/550e8400-e29b-41d4-a716-446655440000`
   - Extract: `productCode = "CC-PREMIUM-001"`
2. `GET /mock-api/customers/123e4567-e89b-12d3-a456-426614174000`
   - Extract: `customerLocation = "CA"`

**Level 2 (Parallel)**:
3. `GET /mock-api/products/CC-PREMIUM-001`
   - Extract: `productCategory = "CREDIT_CARD"`
4. `POST /mock-api/regions/map` with body `{"state": "CA"}`
   - Extract: `regulatoryRegion = "US_WEST"`

**Level 3 (Sequential)**:
5. `GET /mock-api/disclosures?category=CREDIT_CARD&region=US_WEST`
   - Extract: `finalDisclosures = ["TILA", "CARD_ACT", "CA_DISCLOSURE", "CCPA"]`

**Expected Result**:
```json
{
  "productCode": "CC-PREMIUM-001",
  "customerLocation": "CA",
  "productCategory": "CREDIT_CARD",
  "regulatoryRegion": "US_WEST",
  "finalDisclosures": ["TILA", "CARD_ACT", "CA_DISCLOSURE", "CCPA"]
}
```

**Performance Note**:
- Without parallelization: ~5 API calls sequentially = ~5 seconds
- With parallelization: 3 levels = ~3 seconds (40% faster!)

**Documents Returned**:
- Credit_Card_Disclosures_2024.pdf (shared document)

---

### Test Scenario 3: 5-Step Deep Chain

**Template Type**: `BRANCH_SPECIFIC_DOCUMENT`

**Dependency Chain**:
```
accountId → branchCode → regionCode → complianceRules → documentList
```

**Test Request**:
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Expected Extraction Flow**:
1. **Level 1**: `GET /mock-api/accounts/550e8400-e29b-41d4-a716-446655440000`
   - Extract: `branchCode = "BR-555"`

2. **Level 2**: `GET /mock-api/branches/BR-555`
   - Extract: `regionCode = "WEST"`

3. **Level 3**: `GET /mock-api/compliance/regions/WEST`
   - Extract: `complianceRules = {...}`

4. **Level 4**: `POST /mock-api/document-rules` with compliance rules
   - Extract: `documentList = ["PRIVACY_NOTICE", "DATA_RETENTION_POLICY", ...]`

**Expected Result**:
```json
{
  "branchCode": "BR-555",
  "regionCode": "WEST",
  "complianceRules": {
    "dataRetention": "7_YEARS",
    "privacyStandard": "CCPA",
    "disclosureFrequency": "QUARTERLY"
  },
  "documentList": [
    "PRIVACY_NOTICE",
    "DATA_RETENTION_POLICY",
    "QUARTERLY_DISCLOSURE",
    "CCPA_RIGHTS_NOTICE"
  ]
}
```

**Documents Returned**:
- West_Region_Compliance_Guide.pdf (shared document)

---

## Testing Individual Mock APIs

### Test Account API
```bash
curl http://localhost:8080/mock-api/accounts/550e8400-e29b-41d4-a716-446655440000/details
```

**Expected Response**:
```json
{
  "account": {
    "accountId": "550e8400-e29b-41d4-a716-446655440000",
    "accountType": "CHECKING",
    "productCode": "CC-PREMIUM-001",
    "status": "ACTIVE",
    "branchCode": "BR-555",
    "balance": {
      "current": 5432.10,
      "available": 5432.10
    }
  }
}
```

### Test Product API
```bash
curl http://localhost:8080/mock-api/products/CC-PREMIUM-001
```

**Expected Response**:
```json
{
  "product": {
    "productCode": "CC-PREMIUM-001",
    "productName": "Premium Credit Card",
    "category": "CREDIT_CARD",
    "tier": "PREMIUM",
    "features": ["REWARDS", "TRAVEL_INSURANCE", "NO_FOREIGN_FEE"]
  }
}
```

### Test Customer API
```bash
curl http://localhost:8080/mock-api/customers/123e4567-e89b-12d3-a456-426614174000
```

**Expected Response**:
```json
{
  "customer": {
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "firstName": "John",
    "lastName": "Doe",
    "tier": "VIP",
    "address": {
      "street": "123 Main St",
      "city": "San Francisco",
      "state": "CA",
      "zipCode": "94102"
    }
  }
}
```

### Test Region Mapping API (POST)
```bash
curl -X POST http://localhost:8080/mock-api/regions/map \
  -H "Content-Type: application/json" \
  -d '{"state": "CA"}'
```

**Expected Response**:
```json
{
  "mapping": {
    "state": "CA",
    "region": "US_WEST",
    "timezone": "PST",
    "regulatoryBody": "CFPB_WEST"
  }
}
```

### Test Disclosure API
```bash
curl "http://localhost:8080/mock-api/disclosures?category=CREDIT_CARD&region=US_WEST"
```

**Expected Response**:
```json
{
  "disclosures": ["TILA", "CARD_ACT", "CA_DISCLOSURE", "CCPA"],
  "effectiveDate": "2024-01-01",
  "region": "US_WEST",
  "category": "CREDIT_CARD"
}
```

---

## Error Scenario Testing

### Test 1: Account Not Found
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["999e8400-e29b-41d4-a716-446655440999"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Expected Behavior**:
- First API call fails (404)
- System uses default values from config
- Returns documents with default field values

### Test 2: Invalid Product Code
```bash
# Manually call product API with invalid code
curl http://localhost:8080/mock-api/products/INVALID-CODE
```

**Expected Response**: 404 error
```json
{
  "error": "PRODUCT_NOT_FOUND",
  "message": "Product with code INVALID-CODE not found"
}
```

---

## Verification Checklist

### ✅ Scenario 1 (Simple 3-Step Chain)
- [ ] Account API called first
- [ ] productCode extracted successfully
- [ ] Product API called with extracted productCode
- [ ] productCategory extracted successfully
- [ ] Regulatory API called with extracted productCategory
- [ ] disclosureRequirements extracted successfully
- [ ] All 3 API calls executed sequentially
- [ ] Total execution time < 3 seconds

### ✅ Scenario 2 (Parallel + Sequential)
- [ ] Account API and Customer API called in parallel (Level 1)
- [ ] Product API and Region Mapping API called in parallel (Level 2)
- [ ] Disclosure API called last with both Level 2 results
- [ ] POST request body correctly resolved with customerLocation
- [ ] Total execution time < 3 seconds (faster than sequential)

### ✅ Scenario 3 (5-Step Deep Chain)
- [ ] All 5 levels execute in order
- [ ] Each level waits for previous result
- [ ] POST request body includes complex JSON object
- [ ] complianceRules correctly passed to document-rules API
- [ ] Total execution time < 5 seconds

### ✅ Error Handling
- [ ] 404 errors handled gracefully
- [ ] Default values used when API fails
- [ ] System continues or stops based on continueOnError flag
- [ ] Proper error logging

---

## Logging Verification

Check the application logs for:

```
INFO  ConfigurableDataExtractionService - Starting data extraction for 3 required fields
DEBUG ConfigurableDataExtractionService - Planning extraction for fields: [productCode, productCategory, disclosureRequirements]
DEBUG ConfigurableDataExtractionService - Available fields: [accountId, customerId, correlationId, auth.token]
DEBUG ConfigurableDataExtractionService - Added API 'accountDetailsApi' to plan (provides: [productCode])
DEBUG ConfigurableDataExtractionService - Added API 'productCatalogApi' to plan (provides: [productCategory])
DEBUG ConfigurableDataExtractionService - Added API 'regulatoryApi' to plan (provides: [disclosureRequirements])
INFO  ConfigurableDataExtractionService - Execution plan: 3 API calls
DEBUG ConfigurableDataExtractionService - Calling API: GET http://localhost:8080/mock-api/accounts/550e8400.../details
DEBUG ConfigurableDataExtractionService - Extracted productCode: CC-PREMIUM-001
DEBUG ConfigurableDataExtractionService - Calling API: GET http://localhost:8080/mock-api/products/CC-PREMIUM-001
DEBUG ConfigurableDataExtractionService - Extracted productCategory: CREDIT_CARD
DEBUG ConfigurableDataExtractionService - Calling API: GET http://localhost:8080/mock-api/regulatory/requirements?category=CREDIT_CARD
DEBUG ConfigurableDataExtractionService - Extracted disclosureRequirements: [TILA, CARD_ACT, SCHUMER_BOX]
INFO  ConfigurableDataExtractionService - Data extraction completed. Extracted 3 fields
```

---

## Test IDs Reference

Use these IDs for testing:

| Type | ID | Description |
|------|-----|-------------|
| Account ID | `550e8400-e29b-41d4-a716-446655440000` | Standard checking account |
| Account ID | `660e8400-e29b-41d4-a716-446655440001` | Savings account |
| Account ID | `999e8400-e29b-41d4-a716-446655440999` | Invalid (for error testing) |
| Customer ID | `123e4567-e89b-12d3-a456-426614174000` | VIP customer in CA |
| Product Code | `CC-PREMIUM-001` | Premium credit card |
| Product Code | `SAV-PREMIUM-001` | Premium savings |
| Product Code | `INVALID-CODE` | Invalid (for error testing) |
| Branch Code | `BR-555` | San Francisco branch (WEST region) |
| Branch Code | `BR-222` | New York branch (EAST region) |

---

## Performance Benchmarks

Expected execution times:

| Scenario | Sequential Time | Actual Time | APIs | Speedup |
|----------|----------------|-------------|------|---------|
| Scenario 1 | ~3s | ~3s | 3 sequential | N/A |
| Scenario 2 | ~5s | ~3s | 5 APIs, 3 levels | 40% |
| Scenario 3 | ~5s | ~5s | 5 sequential | N/A |

---

## Troubleshooting

### Issue: "Could not resolve all placeholders in URL"
**Cause**: Required field not available in context
**Fix**: Check field dependencies and execution order

### Issue: "API call failed"
**Cause**: Mock API not running or wrong URL
**Fix**: Verify MockApiController is loaded and URLs match

### Issue: "Field not extracted"
**Cause**: JSONPath expression incorrect
**Fix**: Test JSONPath expression with actual API response

### Issue: "Timeout"
**Cause**: API taking too long or not responding
**Fix**: Check timeout configuration in data_extraction_config

---

## Next Steps

1. Run all 3 test scenarios
2. Verify logs show correct execution flow
3. Check performance meets expectations
4. Test error scenarios
5. Add your own custom template types!
