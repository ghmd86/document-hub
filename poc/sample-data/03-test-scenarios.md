# Document Hub POC - Test Scenarios

**Version**: 1.0
**Date**: 2025-11-27
**Purpose**: Comprehensive test scenarios for validating the Document Hub POC functionality

---

## Table of Contents

1. [Test Environment Setup](#test-environment-setup)
2. [Template-Specific Test Scenarios](#template-specific-test-scenarios)
3. [Integration Test Scenarios](#integration-test-scenarios)
4. [Error Handling Scenarios](#error-handling-scenarios)
5. [Performance Test Scenarios](#performance-test-scenarios)
6. [Mock API Responses](#mock-api-responses)

---

## Test Environment Setup

### Prerequisites

1. **Database**: PostgreSQL with sample data loaded
   ```bash
   psql -U username -d dochub_poc -f 01-templates.sql
   psql -U username -d dochub_poc -f 02-documents.sql
   ```

2. **Mock Services**: External API mock server running on port 8081
   - Customer Service API
   - Account Service API
   - Pricing Service API
   - VIP Service API
   - Content Service API

3. **Application**: Document Hub service running on port 8080
   ```bash
   cd poc/doc-hub-poc
   mvn spring-boot:run
   ```

### Test Data Reference

**Customers**:
- `C001` - Regular customer in California (Account: A001)
- `C002` - Regular customer in New York (Account: A002)
- `C003` - VIP customer in Texas (Account: A003)
- `C004` - VIP customer in California (Account: A004)

**Templates**:
- `MONTHLY_STATEMENT` - Simple account-based matching
- `PRIVACY_POLICY` - Single API call + metadata matching
- `CARDHOLDER_AGREEMENT` - 2-step API chain + reference key matching
- `VIP_LETTER` - 3-step API chain + composite matching

---

## Template-Specific Test Scenarios

### Scenario 1: Monthly Statement Retrieval (No External Calls)

**Template**: `MONTHLY_STATEMENT`
**Complexity**: Simple
**External API Calls**: None

#### Test Case 1.1: Basic Account Statement Retrieval

**Request**:
```http
POST /api/documents/enquiry
Content-Type: application/json
X-Correlation-ID: test-001

{
  "customerId": "C001",
  "accountId": "A001",
  "templateType": "MONTHLY_STATEMENT"
}
```

**Expected Response**:
```json
{
  "documents": [
    {
      "documentId": "<uuid>",
      "templateType": "MONTHLY_STATEMENT",
      "documentName": "February 2024 Statement",
      "documentDescription": "Monthly statement for account A001 - February 2024",
      "storageLocation": "s3://document-hub/statements/2024/02/A001-202402.pdf",
      "fileName": "A001-202402.pdf",
      "fileType": "application/pdf",
      "fileSizeBytes": 985432,
      "metadata": {
        "statementDate": "2024-02-29",
        "statementPeriod": "2024-02",
        "accountNumber": "****1234",
        "balance": 2345.67,
        "minimumDue": 75.00
      }
    },
    {
      "documentId": "<uuid>",
      "templateType": "MONTHLY_STATEMENT",
      "documentName": "January 2024 Statement",
      "documentDescription": "Monthly statement for account A001 - January 2024",
      "storageLocation": "s3://document-hub/statements/2024/01/A001-202401.pdf",
      "fileName": "A001-202401.pdf",
      "fileType": "application/pdf",
      "fileSizeBytes": 1024567,
      "metadata": {
        "statementDate": "2024-01-31",
        "statementPeriod": "2024-01",
        "accountNumber": "****1234",
        "balance": 1234.56,
        "minimumDue": 50.00
      }
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  },
  "processingTimeMs": 50
}
```

**Validation**:
- ✅ Returns 2 documents for account A001
- ✅ No external API calls made
- ✅ Documents ordered by creation date (newest first)
- ✅ Response time < 100ms
- ✅ Metadata correctly included

#### Test Case 1.2: Account with No Documents

**Request**:
```http
POST /api/documents/enquiry
Content-Type: application/json

{
  "customerId": "C999",
  "accountId": "A999",
  "templateType": "MONTHLY_STATEMENT"
}
```

**Expected Response**:
```json
{
  "documents": [],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  },
  "processingTimeMs": 30
}
```

**Validation**:
- ✅ Empty document list
- ✅ HTTP 200 OK (not 404)
- ✅ Fast response (no external calls)

---

### Scenario 2: Privacy Policy Retrieval (Single API Call)

**Template**: `PRIVACY_POLICY`
**Complexity**: Medium
**External API Calls**: 1 (Customer Location Service)

#### Test Case 2.1: California Customer Privacy Policy

**Mock API Setup**:
```
GET http://localhost:8081/api/customers/C001/location
Response:
{
  "customerId": "C001",
  "state": "CA",
  "country": "US",
  "city": "San Francisco",
  "zipCode": "94102"
}
```

**Request**:
```http
POST /api/documents/enquiry
Content-Type: application/json
X-Correlation-ID: test-002

{
  "customerId": "C001",
  "templateType": "PRIVACY_POLICY"
}
```

**Expected Execution Flow**:
1. Template loaded from database
2. Data extraction strategy executed:
   - Call GET `/api/customers/C001/location`
   - Extract `$location.state` = "CA"
   - Validate state format (2 uppercase letters)
3. Eligibility check: `$location.state` is not null ✅
4. Document matching: Find documents where `metadata.state = "CA"`
5. Return matching document(s)

**Expected Response**:
```json
{
  "documents": [
    {
      "documentId": "<uuid>",
      "templateType": "PRIVACY_POLICY",
      "documentName": "California Privacy Policy",
      "documentDescription": "CCPA-compliant privacy policy for California residents",
      "storageLocation": "s3://document-hub/regulatory/privacy/CA-Privacy-Policy-2024.pdf",
      "fileName": "CA-Privacy-Policy-2024.pdf",
      "fileType": "application/pdf",
      "fileSizeBytes": 2567890,
      "isShared": true,
      "metadata": {
        "state": "CA",
        "country": "US",
        "regulationType": "CCPA",
        "effectiveDate": "2024-01-01",
        "version": "2024.1"
      }
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "processingTimeMs": 250
}
```

**Validation**:
- ✅ Correct state-specific document returned
- ✅ External API called exactly once
- ✅ Extracted data used for matching
- ✅ Shared document flag = true
- ✅ Response time < 500ms

#### Test Case 2.2: New York Customer Privacy Policy

**Mock API Setup**:
```
GET http://localhost:8081/api/customers/C002/location
Response:
{
  "customerId": "C002",
  "state": "NY",
  "country": "US"
}
```

**Request**:
```http
POST /api/documents/enquiry

{
  "customerId": "C002",
  "templateType": "PRIVACY_POLICY"
}
```

**Expected Response**: NY-specific privacy policy document

**Validation**:
- ✅ Returns NY privacy policy (not CA)
- ✅ Correct metadata matching logic

#### Test Case 2.3: Customer Location Not Found

**Mock API Setup**:
```
GET http://localhost:8081/api/customers/C999/location
Response: 404 Not Found
```

**Request**:
```http
POST /api/documents/enquiry

{
  "customerId": "C999",
  "templateType": "PRIVACY_POLICY"
}
```

**Expected Response**:
```json
{
  "documents": [],
  "errors": [
    {
      "code": "DATA_EXTRACTION_FAILED",
      "message": "Failed to extract customer location"
    }
  ],
  "processingTimeMs": 150
}
```

**Validation**:
- ✅ Graceful error handling
- ✅ Empty document list
- ✅ Error details provided

---

### Scenario 3: Cardholder Agreement (2-Step API Chain)

**Template**: `CARDHOLDER_AGREEMENT`
**Complexity**: High
**External API Calls**: 2 (Sequential)

#### Test Case 3.1: Standard Tier Cardholder Agreement

**Mock API Setup**:

Step 1 - Get Pricing ID:
```
GET http://localhost:8081/api/accounts/A001/pricing
Response:
{
  "accountId": "A001",
  "pricingId": "P001",
  "tier": "STANDARD",
  "effectiveDate": "2024-01-01"
}
```

Step 2 - Get Disclosure Code (using pricingId from Step 1):
```
GET http://localhost:8081/api/pricing/P001/disclosure
Response:
{
  "pricingId": "P001",
  "disclosureCode": "D001",
  "version": "2024.1"
}
```

**Request**:
```http
POST /api/documents/enquiry
X-Correlation-ID: test-003

{
  "customerId": "C001",
  "accountId": "A001",
  "templateType": "CARDHOLDER_AGREEMENT"
}
```

**Expected Execution Flow**:
1. Load template
2. Execute data extraction strategy (sequential):
   - **Step 1**: Call `/api/accounts/A001/pricing`
     - Extract `$pricing.pricingId` = "P001"
     - Extract `$pricing.tier` = "STANDARD"
     - Validate pricingId is present
     - Check nextCalls condition: pricingId is not null ✅
   - **Step 2**: Call `/api/pricing/P001/disclosure` (URL uses `${pricing.pricingId}`)
     - Extract `$disclosure.code` = "D001"
     - Extract `$disclosure.version` = "2024.1"
     - Validate code matches pattern `^D[0-9]{3,4}$`
3. Eligibility check:
   - `$pricing.pricingId` is not null ✅
   - `$disclosure.code` is not null ✅
4. Document matching (reference_key strategy):
   - Find documents where `reference_key = "D001"` AND `reference_key_type = "DISCLOSURE_CODE"`
5. Return matching document

**Expected Response**:
```json
{
  "documents": [
    {
      "documentId": "<uuid>",
      "templateType": "CARDHOLDER_AGREEMENT",
      "documentName": "Standard Cardholder Agreement",
      "documentDescription": "Cardholder agreement for standard pricing tier",
      "storageLocation": "s3://document-hub/agreements/cardholder/D001-Standard-Agreement.pdf",
      "fileName": "D001-Standard-Agreement.pdf",
      "referenceKey": "D001",
      "referenceKeyType": "DISCLOSURE_CODE",
      "metadata": {
        "disclosureCode": "D001",
        "pricingTier": "STANDARD",
        "apr": 19.99,
        "annualFee": 0,
        "version": "2024.1"
      }
    }
  ],
  "processingTimeMs": 450
}
```

**Validation**:
- ✅ Two API calls made in sequence
- ✅ Variables from Step 1 used in Step 2 URL
- ✅ Conditional chaining worked (nextCalls)
- ✅ Correct disclosure document returned
- ✅ Response time < 1000ms

#### Test Case 3.2: VIP Tier Cardholder Agreement

**Mock API Setup**:
```
Step 1: GET /api/accounts/A003/pricing
{
  "accountId": "A003",
  "pricingId": "P003",
  "tier": "VIP"
}

Step 2: GET /api/pricing/P003/disclosure
{
  "pricingId": "P003",
  "disclosureCode": "D003",
  "version": "2024.1"
}
```

**Request**:
```http
POST /api/documents/enquiry

{
  "customerId": "C003",
  "accountId": "A003",
  "templateType": "CARDHOLDER_AGREEMENT"
}
```

**Expected Response**: VIP Cardholder Agreement (D003)

**Validation**:
- ✅ Correct VIP-tier document returned
- ✅ Multi-step extraction works for different tiers

---

### Scenario 4: VIP Letter (3-Step API Chain + Complex Matching)

**Template**: `VIP_LETTER`
**Complexity**: Very High
**External API Calls**: 3 (Sequential with conditional chaining)

#### Test Case 4.1: VIP Customer with Active Offer

**Mock API Setup**:

Step 1 - Get Customer Tier:
```
GET http://localhost:8081/api/customers/C003/tier
Response:
{
  "customerId": "C003",
  "tier": "VIP",
  "status": "ACTIVE",
  "vipJoinDate": "2023-06-15"
}
```

Step 2 - Get VIP Benefits (conditional on tier=VIP):
```
GET http://localhost:8081/api/vip/benefits?tier=VIP
Response:
{
  "tier": "VIP",
  "currentOfferCode": "SUMMER2024",
  "offerExpiryDate": "2024-09-30",
  "benefits": ["concierge", "lounge_access", "triple_points"]
}
```

Step 3 - Get Personalized Content (conditional on offerCode exists):
```
GET http://localhost:8081/api/content/personalized?offer=SUMMER2024&customer=C003
Response:
{
  "customerId": "C003",
  "offerCode": "SUMMER2024",
  "letterTemplateId": "VIP_SUMMER_TEMPLATE",
  "personalizationData": {
    "firstName": "John",
    "vipSince": "2023"
  }
}
```

**Request**:
```http
POST /api/documents/enquiry
X-Correlation-ID: test-004

{
  "customerId": "C003",
  "templateType": "VIP_LETTER"
}
```

**Expected Execution Flow**:
1. Load template
2. Execute 3-step extraction strategy:
   - **Step 1**: Get customer tier
     - Extract: `$customer.tier` = "VIP", `$customer.status` = "ACTIVE"
     - Condition check: tier == "VIP" ✅ → proceed to Step 2
   - **Step 2**: Get VIP benefits
     - Extract: `$vip.offerCode` = "SUMMER2024"
     - Condition check: offerCode not null ✅ → proceed to Step 3
   - **Step 3**: Get personalized content
     - Extract: `$content.templateId` = "VIP_SUMMER_TEMPLATE"
3. Eligibility check (3 conditions with AND logic):
   - `$customer.tier` equals "VIP" ✅
   - `$customer.status` equals "ACTIVE" ✅
   - `$vip.offerCode` is not null ✅
4. Document matching (composite strategy - 2 metadata conditions):
   - `metadata.tier` = `$customer.tier` ("VIP")
   - `metadata.offerCode` = `$vip.offerCode` ("SUMMER2024")
5. Return matching document

**Expected Response**:
```json
{
  "documents": [
    {
      "documentId": "<uuid>",
      "templateType": "VIP_LETTER",
      "documentName": "VIP Summer Welcome Letter",
      "documentDescription": "Personalized VIP welcome letter with summer 2024 offer",
      "storageLocation": "s3://document-hub/marketing/vip/VIP-Summer-2024-C003.pdf",
      "customerKey": "C003",
      "metadata": {
        "tier": "VIP",
        "offerCode": "SUMMER2024",
        "offerDescription": "Triple points on travel",
        "expiryDate": "2024-09-30",
        "personalized": true
      }
    }
  ],
  "processingTimeMs": 800
}
```

**Validation**:
- ✅ Three API calls executed in sequence
- ✅ Conditional chaining worked at each step
- ✅ Variables passed between steps correctly
- ✅ Complex eligibility rules evaluated (3 AND conditions)
- ✅ Composite matching (2 metadata fields)
- ✅ Correct offer-specific document returned
- ✅ Response time < 1500ms

#### Test Case 4.2: Non-VIP Customer (Conditional Chain Stops Early)

**Mock API Setup**:
```
Step 1: GET /api/customers/C001/tier
{
  "customerId": "C001",
  "tier": "STANDARD",
  "status": "ACTIVE"
}
```

**Request**:
```http
POST /api/documents/enquiry

{
  "customerId": "C001",
  "templateType": "VIP_LETTER"
}
```

**Expected Execution Flow**:
1. Step 1 executes: tier = "STANDARD"
2. Condition check: tier == "VIP" ❌
3. Step 2 and Step 3 **NOT executed** (conditional chaining stops)
4. Eligibility check fails: tier != "VIP"
5. Return empty documents

**Expected Response**:
```json
{
  "documents": [],
  "message": "Customer not eligible for VIP letters",
  "processingTimeMs": 200
}
```

**Validation**:
- ✅ Only 1 API call made (not all 3)
- ✅ Conditional chaining prevented unnecessary calls
- ✅ Eligibility check correctly failed
- ✅ Fast response (no wasted API calls)

---

## Integration Test Scenarios

### Scenario 5: Pagination

**Request**:
```http
POST /api/documents/enquiry?page=0&size=1

{
  "customerId": "C001",
  "accountId": "A001",
  "templateType": "MONTHLY_STATEMENT"
}
```

**Expected Response**:
```json
{
  "documents": [
    { "documentName": "February 2024 Statement" }
  ],
  "pagination": {
    "page": 0,
    "size": 1,
    "totalElements": 2,
    "totalPages": 2
  },
  "links": {
    "self": "/api/documents/enquiry?page=0&size=1",
    "first": "/api/documents/enquiry?page=0&size=1",
    "next": "/api/documents/enquiry?page=1&size=1",
    "last": "/api/documents/enquiry?page=1&size=1"
  }
}
```

**Validation**:
- ✅ Only 1 document returned
- ✅ HATEOAS links present
- ✅ Correct totalElements and totalPages

---

### Scenario 6: Cache Testing

#### Test Case 6.1: First Request (Cache Miss)

**Request 1**:
```http
POST /api/documents/enquiry
X-Correlation-ID: cache-test-1

{
  "customerId": "C001",
  "templateType": "PRIVACY_POLICY"
}
```

**Expected**:
- External API called
- Response time: ~250ms
- Cache entry created: `customer:C001:location` (TTL: 3600s)

#### Test Case 6.2: Second Request (Cache Hit)

**Request 2** (same customer, within TTL):
```http
POST /api/documents/enquiry
X-Correlation-ID: cache-test-2

{
  "customerId": "C001",
  "templateType": "PRIVACY_POLICY"
}
```

**Expected**:
- External API **NOT called**
- Response time: ~50ms (much faster)
- Data served from cache

**Validation**:
- ✅ Cache working correctly
- ✅ Significant performance improvement
- ✅ Same result as uncached request

---

## Error Handling Scenarios

### Scenario 7: External API Errors

#### Test Case 7.1: API Timeout

**Mock API**: Delay response by 6 seconds (exceeds 5s timeout)

**Expected**:
```json
{
  "documents": [],
  "errors": [
    {
      "code": "API_TIMEOUT",
      "message": "External service timed out",
      "details": "get_customer_location timeout after 5000ms"
    }
  ]
}
```

#### Test Case 7.2: API Returns 500 Error

**Mock API**: Return HTTP 500

**Expected**:
- Circuit breaker may activate
- Graceful error response
- Empty document list

---

### Scenario 8: Validation Errors

#### Test Case 8.1: Missing Required Field

**Request**:
```http
POST /api/documents/enquiry

{
  "templateType": "MONTHLY_STATEMENT"
}
```

**Expected**:
```json
{
  "error": "BAD_REQUEST",
  "message": "customerId is required"
}
```

**HTTP Status**: 400 Bad Request

---

## Performance Test Scenarios

### Scenario 9: Load Testing

**Test**: 100 concurrent requests for MONTHLY_STATEMENT

**Metrics to Track**:
- Average response time
- p95 response time
- p99 response time
- Throughput (req/sec)
- Error rate

**Success Criteria**:
- Average response time < 100ms
- p95 < 150ms
- p99 < 200ms
- Error rate < 0.1%

---

### Scenario 10: Cache Performance

**Test**: 1000 requests with 80% cache hit rate

**Expected**:
- Cached requests: ~50ms avg
- Non-cached requests: ~250ms avg
- Overall average: ~100ms

---

## Mock API Responses

### Customer Service Mock

```javascript
// Mock responses for customer service
const mockCustomerLocations = {
  "C001": { "state": "CA", "country": "US" },
  "C002": { "state": "NY", "country": "US" },
  "C003": { "state": "TX", "country": "US" },
  "C004": { "state": "CA", "country": "US" }
};

const mockCustomerTiers = {
  "C001": { "tier": "STANDARD", "status": "ACTIVE" },
  "C002": { "tier": "PREMIUM", "status": "ACTIVE" },
  "C003": { "tier": "VIP", "status": "ACTIVE", "vipJoinDate": "2023-06-15" },
  "C004": { "tier": "VIP", "status": "ACTIVE", "vipJoinDate": "2022-01-10" }
};
```

### Account Service Mock

```javascript
const mockAccountPricing = {
  "A001": { "pricingId": "P001", "tier": "STANDARD" },
  "A002": { "pricingId": "P002", "tier": "PREMIUM" },
  "A003": { "pricingId": "P003", "tier": "VIP" },
  "A004": { "pricingId": "P003", "tier": "VIP" }
};
```

### Pricing Service Mock

```javascript
const mockDisclosureCodes = {
  "P001": { "disclosureCode": "D001", "version": "2024.1" },
  "P002": { "disclosureCode": "D002", "version": "2024.1" },
  "P003": { "disclosureCode": "D003", "version": "2024.1" }
};
```

### VIP Service Mock

```javascript
const mockVipBenefits = {
  "VIP": {
    "currentOfferCode": "SUMMER2024",
    "offerExpiryDate": "2024-09-30"
  }
};
```

---

## Test Execution Checklist

### Unit Tests
- [ ] PlaceholderResolver resolves variables correctly
- [ ] JsonPathExtractor extracts data from JSON
- [ ] RuleEvaluationService evaluates all operators
- [ ] DocumentMatchingService matches all strategies

### Integration Tests
- [ ] All 4 templates execute successfully
- [ ] Sequential execution works (multi-step chains)
- [ ] Conditional chaining works (nextCalls)
- [ ] Pagination works correctly
- [ ] Cache works (hit/miss)

### End-to-End Tests
- [ ] Test Case 1.1: Monthly statements
- [ ] Test Case 2.1: Privacy policy (CA)
- [ ] Test Case 2.2: Privacy policy (NY)
- [ ] Test Case 3.1: Cardholder agreement (2-step)
- [ ] Test Case 4.1: VIP letter (3-step)
- [ ] Test Case 4.2: Non-VIP rejection

### Error Handling
- [ ] API timeout handled gracefully
- [ ] API 500 error handled
- [ ] Missing required fields validated
- [ ] Invalid data handled

### Performance
- [ ] Simple queries < 100ms
- [ ] Single API call queries < 500ms
- [ ] Multi-step queries < 1500ms
- [ ] Cache improves performance significantly

---

## Summary

This test suite covers:
- ✅ 4 different template types (simple → complex)
- ✅ 0 to 3 external API calls
- ✅ 4 different matching strategies
- ✅ Conditional chaining (nextCalls)
- ✅ Variable dependency between API calls
- ✅ Eligibility rule evaluation
- ✅ Cache functionality
- ✅ Pagination and HATEOAS
- ✅ Error handling
- ✅ Performance benchmarks

**Total Test Cases**: 20+
**Coverage**: All major POC features
**Ready for**: Demo and validation
