# Postman Collection Guide

## Overview

This guide explains how to use the Postman collection for testing the Document Hub POC with disclosure code extraction.

## Import the Collection

1. Open Postman
2. Click **Import** button (top-left)
3. Select the file: `Document_Hub_POC.postman_collection.json`
4. Click **Import**

## Collection Structure

The collection is organized into 3 main folders:

### 1. Document Enquiry - End-to-End

Complete end-to-end tests that trigger the full extraction flow.

#### **Scenario 1: Standard Card (D164)**
- **Request**: POST to `/documents-enquiry`
- **Account ID**: `550e8400-e29b-41d4-a716-446655440000`
- **Expected Flow**:
  1. Call arrangements API → extract pricingId = `PRC-12345`
  2. Call pricing API → extract disclosureCode = `D164`
  3. Match document: `Credit_Card_Terms_D164_v1.pdf`

#### **Scenario 2: Premium Card (D166)**
- **Request**: POST to `/documents-enquiry`
- **Account ID**: `770e8400-e29b-41d4-a716-446655440002`
- **Expected Flow**:
  1. Call arrangements API → extract pricingId = `PRC-67890`
  2. Call pricing API → extract disclosureCode = `D166`
  3. Match document: `Premium_Credit_Card_Terms_D166_v1.pdf`

### 2. Mock APIs - Disclosure Code Flow

Individual API calls that show the step-by-step extraction process.

#### **Step 1: Get Arrangements**
- **Standard Card**: Returns pricingId = `PRC-12345`
- **Premium Card**: Returns pricingId = `PRC-67890`
- **JSONPath**: `$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId`

#### **Step 2: Get Pricing Data**
- **PRC-12345**: Returns disclosureCode = `D164` (Standard Card)
- **PRC-67890**: Returns disclosureCode = `D166` (Premium Card)
- **PRC-DEFAULT**: Returns disclosureCode = `D165` (Basic Card)
- **JSONPath**: `$.cardholderAgreementsTncCode`

### 3. Mock APIs - Other Scenarios

Additional endpoints for testing other extraction scenarios:
- Get Account Details
- Get Customer
- Get Product
- Get Branch
- Get Compliance Rules
- Get Regulatory Requirements
- Map Region (POST)
- Get Disclosures
- Get Document Rules (POST)

## Collection Variables

The collection includes predefined variables for easy testing:

| Variable | Value | Usage |
|----------|-------|-------|
| `baseUrl` | `http://localhost:8080` | Base URL for all requests |
| `standardAccountId` | `550e8400-e29b-41d4-a716-446655440000` | Standard credit card account |
| `premiumAccountId` | `770e8400-e29b-41d4-a716-446655440002` | Premium credit card account |
| `customerId` | `123e4567-e89b-12d3-a456-426614174000` | Test customer ID |

### Using Variables in Requests

You can use these variables in your requests:
```
{{baseUrl}}/documents-enquiry
{{standardAccountId}}
{{premiumAccountId}}
```

## Quick Start Testing

### Prerequisites

1. **Start the application**:
   ```bash
   cd fresh-doc-hub-poc
   mvn spring-boot:run
   ```

2. **Verify application is running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Load test data** (if not already loaded):
   ```bash
   psql -U postgres -d document_hub -f src/main/resources/test-data-disclosure-example-postgres.sql
   ```

### Test Sequence

#### Option 1: End-to-End Testing

Run the requests in this order:

1. **Document Enquiry - Scenario 1 (Standard Card)**
   - Should return `Credit_Card_Terms_D164_v1.pdf`
   - Check response for successful extraction

2. **Document Enquiry - Scenario 2 (Premium Card)**
   - Should return `Premium_Credit_Card_Terms_D166_v1.pdf`
   - Verify different disclosure code matched

#### Option 2: Step-by-Step Testing

To see the extraction flow in detail:

1. **Get Arrangements (Standard Card)**
   - Verify response contains PRICING domain
   - Verify pricingId = `PRC-12345`

2. **Get Pricing Data (PRC-12345)**
   - Verify disclosureCode = `D164`
   - Note the productType and fees

3. **Document Enquiry - Scenario 1**
   - See how the system uses extracted data
   - Verify document matching works

Repeat for Premium Card scenario with different account ID.

## Expected Responses

### Document Enquiry Response (Success)

```json
{
  "documents": [
    {
      "storageIndexId": "44444444-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "templateType": "CardholderAgreement",
      "fileName": "Credit_Card_Terms_D164_v1.pdf",
      "referenceKey": "D164",
      "referenceKeyType": "DISCLOSURE_CODE",
      "docMetadata": {
        "disclosureCode": "D164",
        "version": "1.0",
        "effectiveDate": "2024-01-01",
        "documentType": "CARDHOLDER_AGREEMENT"
      }
    }
  ],
  "totalDocuments": 1
}
```

### Arrangements API Response

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

### Pricing API Response

