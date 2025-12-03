# Mock Data Summary

## Overview

Complete mock data setup for testing multi-step data extraction with 3 different scenarios, including a working mock API controller.

## Files Created

### 1. **test-data.sql**
Database mock data with 3 template types and associated documents

**Templates**:
- `ACCOUNT_STATEMENT` - Simple 3-step chain
- `REGULATORY_DISCLOSURE` - Parallel + sequential diamond pattern
- `BRANCH_SPECIFIC_DOCUMENT` - 5-step deep chain

**Documents**:
- 2 account-specific statement documents
- 2 shared regulatory/compliance documents

### 2. **mock-api-responses.json**
Complete API response examples for all endpoints

**Scenarios Covered**:
- Scenario 1: Simple 3-step chain (3 API calls)
- Scenario 2: Parallel + sequential (5 API calls)
- Scenario 3: 5-step deep chain (4 API calls)
- Additional test data for different account types
- Error scenarios (404, invalid codes)

### 3. **MockApiController.java**
Spring Boot REST controller simulating external APIs

**Endpoints Implemented**:
- `GET /mock-api/accounts/{accountId}/details` - Account details
- `GET /mock-api/accounts/{accountId}` - Account summary
- `GET /mock-api/customers/{customerId}` - Customer profile
- `GET /mock-api/products/{productCode}` - Product details
- `GET /mock-api/regulatory/requirements` - Regulatory rules
- `POST /mock-api/regions/map` - State to region mapping
- `GET /mock-api/disclosures` - Final disclosure requirements
- `GET /mock-api/branches/{branchCode}` - Branch details
- `GET /mock-api/compliance/regions/{regionCode}` - Compliance rules
- `POST /mock-api/document-rules` - Document rules calculation

### 4. **TESTING_GUIDE.md**
Comprehensive testing guide with curl commands and verification checklists

## Quick Start

### Step 1: Load Database
```sql
psql -U postgres -d document_hub -f src/main/resources/test-data.sql
```

### Step 2: Start Application
```bash
mvn spring-boot:run
```

### Step 3: Test Scenario 1 (Simple Chain)
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

## Test Data Reference

### Test Accounts

| Account ID | Type | Product Code | Branch | Description |
|------------|------|--------------|--------|-------------|
| `550e8400-e29b-41d4-a716-446655440000` | CHECKING | CC-PREMIUM-001 | BR-555 | Primary test account |
| `660e8400-e29b-41d4-a716-446655440001` | SAVINGS | SAV-PREMIUM-001 | BR-222 | Savings account |
| `999e8400-e29b-41d4-a716-446655440999` | N/A | N/A | N/A | Error scenario (404) |

### Test Customers

| Customer ID | Name | Tier | Location | Region |
|-------------|------|------|----------|--------|
| `123e4567-e89b-12d3-a456-426614174000` | John Doe | VIP | San Francisco, CA | US_WEST |

### Product Codes

| Product Code | Name | Category | Tier |
|--------------|------|----------|------|
| CC-PREMIUM-001 | Premium Credit Card | CREDIT_CARD | PREMIUM |
| SAV-PREMIUM-001 | Premium Savings | SAVINGS | PREMIUM |
| INVALID-CODE | N/A | N/A | Error scenario |

### Branch Codes

| Branch Code | Name | Region | Location |
|-------------|------|--------|----------|
| BR-555 | Downtown San Francisco | WEST | San Francisco, CA |
| BR-222 | Manhattan Central | EAST | New York, NY |

## Expected Results

### Scenario 1: Simple 3-Step Chain
**Execution Flow**: Account → Product → Regulatory

**Extracted Data**:
```json
{
  "productCode": "CC-PREMIUM-001",
  "productCategory": "CREDIT_CARD",
  "disclosureRequirements": ["TILA", "CARD_ACT", "SCHUMER_BOX"]
}
```

**Documents**: 2 account statements

---

### Scenario 2: Parallel + Sequential
**Execution Flow**:
```
Level 1 (parallel): Account + Customer
Level 2 (parallel): Product + Region Mapping
Level 3: Final Disclosures
```

