# Document Hub Service

A reactive Spring WebFlux microservice for document enquiry with advanced custom rule engine capabilities.

## Overview

This service provides the `/documents-enquiry` endpoint that retrieves documents based on:
- **Account-specific documents** from the storage_index table
- **Shared documents** based on eligibility rules including:
  - Basic scopes (all, credit_card_account_only, digital_bank_customer_only, enterprise_customer_only)
  - **Custom rules** with dynamic data extraction and complex eligibility criteria

## Features

- ✅ Reactive architecture using Spring WebFlux and R2DBC
- ✅ Advanced custom rule engine with 8 rule types
- ✅ 13 operators for flexible criteria evaluation
- ✅ Redis caching for performance optimization
- ✅ Circuit breaker pattern for resilience
- ✅ Comprehensive error handling
- ✅ HATEOAS pagination support
- ✅ Metrics and monitoring with Micrometer

## Technology Stack

- Java 17
- Spring Boot 3.2.1
- Spring WebFlux (Reactive)
- Spring Data R2DBC (Reactive Database)
- PostgreSQL with R2DBC driver
- Redis (Caching)
- Resilience4j (Circuit Breaker)
- Micrometer (Metrics)
- Lombok

## Architecture

```
DocumentEnquiryController
    └── DocumentEnquiryService
            ├── StorageIndexRepository (Account-specific documents)
            └── SharedDocumentEligibilityService
                    ├── BasicScopeEvaluator
                    └── CustomRuleEngine
                            ├── DataExtractionOrchestrator
                            └── RuleEvaluator (13 operators)
```

## Custom Rule Engine

### Supported Rule Types

1. **location_based** - Filter by customer zipcode/location
2. **tenure_based** - Filter by customer relationship duration
3. **balance_based** - Filter by account balance thresholds
4. **credit_limit_based** - Filter by credit limit
5. **transaction_pattern_based** - Filter by transaction history
6. **customer_segment_based** - Filter by customer tier/segment
7. **product_type_based** - Filter by product type
8. **composite** - Combine multiple rules with AND/OR logic

### Supported Operators

- `in` / `notIn` - Check if value is in/not in a list
- `equals` / `notEquals` - Exact match
- `lessThan` / `lessThanOrEqual` - Numeric/date comparison
- `greaterThan` / `greaterThanOrEqual` - Numeric/date comparison
- `between` - Range check
- `contains` / `notContains` - String/array contains
- `startsWith` / `endsWith` - String pattern matching
- `matches` - Regex pattern matching

## Example Rule Configurations

### 1. Location-Based Rule (Zipcode Filter)

```json
{
  "ruleType": "location_based",
  "extractionStrategy": [{
    "id": "getCustomerAddress",
    "endpoint": {
      "url": "/customer-service/customers/${$input.customerId}/profile",
      "method": "GET",
      "timeout": 3000
    },
    "responseMapping": {
      "extract": {
        "zipcode": "$.primaryAddress.postalCode"
      }
    }
  }],
  "eligibilityCriteria": {
    "zipcode": {
      "operator": "in",
      "values": ["90001", "90002", "90210"],
      "dataType": "string"
    }
  }
}
```

### 2. Tenure-Based Rule (> 5 years)

```json
{
  "ruleType": "tenure_based",
  "extractionStrategy": [{
    "id": "getCustomerTenure",
    "endpoint": {
      "url": "/customer-service/customers/${$input.customerId}/profile",
      "method": "GET"
    },
    "responseMapping": {
      "extract": {
        "customerSinceDate": "$.customerSince"
      }
    }
  }],
  "eligibilityCriteria": {
    "tenureYears": {
      "operator": "greaterThan",
      "value": 5,
      "dataType": "duration",
      "unit": "years",
      "compareField": "customerSinceDate"
    }
  }
}
```

### 3. Balance-Based Rule (< $5000)

```json
{
  "ruleType": "balance_based",
  "extractionStrategy": [{
    "id": "getAccountBalance",
    "endpoint": {
      "url": "/accounts-service/accounts/${$input.accountId}/balance",
      "method": "GET"
    },
    "responseMapping": {
      "extract": {
        "currentBalance": "$.currentBalance"
      }
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
```

### 4. Composite Rule (Balance + Tenure)