```json
{
  "pricingId": "PRC-12345",
  "cardholderAgreementsTncCode": "D164",
  "productType": "STANDARD_CREDIT_CARD",
  "annualFee": 0,
  "interestRate": 18.99,
  "effectiveDate": "2024-01-01",
  "currency": "USD",
  "lastUpdated": "2024-01-15"
}
```

## Testing Tips

### 1. Use Postman Console

Open Postman Console (View → Show Postman Console) to see:
- Request/response details
- Network timing
- Error messages

### 2. Check Response Status

- **200 OK**: Request successful
- **404 Not Found**: Resource not found (check IDs)
- **500 Internal Server Error**: Check application logs

### 3. Validate JSONPath Extraction

Test JSONPath expressions using the Mock API responses:

**Arrangements JSONPath**:
```
$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId
```

This should extract: `"PRC-12345"`

**Pricing JSONPath**:
```
$.cardholderAgreementsTncCode
```

This should extract: `"D164"`

### 4. Compare Different Scenarios

Run both Standard and Premium scenarios side-by-side:
- Standard: PRC-12345 → D164 → Standard terms
- Premium: PRC-67890 → D166 → Premium terms

Notice how the same template returns different documents based on extracted data.

## Troubleshooting

### Issue: "Connection refused"

**Solution**: Verify the application is running on port 8080
```bash
curl http://localhost:8080/actuator/health
```

### Issue: "No documents found"

**Possible causes**:
1. Test data not loaded
2. Extraction failed (check logs)
3. Document matching criteria not met

**Solution**: Check application logs for extraction details:
```
DEBUG ConfigurableDataExtractionService - Extracted pricingId: PRC-12345
DEBUG ConfigurableDataExtractionService - Extracted disclosureCode: D164
```

### Issue: "404 Not Found" for mock APIs

**Possible causes**:
1. Wrong account ID
2. Wrong pricing ID
3. Endpoint path incorrect

**Solution**: Verify the IDs match the test data:
- Standard account: `550e8400-e29b-41d4-a716-446655440000`
- Premium account: `770e8400-e29b-41d4-a716-446655440002`

### Issue: Wrong disclosure code extracted

**Solution**:
1. Check arrangements API response
2. Verify correct pricingId extracted
3. Check pricing API response for that pricingId
4. Verify disclosureCode in response

## Advanced Usage

### 1. Environment Variables

Create different environments for different setups:

**Local Development**:
```
baseUrl = http://localhost:8080
```

**Docker**:
```
baseUrl = http://localhost:8080
```

**Remote Testing**:
```
baseUrl = https://your-server.com
```

### 2. Test Scripts

Add test scripts to automate validation:

```javascript
// Test for successful response
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// Test for disclosure code extraction
pm.test("Disclosure code extracted", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.cardholderAgreementsTncCode).to.eql("D164");
});

// Test for document match
pm.test("Document matched", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.documents).to.have.lengthOf(1);
    pm.expect(jsonData.documents[0].referenceKey).to.eql("D164");
});
```

### 3. Collection Runner

Run all tests automatically:

1. Click on collection name → **Run**
2. Select folder to run (e.g., "Mock APIs - Disclosure Code Flow")
3. Click **Run Document Hub POC**
4. View results with pass/fail status

### 4. Export Test Results

After running tests:
1. Click **Export Results**
2. Choose format (JSON/CSV)
3. Save for documentation/reporting

## Test Data Reference

### Account IDs and Expected Results

| Account ID | Pricing ID | Disclosure Code | Document |
|------------|------------|-----------------|----------|
| `550e8400-e29b-41d4-a716-446655440000` | PRC-12345 | D164 | Credit_Card_Terms_D164_v1.pdf |
| `770e8400-e29b-41d4-a716-446655440002` | PRC-67890 | D166 | Premium_Credit_Card_Terms_D166_v1.pdf |

### Pricing ID Mappings

| Pricing ID | Disclosure Code | Product Type |
|------------|-----------------|--------------|
| PRC-12345 | D164 | STANDARD_CREDIT_CARD |
| PRC-67890 | D166 | PREMIUM_CREDIT_CARD |
| PRC-DEFAULT | D165 | BASIC_CREDIT_CARD |

## Related Documentation

- **DISCLOSURE_CODE_TESTING_GUIDE.md** - Detailed testing scenarios
- **DISCLOSURE_CODE_SUMMARY.md** - Implementation overview
- **MULTI_STEP_EXTRACTION.md** - Multi-step extraction concepts
- **DATA_EXTRACTION_CONFIG_SCHEMA.md** - Configuration schema

## Summary

✅ **Import**: Load `Document_Hub_POC.postman_collection.json` into Postman
✅ **Configure**: Verify `baseUrl` variable points to your server
✅ **Test**: Run end-to-end scenarios first
✅ **Debug**: Use step-by-step API calls to understand flow
✅ **Validate**: Check logs and responses match expected results

The Postman collection provides a complete testing toolkit for the disclosure code extraction feature and all mock API endpoints.
