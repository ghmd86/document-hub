# Mock Services Guide

## Overview

Mock Services is a standalone Spring Boot application that simulates external APIs required by the Document Hub Service. It allows complete end-to-end testing of custom rules without external dependencies.

---

## 🎯 Quick Start

### Step 1: Start Mock Services

```bash
cd mock-services
mvn spring-boot:run
```

Mock services will start on **port 8090**.

### Step 2: Start Document Hub Service with Mock Profile

```bash
cd ..  # Back to document-hub-service
mvn spring-boot:run -Dspring-boot.run.profiles=mock
```

Document Hub Service will start on **port 8080** and connect to mock services.

### Step 3: Run Tests

```bash
# Run the comprehensive test suite
test-with-mocks.bat

# Or test individual scenarios
curl -X POST http://localhost:8080/documents-enquiry ...
```

---

## 📊 Mock Data Summary

### 3 Customers with Different Profiles

| Customer | ID | Type | Tenure | Location | Segment | Accounts |
|----------|-----|------|--------|----------|---------|----------|
| **John Doe** | ...440001 | Credit Card | **8 years** | CA (90001) | Premium/GOLD | 2 |
| **Jane Smith** | ...440002 | **Digital Banking** | 2 years | NY (10001) | Standard/SILVER | 1 |
| **Bob Johnson** | ...440003 | Credit Card | **10 years** | CA (94102) | **Enterprise**/PLATINUM | 0* |

*Bob is available for custom rule testing but has no sample documents

### 3 Accounts with Different Characteristics

| Account | Customer | Balance | Credit Limit | Product Family | Intl Txn |
|---------|----------|---------|--------------|----------------|----------|
| **Account 1** | John | **$4,500** | $10,000 | CASHBACK_CARDS | ✓ Yes |
| **Account 2** | John | $12,500 | **$25,000** | PREMIUM_CARDS | No |
| **Account 3** | Jane | $8,200 | $15,000 | REWARDS_CARDS | ✓ Yes |

---

## 🧪 Test Scenarios

### Scenario 1: Low Balance Alert (balance < $5000)

**Rule:** `balance_based` with `lessThan 5000`

**Test with:**
```bash
Customer: 880e8400-e29b-41d4-a716-446655440001 (John)
Account:  770e8400-e29b-41d4-a716-446655440001 (Account 1)
Balance:  $4,500
Result:   ✓ SHOULD MATCH
```

**Should NOT match:**
- Account 2 (balance $12,500)
- Account 3 (balance $8,200)

### Scenario 2: Loyal Customer (tenure > 5 years)

**Rule:** `tenure_based` with `greaterThan 5`

**Test with:**
```bash
Customer: 880e8400-e29b-41d4-a716-446655440001 (John - 8 years)
Result:   ✓ SHOULD MATCH

Customer: 880e8400-e29b-41d4-a716-446655440003 (Bob - 10 years)
Result:   ✓ SHOULD MATCH
```

**Should NOT match:**
- Customer 2 (Jane - 2 years)

### Scenario 3: Location-Based (CA zipcodes)

**Rule:** `location_based` with zipcodes `[90001, 90002, 94102]`

**Test with:**
```bash
Customer: 880e8400-e29b-41d4-a716-446655440001 (John - 90001)
Result:   ✓ SHOULD MATCH

Customer: 880e8400-e29b-41d4-a716-446655440003 (Bob - 94102)
Result:   ✓ SHOULD MATCH
```

**Should NOT match:**
- Customer 2 (Jane - 10001 NY)

### Scenario 4: Digital Banking Customer

**Rule:** `sharing_scope = digital_bank_customer_only`

**Test with:**
```bash
Customer: 880e8400-e29b-41d4-a716-446655440002 (Jane)
Type:     digital_banking
Result:   ✓ SHOULD MATCH
```

**Should NOT match:**
- Customer 1 (John - credit_card type)
- Customer 3 (Bob - credit_card type)