```json
{
  "ruleType": "composite",
  "logicOperator": "AND",
  "rules": [
    {
      "ruleType": "balance_based",
      "extractionStrategy": [{
        "id": "getBalance",
        "endpoint": {
          "url": "/accounts-service/accounts/${$input.accountId}/balance"
        },
        "responseMapping": {
          "extract": {"currentBalance": "$.currentBalance"}
        }
      }],
      "eligibilityCriteria": {
        "currentBalance": {
          "operator": "between",
          "minValue": 1000,
          "maxValue": 10000,
          "dataType": "number"
        }
      }
    },
    {
      "ruleType": "tenure_based",
      "extractionStrategy": [{
        "id": "getTenure",
        "endpoint": {
          "url": "/customer-service/customers/${$input.customerId}/profile"
        },
        "responseMapping": {
          "extract": {"customerSinceDate": "$.customerSince"}
        }
      }],
      "eligibilityCriteria": {
        "tenureYears": {
          "operator": "greaterThan",
          "value": 2,
          "dataType": "duration",
          "unit": "years",
          "compareField": "customerSinceDate"
        }
      }
    }
  ]
}
```

## API Usage

### POST /documents-enquiry

**Headers:**
```
X-version: 1
X-correlation-id: uuid
X-requestor-id: uuid
X-requestor-type: CUSTOMER|AGENT|SYSTEM
Content-Type: application/json
```

**Request Body:**
```json
{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountId": ["3fa85f64-5717-4562-b3fc-2c963f66afa6"],
  "postedFromDate": 1704067200,
  "postedToDate": 1735689600,
  "pageNumber": 1,
  "pageSize": 20,
  "sortOrder": [
    {
      "orderBy": "creationDate",
      "sortBy": "desc"
    }
  ]
}
```

**Response:**
```json
{
  "documentList": [
    {
      "documentId": "encoded-id",
      "sizeInMb": 1,
      "languageCode": "EN_US",
      "displayName": "Monthly Statement",
      "mimeType": "application/pdf",
      "description": "January 2024 Statement",
      "lineOfBusiness": ["CREDIT_CARD"],
      "category": "Statement",
      "documentType": "monthly_statement",
      "datePosted": 1704067200,
      "metadata": [
        {"key": "accountId", "value": "...", "dataType": "STRING"}
      ],
      "_links": {
        "download": {
          "href": "/documents/encoded-id",
          "type": "GET",
          "rel": "download"
        }
      }
    }
  ],
  "pagination": {
    "pageNumber": 1,
    "pageSize": 20,
    "totalItems": 45,
    "totalPages": 3
  },
  "_links": {
    "self": {"href": "/documents-enquiry", "type": "POST"},
    "next": {"href": "/documents-enquiry", "type": "POST"},
    "prev": null
  }
}
```

## Configuration

### Database Configuration

Update `application.yml`:
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/documenthub
    username: your_username
    password: your_password
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### External Service URLs

```yaml
integration:
  customer-service:
    base-url: http://customer-service:8081
    timeout: 3000
  account-service:
    base-url: http://account-service:8082
    timeout: 3000
```

## Building and Running

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

### Run with Docker
```bash
docker build -t document-hub-service .
docker run -p 8080:8080 document-hub-service
```

## Database Schema

### storage_index table
- Stores actual document records
- Linked to accounts and customers
- Contains metadata and storage references

### master_template_definition table
- Stores document template definitions
- Contains sharing configuration (isSharedDocument, sharingScope)
- Contains custom rule definitions (data_extraction_schema)

## Caching Strategy

| Cache Name | TTL | Description |
|------------|-----|-------------|
| customerProfile | 30 min | Customer profile data |
| customerSegment | 60 min | Customer segment/tier |
| accountDetails | 60 min | Account metadata |
| accountBalance | 5 min | Account balance (volatile) |
| accountArrangements | 60 min | Account arrangements |
| transactionSummary | 15 min | Transaction summaries |

## Monitoring

### Health Check
```
GET /actuator/health
```

### Metrics
```
GET /actuator/metrics
GET /actuator/prometheus
```

## Error Handling

The service returns standard error responses:

```json
{
  "errorMsg": ["Error description"],
  "statusCode": 400,
  "timestamp": "1704067200"
}
```

**Status Codes:**
- 200: Success
- 400: Bad Request (validation errors)
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 503: Service Unavailable

## Performance Considerations

1. **Caching**: Redis caching reduces external API calls
2. **Parallel Execution**: Independent rules execute in parallel
3. **Circuit Breaker**: Prevents cascading failures
4. **Connection Pooling**: R2DBC and Redis connection pools
5. **Reactive Streams**: Non-blocking I/O for high throughput

## Testing

### Run Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

## Contributing

1. Create feature branch
2. Write tests
3. Ensure all tests pass
4. Submit pull request

## License

Copyright © 2024. All rights reserved.
