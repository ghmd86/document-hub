# Document Hub POC

A Proof of Concept for a dynamic rule-based document retrieval system that supports both account-specific and shared documents with configurable access control rules.

## Features

- ✅ **Account-Specific Documents**: Retrieve documents tied to specific accounts
- ✅ **Shared Documents**: Three sharing strategies:
  - `ALL`: Shared with all customers
  - `ACCOUNT_TYPE`: Shared based on account type (credit_card, digital_bank, etc.)
  - `CUSTOM_RULES`: Shared based on custom JSON-defined rules
- ✅ **Dynamic Rule Evaluation**: Flexible rule engine supporting AND/OR logic
- ✅ **Reactive Programming**: Built with Spring WebFlux (R2DBC)
- ✅ **Configurable for Non-Technical Users**: Rules defined in JSON format

## Architecture

```
Request → Controller → Service → Repository → PostgreSQL (R2DBC)
                          ↓
              RuleEvaluationService
                          ↓
            AccountMetadataService (Mock)
```

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL 12+

## Quick Start

### 1. Setup Database

```bash
# Create database
createdb document_hub

# Run schema
psql -d document_hub -f src/main/resources/database.sql

# Load sample data
psql -d document_hub -f src/main/resources/data.sql
```

### 2. Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/document_hub
spring.r2dbc.username=postgres
spring.r2dbc.password=postgres
```

### 3. Build and Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access Swagger UI

Open: `http://localhost:8080/swagger-ui.html`

## API Usage

### Endpoint: POST /api/v1/documents-enquiry

**Headers:**
- `X-version`: 1
- `X-correlation-id`: any-correlation-id
- `X-requestor-id`: UUID
- `X-requestor-type`: CUSTOMER | AGENT | SYSTEM

**Request Body:**
```json
{
  "customerId": "cccc0000-0000-0000-0000-000000000001",
  "accountId": ["aaaa0000-0000-0000-0000-000000000001"],
  "pageNumber": 0,
  "pageSize": 20
}
```

## Test Scenarios

### Scenario 1: VIP Customer in US_WEST (Account 1)

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-123" \
  -H "X-requestor-id: 12345678-1234-1234-1234-123456789012" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{
    "customerId": "cccc0000-0000-0000-0000-000000000001",
    "accountId": ["aaaa0000-0000-0000-0000-000000000001"],
    "pageNumber": 0,
    "pageSize": 20
  }'
