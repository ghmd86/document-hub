# Mock Services for Document Hub Testing

This is a standalone Spring Boot application that provides mock implementations of external services required by the Document Hub Service.

## Purpose

The mock services allow you to:
- Test the Document Hub Service without external dependencies
- Test custom rule engine with realistic data
- Develop and debug locally
- Run integration tests

## Mock Services Included

### 1. Customer Service Mock
**Base Path:** `/customer-service/customers`

**Endpoints:**
- `GET /{customerId}/profile` - Returns customer profile with address and tenure
- `GET /{customerId}/segment` - Returns customer segment and tier
- `GET /{customerId}/address` - Returns customer address

### 2. Account Service Mock
**Base Paths:** `/accounts-service`, `/creditcard`

**Endpoints:**
- `GET /accounts-service/accounts/{accountId}` - Returns account details
- `GET /accounts-service/accounts/{accountId}/balance` - Returns balance info
- `GET /creditcard/accounts/{accountId}/arrangements` - Returns arrangements
- `GET /creditcard/accounts/{accountId}/product` - Returns product info

### 3. Transaction Service Mock
**Base Path:** `/transaction-service/accounts`

**Endpoints:**
- `GET /{accountId}/transactions/summary` - Returns transaction summary
- `GET /{accountId}/transactions/recent` - Returns recent transactions

## Mock Data

### Customers

**Customer 1: John Doe**
- ID: `880e8400-e29b-41d4-a716-446655440001`
- Type: `credit_card`
- Segment: `premium`
- Tier: `GOLD`
- Tenure: **8 years** (exceeds 5 year rule ✓)
- Location: Los Angeles, CA - Zipcode `90001` (in CA rule ✓)
- Accounts: 2 (Account 1 and Account 2)

**Customer 2: Jane Smith**
- ID: `880e8400-e29b-41d4-a716-446655440002`
- Type: `digital_banking` ✓
- Segment: `standard`
- Tier: `SILVER`
- Tenure: **2 years** (does not exceed 5 year rule ✗)
- Location: New York, NY - Zipcode `10001`
- Accounts: 1 (Account 3)

**Customer 3: Bob Johnson**
- ID: `880e8400-e29b-41d4-a716-446655440003`
- Type: `credit_card`
- Segment: `enterprise` ✓
- Tier: `PLATINUM`
- VIP: `true`
- Tenure: **10 years** (exceeds 5 year rule ✓)
- Location: San Francisco, CA - Zipcode `94102` (in CA rule ✓)

### Accounts

**Account 1: John's Low Balance Card**
- ID: `770e8400-e29b-41d4-a716-446655440001`
- Customer: Customer 1 (John)
- Balance: **$4,500** (< $5000 rule ✓)
- Credit Limit: $10,000
- Product: REWARDS_PLUS
- Product Family: CASHBACK_CARDS
- Has International Transactions: **Yes** ✓

**Account 2: John's High Balance Card**
- ID: `770e8400-e29b-41d4-a716-446655440002`
- Customer: Customer 1 (John)
- Balance: **$12,500** (> $5000 rule ✗)
- Credit Limit: $25,000
- Product: PREMIUM_PLATINUM
- Product Family: PREMIUM_CARDS
- Has International Transactions: No

**Account 3: Jane's Medium Balance Card**
- ID: `770e8400-e29b-41d4-a716-446655440003`
- Customer: Customer 2 (Jane)
- Balance: **$8,200** (> $5000 rule ✗)
- Credit Limit: $15,000
- Product: TRAVEL_REWARDS
- Product Family: REWARDS_CARDS
- Has International Transactions: **Yes** ✓

## Testing Custom Rules

### Rule 1: Location-Based (Zipcode in [90001, 90002, 94102])

**Will match:**
- Customer 1 (John) - Zipcode 90001 ✓
- Customer 3 (Bob) - Zipcode 94102 ✓

**Won't match:**
- Customer 2 (Jane) - Zipcode 10001 ✗

### Rule 2: Tenure-Based (> 5 years)

**Will match:**
- Customer 1 (John) - 8 years ✓
- Customer 3 (Bob) - 10 years ✓

**Won't match:**
- Customer 2 (Jane) - 2 years ✗

### Rule 3: Balance-Based (< $5000)