### Scenario 5: Credit Card Account Only

**Rule:** `sharing_scope = credit_card_account_only`

**Test with:**
```bash
All accounts are credit card accounts
Result:   ✓ ALL SHOULD MATCH
```

### Scenario 6: Composite Rule (balance < $5000 AND tenure > 5 years)

**Rule:** `composite` with `AND` logic

**Test with:**
```bash
Customer: 880e8400-e29b-41d4-a716-446655440001 (John)
Account:  770e8400-e29b-41d4-a716-446655440001 (Account 1)
Balance:  $4,500 ✓
Tenure:   8 years ✓
Result:   ✓ SHOULD MATCH (both conditions met)
```

**Should NOT match:**
- John + Account 2 (balance $12,500 - fails balance rule)
- Jane + Account 3 (tenure 2 years - fails tenure rule)

---

## 📡 Mock API Endpoints

### Customer Service (Port 8090)

```bash
# Get full customer profile (includes address, tenure, segment)
GET http://localhost:8090/customer-service/customers/{customerId}/profile

# Get customer segment only
GET http://localhost:8090/customer-service/customers/{customerId}/segment

# Get customer address only
GET http://localhost:8090/customer-service/customers/{customerId}/address
```

**Example Response:**
```json
{
  "customerId": "880e8400-e29b-41d4-a716-446655440001",
  "firstName": "John",
  "lastName": "Doe",
  "customerType": "credit_card",
  "segment": "premium",
  "tier": "GOLD",
  "customerSince": 1575158400,
  "primaryAddress": {
    "postalCode": "90001",
    "city": "Los Angeles",
    "state": "CA"
  }
}
```

### Account Service (Port 8090)

```bash
# Get account details
GET http://localhost:8090/accounts-service/accounts/{accountId}

# Get account balance
GET http://localhost:8090/accounts-service/accounts/{accountId}/balance

# Get account arrangements
GET http://localhost:8090/creditcard/accounts/{accountId}/arrangements

# Get account product
GET http://localhost:8090/creditcard/accounts/{accountId}/product
```

**Example Balance Response:**
```json
{
  "accountId": "770e8400-e29b-41d4-a716-446655440001",
  "currentBalance": 4500.00,
  "availableBalance": 5500.00,
  "creditLimit": 10000.00
}
```

### Transaction Service (Port 8090)

```bash
# Get transaction summary
GET http://localhost:8090/transaction-service/accounts/{accountId}/transactions/summary?period=last_90_days

# Get international transactions only
GET http://localhost:8090/transaction-service/accounts/{accountId}/transactions/summary?type=international

# Get recent transactions
GET http://localhost:8090/transaction-service/accounts/{accountId}/transactions/recent?limit=10
```

---

## 🔍 Testing Custom Rules End-to-End

### Test Setup

1. **Start Infrastructure**
   ```bash
   docker-compose up -d  # Starts PostgreSQL + Redis
   ```

2. **Start Mock Services**
   ```bash
   cd mock-services
   mvn spring-boot:run
   ```

3. **Start Document Hub Service (with mock profile)**
   ```bash
   cd ..
   mvn spring-boot:run -Dspring-boot.run.profiles=mock
   ```

### Example Test: Low Balance Alert

**Shared Document Template in Database:**
```json
{
  "templateName": "Low Balance Alert Notice",
  "sharingScope": "custom_rule",
  "dataExtractionSchema": {
    "ruleType": "balance_based",
    "extractionStrategy": [{
      "id": "getBalance",
      "endpoint": {
        "url": "/accounts-service/accounts/${$input.accountId}/balance",
        "method": "GET"
      },
      "responseMapping": {
        "extract": {"currentBalance": "$.currentBalance"}
      }
    }],
    "eligibilityCriteria": {
      "currentBalance": {
        "operator": "lessThan",
        "value": 5000,
        "dataType": "number"
      }
    }
  }
}
```