**Extracted Data**:
```json
{
  "productCode": "CC-PREMIUM-001",
  "customerLocation": "CA",
  "productCategory": "CREDIT_CARD",
  "regulatoryRegion": "US_WEST",
  "finalDisclosures": ["TILA", "CARD_ACT", "CA_DISCLOSURE", "CCPA"]
}
```

**Documents**: 1 shared disclosure document

---

### Scenario 3: 5-Step Deep Chain
**Execution Flow**: Account → Branch → Region → Compliance → Documents

**Extracted Data**:
```json
{
  "branchCode": "BR-555",
  "regionCode": "WEST",
  "complianceRules": {
    "dataRetention": "7_YEARS",
    "privacyStandard": "CCPA",
    "disclosureFrequency": "QUARTERLY"
  },
  "documentList": ["PRIVACY_NOTICE", "DATA_RETENTION_POLICY", "QUARTERLY_DISCLOSURE", "CCPA_RIGHTS_NOTICE"]
}
```

**Documents**: 1 shared compliance document

## Mock API Behavior

### Success Responses
All mock APIs return appropriate data based on input IDs

### Error Responses
- Account ID `999...999`: Returns 404
- Product Code `INVALID-CODE`: Returns 404
- Region Code `INVALID`: Returns 404

### Response Variations
- Different states map to different regions (CA→US_WEST, NY→US_EAST, etc.)
- Product categories trigger different regulatory requirements
- Compliance rules vary by region

## Testing Verification

### ✅ What to Check

1. **Execution Order**
   - APIs called in correct dependency order
   - Parallel APIs execute simultaneously
   - Sequential APIs wait for dependencies

2. **Data Extraction**
   - All required fields extracted
   - JSONPath expressions work correctly
   - Placeholders resolved properly

3. **Error Handling**
   - 404 errors handled gracefully
   - Default values used when configured
   - System continues/stops based on config

4. **Performance**
   - Parallel scenarios faster than sequential
   - Total time within timeout limits
   - No unnecessary API calls

5. **Documents**
   - Correct templates returned
   - Account-specific vs shared logic works
   - Metadata matches extracted fields

## Sample Curl Commands

### Test Individual APIs

**Account**:
```bash
curl http://localhost:8080/mock-api/accounts/550e8400-e29b-41d4-a716-446655440000
```

**Customer**:
```bash
curl http://localhost:8080/mock-api/customers/123e4567-e89b-12d3-a456-426614174000
```

**Product**:
```bash
curl http://localhost:8080/mock-api/products/CC-PREMIUM-001
```

**Region Mapping** (POST):
```bash
curl -X POST http://localhost:8080/mock-api/regions/map \
  -H "Content-Type: application/json" \
  -d '{"state": "CA"}'
```

**Disclosures**:
```bash
curl "http://localhost:8080/mock-api/disclosures?category=CREDIT_CARD&region=US_WEST"
```

## Customization

### Add New Template Type

1. Insert into `master_template_definition` with `data_extraction_config` JSON
2. Add corresponding mock API responses if needed
3. Create storage index entries for documents

### Add New Mock API

1. Add endpoint to `MockApiController.java`
2. Update `data_extraction_config` to reference new API
3. Test with curl

### Modify Extraction Logic

Edit the `data_extraction_config` JSON in `test-data.sql`:
- Add/remove required fields
- Change API endpoints
- Modify execution strategy
- Update field dependencies

## Troubleshooting

### "No data extracted"
- Check if mock APIs are responding
- Verify JSONPath expressions match response structure
- Ensure placeholders are resolving correctly

### "Timeout"
- Increase timeout in `executionStrategy`
- Check if mock API is slow
- Verify network connectivity

### "Field not found"
- Check field dependencies
- Ensure required inputs are available
- Verify API provides the field

### "Wrong execution order"
- Review `requiredInputs` for each field
- Check if dependencies are correctly specified
- Verify execution mode is "auto"

## Summary

✅ **3 Complete Test Scenarios** - Simple, parallel, and deep chains
✅ **Working Mock API** - All endpoints functional
✅ **Sample Data** - Templates and documents ready
✅ **Error Scenarios** - 404 handling tested
✅ **Testing Guide** - Step-by-step instructions
✅ **Verification Checklists** - Know what to expect

Everything needed to test multi-step data extraction is ready to use!