**Will match:**
- Account 1 (John's first account) - $4,500 ✓

**Won't match:**
- Account 2 (John's second account) - $12,500 ✗
- Account 3 (Jane's account) - $8,200 ✗

### Rule 4: Credit Limit-Based (>= $25000)

**Will match:**
- Account 2 (John's second account) - $25,000 ✓

**Won't match:**
- Account 1 - $10,000 ✗
- Account 3 - $15,000 ✗

### Rule 5: Transaction Pattern (Has International Activity)

**Will match:**
- Account 1 (John's first) ✓
- Account 3 (Jane's) ✓

**Won't match:**
- Account 2 (John's second) ✗

### Rule 6: Customer Segment-Based

**Premium/Gold:**
- Customer 1 (John) ✓

**Enterprise:**
- Customer 3 (Bob) ✓

**Digital Banking:**
- Customer 2 (Jane) ✓

### Rule 7: Product Type-Based

**CASHBACK_CARDS:**
- Account 1 ✓

**PREMIUM_CARDS:**
- Account 2 ✓

**REWARDS_CARDS:**
- Account 3 ✓

### Composite Rule Examples

**Example 1: Balance < $5000 AND Tenure > 5 years**
- Customer 1 + Account 1: **MATCH** ✓ (balance $4,500 AND 8 years)
- Customer 1 + Account 2: **NO MATCH** ✗ (balance $12,500)
- Customer 2 + Account 3: **NO MATCH** ✗ (tenure only 2 years)

**Example 2: Balance BETWEEN $1000-$10000 AND Tenure > 2 years**
- Customer 1 + Account 1: **MATCH** ✓ (balance $4,500, tenure 8 years)
- Customer 1 + Account 2: **NO MATCH** ✗ (balance $12,500)
- Customer 2 + Account 3: **NO MATCH** ✗ (balance $8,200 but tenure only 2 years)

## Running the Mock Services

### Option 1: Using Maven
```bash
cd mock-services
mvn spring-boot:run
```

### Option 2: Using JAR
```bash
cd mock-services
mvn clean package
java -jar target/mock-services-1.0.0-SNAPSHOT.jar
```

### Option 3: Using IDE
1. Open `MockServicesApplication.java`
2. Run the main method

The service will start on **port 8090**.

## Testing the Mock APIs

### Test Customer Service
```bash
# Get customer profile (Customer 1 - John, 8 years tenure, CA)
curl http://localhost:8090/customer-service/customers/880e8400-e29b-41d4-a716-446655440001/profile

# Get customer segment
curl http://localhost:8090/customer-service/customers/880e8400-e29b-41d4-a716-446655440001/segment

# Get customer address
curl http://localhost:8090/customer-service/customers/880e8400-e29b-41d4-a716-446655440001/address
```

### Test Account Service
```bash
# Get account details (Account 1 - Low balance $4,500)
curl http://localhost:8090/accounts-service/accounts/770e8400-e29b-41d4-a716-446655440001

# Get account balance
curl http://localhost:8090/accounts-service/accounts/770e8400-e29b-41d4-a716-446655440001/balance

# Get account arrangements
curl http://localhost:8090/creditcard/accounts/770e8400-e29b-41d4-a716-446655440001/arrangements

# Get account product
curl http://localhost:8090/creditcard/accounts/770e8400-e29b-41d4-a716-446655440001/product
```

### Test Transaction Service
```bash
# Get transaction summary
curl "http://localhost:8090/transaction-service/accounts/770e8400-e29b-41d4-a716-446655440001/transactions/summary?period=last_90_days"

# Get international transactions
curl "http://localhost:8090/transaction-service/accounts/770e8400-e29b-41d4-a716-446655440001/transactions/summary?period=last_90_days&type=international"

# Get recent transactions
curl http://localhost:8090/transaction-service/accounts/770e8400-e29b-41d4-a716-446655440001/transactions/recent
```

## Using with Document Hub Service

### Update Configuration
The Document Hub Service has a pre-configured `application-mock.yml` profile.

### Run with Mock Profile
```bash
cd ../  # Back to document-hub-service directory

# Run with mock profile
mvn spring-boot:run -Dspring-boot.run.profiles=mock

# Or set environment variable
export SPRING_PROFILES_ACTIVE=mock
mvn spring-boot:run
```

### Verify Integration
```bash
# Test document enquiry with Customer 1 (should trigger custom rules)
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-001" \
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{
    "customerId": "880e8400-e29b-41d4-a716-446655440001",
    "accountId": ["770e8400-e29b-41d4-a716-446655440001"],
    "pageNumber": 1,
    "pageSize": 20
  }'
```

## Health Check

```bash
curl http://localhost:8090/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Logs

The mock services provide detailed logging:
- Request logging for all endpoints
- Response data logging
- Customer/Account IDs being accessed

Look for logs prefixed with `[MOCK-API]`.

## Adding More Mock Data

To add more customers or accounts, edit the static initialization blocks in:
- `CustomerServiceController.java`
- `AccountServiceController.java`

## Port Configuration

Default port: **8090**

To change, edit `mock-services/src/main/resources/application.yml`:
```yaml
server:
  port: 8090  # Change this
```

## Summary

**Total Endpoints:** 10
- Customer Service: 3
- Account Service: 4
- Transaction Service: 2
- Health: 1

**Mock Customers:** 3
**Mock Accounts:** 3

**Rule Coverage:**
- ✓ Location-based rules
- ✓ Tenure-based rules
- ✓ Balance-based rules
- ✓ Credit limit rules
- ✓ Transaction pattern rules
- ✓ Customer segment rules
- ✓ Product type rules
- ✓ Composite rules

All custom rule scenarios in the sample data are testable with this mock service!