**Test Request:**
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-001" \
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{
    "customerId": "880e8400-e29b-41d4-a716-446655440001",
    "accountId": ["770e8400-e29b-41d4-a716-446655440001"]
  }'
```

**What Happens:**
1. Document Hub Service calls mock service: `GET /accounts-service/accounts/.../balance`
2. Mock service returns: `{"currentBalance": 4500.00}`
3. Rule evaluator checks: `4500 < 5000` → **TRUE**
4. Low Balance Alert document is included in response ✓

**Check Logs:**
```
[CustomRuleEngine] Executing data source: getBalance
[CustomRuleEngine] Extracted field currentBalance: 4500.0
[RuleEvaluator] Evaluating rule: currentBalance lessThan 5000
[RuleEvaluator] Rule evaluation result: true
[SharedDocumentEligibilityService] Custom rule evaluation result: true
```

---

## 📝 Test Matrix

| Customer | Account | Balance | Tenure | Location | Low Balance Alert | Loyalty Rewards | Digital Banking Guide |
|----------|---------|---------|--------|----------|-------------------|------------------|-----------------------|
| John | Acct 1 | $4,500 | 8 yrs | CA | ✓ Yes | ✓ Yes | No |
| John | Acct 2 | $12,500 | 8 yrs | CA | No | ✓ Yes | No |
| Jane | Acct 3 | $8,200 | 2 yrs | NY | No | No | ✓ Yes |
| Bob | N/A | N/A | 10 yrs | CA | N/A | ✓ Yes | No |

---

## 🚀 Running the Complete Test Suite

```bash
# Automated test script
test-with-mocks.bat

# Or manual tests
cd mock-services
start-mock.bat  # Terminal 1

cd ..
mvn spring-boot:run -Dspring-boot.run.profiles=mock  # Terminal 2

# Then run individual curl commands
```

---

## 🔧 Troubleshooting

### Mock Services Not Starting

**Check port 8090 availability:**
```bash
netstat -ano | findstr :8090
```

**Change port if needed:**
Edit `mock-services/src/main/resources/application.yml`

### Document Hub Service Can't Connect

**Verify mock services are running:**
```bash
curl http://localhost:8090/actuator/health
```

**Check profile is active:**
Look for `[mock]` in startup logs

### Rules Not Evaluating

**Check logs for:**
- `CustomRuleEngine` - API calls
- `RuleEvaluator` - Rule evaluation
- `SharedDocumentEligibilityService` - Eligibility decisions

**Enable debug logging:**
```yaml
logging:
  level:
    com.documenthub.rules: TRACE
```

---

## 📚 Key Files

| File | Purpose |
|------|---------|
| `mock-services/pom.xml` | Maven configuration |
| `mock-services/src/main/java/.../MockServicesApplication.java` | Main application |
| `mock-services/src/main/java/.../CustomerServiceController.java` | Customer API mock |
| `mock-services/src/main/java/.../AccountServiceController.java` | Account API mock |
| `mock-services/src/main/java/.../TransactionServiceController.java` | Transaction API mock |
| `mock-services/README.md` | Detailed mock service documentation |
| `application-mock.yml` | Document Hub config for mocks |
| `test-with-mocks.bat` | Automated test suite |

---

## ✅ Summary

**Mock Services Provide:**
- ✓ 3 Customers with realistic profiles
- ✓ 3 Accounts with varying balances and attributes
- ✓ 10 API endpoints covering all integrations
- ✓ Complete coverage of all rule types
- ✓ Realistic data for testing custom rules
- ✓ No external dependencies required

**Test Coverage:**
- ✓ Location-based rules
- ✓ Tenure-based rules
- ✓ Balance-based rules
- ✓ Credit limit rules
- ✓ Transaction pattern rules
- ✓ Customer segment rules
- ✓ Product type rules
- ✓ Composite rules (AND/OR)

**Ready to use for:**
- Local development
- Integration testing
- Rule engine validation
- Custom rule development
- Demo purposes

All custom rule scenarios can be tested without any external services! 🎉