```

**Expected Documents:**
1. ✅ Privacy Policy (shared with ALL)
2. ✅ Account statement (account-specific)
3. ✅ Balance Transfer Offer (shared by account type: credit_card)
4. ✅ VIP Exclusive Offer (custom rules: VIP + US_WEST)

### Scenario 2: Standard Customer in US_WEST (Account 2)

**Request:**
```json
{
  "customerId": "cccc0000-0000-0000-0000-000000000001",
  "accountId": ["aaaa0000-0000-0000-0000-000000000002"]
}
```

**Expected Documents:**
1. ✅ Privacy Policy (shared with ALL)
2. ✅ Account statement for Account 2 (account-specific)
3. ✅ Balance Transfer Offer (shared by account type: credit_card)
4. ❌ VIP Exclusive Offer (NOT accessible - customer segment is STANDARD)

### Scenario 3: Digital Bank Customer in US_EAST (Account 3)

**Request:**
```json
{
  "customerId": "cccc0000-0000-0000-0000-000000000002",
  "accountId": ["aaaa0000-0000-0000-0000-000000000003"]
}
```

**Expected Documents:**
1. ✅ Privacy Policy (shared with ALL)
2. ❌ Credit card statements (not for this account type)
3. ❌ Balance Transfer Offer (account type: digital_bank, not credit_card)
4. ❌ VIP Exclusive Offer (segment: ENTERPRISE, not VIP)

## Rule Configuration

### Example: VIP Customers in US_WEST

```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "customerSegment",
        "operator": "EQUALS",
        "value": "VIP"
      },
      {
        "field": "region",
        "operator": "EQUALS",
        "value": "US_WEST"
      }
    ]
  }
}
```

### Example: Credit Card Customers

```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "accountType",
        "operator": "IN",
        "value": ["credit_card"]
      }
    ]
  }
}
```

### Supported Operators

- `EQUALS` / `NOT_EQUALS`
- `IN` / `NOT_IN`
- `GREATER_THAN` / `LESS_THAN`
- `GREATER_THAN_OR_EQUAL` / `LESS_THAN_OR_EQUAL`
- `CONTAINS` / `STARTS_WITH` / `ENDS_WITH`

### Supported Fields

From Account Metadata:
- `accountType`
- `region`
- `state`
- `customerSegment`
- `lineOfBusiness`
- `accountId`
- `customerId`

From Request Context:
- `$metadata.<field>` - Document metadata fields
- `$request.<field>` - Request fields (e.g., referenceKey)

## Project Structure

```
fresh-doc-hub-poc/
├── src/main/java/com/documenthub/
│   ├── DocumentHubApplication.java       # Main Spring Boot app
│   ├── controller/
│   │   └── DocumentEnquiryController.java  # REST API controller
│   ├── service/
│   │   ├── AccountMetadataService.java     # Mock account data service
│   │   ├── RuleEvaluationService.java      # Rule evaluation engine
│   │   └── DocumentEnquiryService.java     # Main business logic
│   ├── repository/
│   │   ├── MasterTemplateRepository.java   # Template repository
│   │   └── StorageIndexRepository.java     # Document repository
│   ├── entity/
│   │   ├── MasterTemplateDefinitionEntity.java
│   │   └── StorageIndexEntity.java
│   └── model/
│       ├── AccountMetadata.java
│       ├── AccessControl.java
│       ├── EligibilityCriteria.java
│       ├── Rule.java
│       └── [Generated OpenAPI models]
├── src/main/resources/
│   ├── application.properties
│   ├── doc-hub.yaml                      # OpenAPI spec
│   ├── database.sql                      # DB schema
│   └── data.sql                          # Sample data
└── pom.xml
```

## Mock Data

The POC includes 4 mock accounts with different configurations:

| Account ID | Customer ID | Type | Segment | Region | Details |
|------------|-------------|------|---------|--------|---------|
| `aaaa...001` | `cccc...001` | credit_card | VIP | US_WEST | 2 years old |
| `aaaa...002` | `cccc...001` | credit_card | STANDARD | US_WEST | 3 months old |
| `aaaa...003` | `cccc...002` | digital_bank | ENTERPRISE | US_EAST | 1 year old |
| `aaaa...004` | `cccc...003` | savings | STANDARD | US_EAST | 1 month old |

## Next Steps

1. **Production Ready Changes:**
   - Replace `AccountMetadataService` mock with real API/database integration
   - Add authentication/authorization
   - Implement caching layer (Redis)
   - Add comprehensive error handling

2. **Enhancement Opportunities:**
   - GUI for rule configuration
   - Document versioning support
   - Advanced filtering (date ranges, categories)
   - Performance optimization (query optimization, indexing)

3. **Testing:**
   - Add integration tests
   - Add unit tests for rule evaluation
   - Performance testing with large datasets

## Troubleshooting

### Database Connection Issues

```bash
# Check PostgreSQL is running
pg_isready

# Verify database exists
psql -l | grep document_hub

# Check connection
psql -d document_hub -c "SELECT version();"
```

### Build Issues

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests
mvn clean install -DskipTests
```

### Logs

Check logs at:
- Console output (DEBUG level enabled)
- Look for `com.documenthub` package logs

## License

POC - Internal Use Only

---

**Built with:**
- Java 17
- Spring Boot 2.7.18
- Spring WebFlux
- R2DBC PostgreSQL
- OpenAPI Generator
- Lombok
