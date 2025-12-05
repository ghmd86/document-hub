# Document Hub API Reference

## Required Headers

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Content-Type | string | Yes | Must be `application/json` |
| X-version | string | Yes | API version (e.g., `1`) |
| X-correlation-id | string | Yes | Unique request correlation ID |
| X-requestor-id | UUID | Yes | Requestor identifier |
| X-requestor-type | enum | Yes | One of: `CUSTOMER`, `BANKER`, etc. |

## Base URL
```
http://localhost:8080/api/v1
```

## Document Enquiry Endpoint

### POST /documents-enquiry

**Full curl command:**
```bash
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: <unique-id>" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"<customer-id>\", \"accountId\": [\"<account-id>\"]}"
```

## Test Accounts

| Account ID | Credit Limit | Expected Tier | Expected Document |
|------------|--------------|---------------|-------------------|
| aaaa0000-0000-0000-0000-000000000001 | $75,000 | PLATINUM | Platinum_Card_Agreement.pdf |
| aaaa0000-0000-0000-0000-000000000002 | $35,000 | GOLD | Gold_Card_Agreement.pdf |
| aaaa0000-0000-0000-0000-000000000003 | $15,000 | STANDARD | Standard_Card_Agreement.pdf |

## Disclosure Code Mapping (CardholderAgreement)

| Account | Disclosure Code | Document |
|---------|-----------------|----------|
| Account 1 (VIP) | D164 | Credit_Card_Terms_D164_v1.pdf |
| Account 2 | D166 | Premium_Credit_Card_Terms_D166_v1.pdf |
| Account 3 | D165 | Credit_Card_Terms_D165_v1.pdf |

## Available Test Scripts

1. **restart-app.bat** - Kill and restart the Spring Boot application
2. **test-conditional-platinum.bat** - Test Platinum tier document
3. **test-conditional-gold.bat** - Test Gold tier document
4. **test-conditional-standard.bat** - Test Standard tier document
5. **test-disclosure-code-d164.bat** - Test disclosure code D164
6. **test-disclosure-code-d166.bat** - Test disclosure code D166
7. **test-mock-credit-info.bat** - Test credit info mock API directly
8. **run-all-tests.bat** - Run all tests
