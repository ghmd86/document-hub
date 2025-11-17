# Drools Reactive Eligibility System

A clean, production-ready implementation of a reactive document eligibility system using Drools rule engine with Spring Boot WebFlux.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Quick Start](#quick-start)
5. [Project Structure](#project-structure)
6. [Database Setup](#database-setup)
7. [Configuration](#configuration)
8. [Running the Application](#running-the-application)
9. [API Documentation](#api-documentation)
10. [Testing](#testing)
11. [How It Works](#how-it-works)

---

## Overview

This system determines which documents a customer is eligible to receive based on:
- **Database-driven rules** (stored as JSONB in PostgreSQL)
- **External API data** (fetched reactively and chained)
- **Drools rule engine** (for complex business logic evaluation)

### Key Features

- **Fully Reactive**: Uses Spring WebFlux and R2DBC for non-blocking operations
- **Database-Backed Configuration**: Rules and API configs stored in PostgreSQL JSONB columns
- **Runtime Updates**: Update rules without restarting the application
- **Chained API Calls**: Automatically handles dependent API calls
- **Production-Ready**: Includes caching, error handling, and monitoring

---

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Eligibility Controller (REST API)  │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Eligibility Service (Orchestrator) │
└──────┬──────────────────────────────┘
       │
       ├─────────────────┬──────────────────┐
       ▼                 ▼                  ▼
┌──────────────┐  ┌─────────────┐  ┌──────────────┐
│Config Loader │  │Data Fetcher │  │Rule Evaluator│
│(Database)    │  │(External    │  │(Drools)      │
│              │  │ APIs)       │  │              │
└──────┬───────┘  └──────┬──────┘  └──────┬───────┘
       │                 │                 │
       ▼                 ▼                 ▼
┌──────────────────────────────────────────────┐
│           PostgreSQL Database                │
│  ┌─────────────┐  ┌──────────────────────┐  │
│  │data_sources │  │eligibility_rules     │  │
│  │(JSONB)      │  │(JSONB)               │  │
│  └─────────────┘  └──────────────────────┘  │
└──────────────────────────────────────────────┘
```

---

## Prerequisites

Before you begin, ensure you have:

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 12+**
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code)
- **Git** (optional)

---

## Quick Start

### 1. Clone or Download

```bash
cd drools-reactive-clean
```

### 2. Set Up Database

```bash
# Create PostgreSQL database
psql -U postgres
CREATE DATABASE drools_eligibility;
\q

# Run schema creation
psql -U postgres -d drools_eligibility -f src/main/resources/db/schema.sql

# Load sample data
psql -U postgres -d drools_eligibility -f src/main/resources/db/sample-data.sql
```

### 3. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/drools_eligibility
    username: postgres
    password: your_password
```

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### 5. Test the API

```bash
# Check eligibility
curl http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001
```

---

## Project Structure

```
drools-reactive-clean/
│
├── README.md                          # This file
├── pom.xml                            # Maven dependencies
│
├── src/main/java/com/example/eligibility/
│   │
│   ├── EligibilityApplication.java   # Spring Boot main class
│   │
│   ├── controller/                   # REST API controllers
│   │   └── EligibilityController.java
│   │
│   ├── service/                      # Business logic
│   │   ├── EligibilityService.java           # Main orchestrator
│   │   ├── ConfigurationLoaderService.java   # Loads config from DB
│   │   ├── DataFetcherService.java           # Fetches external API data
│   │   └── RuleEvaluatorService.java         # Evaluates rules with Drools
│   │
│   ├── repository/                   # Database repositories (R2DBC)
│   │   ├── DataSourceRepository.java
│   │   └── EligibilityRuleRepository.java
│   │
│   ├── entity/                       # Database entities
│   │   ├── DataSourceEntity.java
│   │   └── EligibilityRuleEntity.java
│   │
│   ├── model/                        # DTOs and domain models
│   │   ├── EligibilityRequest.java
│   │   ├── EligibilityResponse.java
│   │   ├── DataSourceConfig.java
│   │   ├── RuleConfig.java
│   │   └── RuleFact.java
│   │
│   └── config/                       # Spring configuration
│       ├── DroolsConfig.java
│       ├── WebClientConfig.java
│       └── CacheConfig.java
│
├── src/main/resources/
│   ├── application.yml               # Application configuration
│   ├── logback-spring.xml           # Logging configuration
│   │
│   └── db/                          # Database scripts
│       ├── schema.sql               # Table creation
│       └── sample-data.sql          # Sample rules and data sources
│
└── src/test/                        # Unit and integration tests
    └── java/com/example/eligibility/
        ├── EligibilityServiceTest.java
        └── IntegrationTest.java
```

---

## Database Setup

### Schema Overview

The system uses 2 main tables:

#### 1. `data_sources` - External API Configuration

Stores configuration for external APIs (stored as JSONB).

| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(100) | Unique identifier (e.g., "arrangements_api") |
| name | VARCHAR(255) | Human-readable name |
| type | VARCHAR(50) | Always "REST_API" for now |
| configuration | JSONB | API config (URL, endpoint, mapping, etc.) |
| enabled | BOOLEAN | Is this data source active? |

**Sample JSONB structure:**

```json
{
  "method": "GET",
  "baseUrl": "http://localhost:8081",
  "endpoint": "/api/v1/arrangements/{arrangementId}",
  "timeoutMs": 5000,
  "retryCount": 2,
  "dependsOn": [
    {
      "sourceId": "arrangements_api",
      "field": "pricingId"
    }
  ],
  "responseMapping": [
    {
      "fieldName": "pricingId",
      "jsonPath": "$.pricingId",
      "dataType": "STRING"
    }
  ]
}
```

#### 2. `eligibility_rules` - Business Rules

Stores eligibility rules (stored as JSONB).

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Auto-increment primary key |
| rule_id | VARCHAR(100) | Unique rule identifier |
| document_id | VARCHAR(100) | Document to add if rule matches |
| name | VARCHAR(255) | Rule name |
| description | TEXT | What this rule does |
| priority | INTEGER | Evaluation order (higher = first) |
| enabled | BOOLEAN | Is this rule active? |
| conditions | JSONB | Rule conditions (field comparisons) |

**Sample JSONB conditions:**

```json
{
  "type": "ALL",
  "expressions": [
    {
      "source": "cardholder_agreements_api",
      "field": "cardholderAgreementsTNCCode",
      "operator": "EQUALS",
      "value": "TNC_GOLD_2024"
    },
    {
      "source": "account_service_api",
      "field": "accountBalance",
      "operator": "GREATER_THAN",
      "value": 50000
    }
  ]
}
```

### Creating the Database

```bash
# Step 1: Create database
createdb drools_eligibility

# Step 2: Run schema
psql -d drools_eligibility -f src/main/resources/db/schema.sql

# Step 3: Load sample data
psql -d drools_eligibility -f src/main/resources/db/sample-data.sql

# Step 4: Verify
psql -d drools_eligibility -c "SELECT * FROM data_sources;"
psql -d drools_eligibility -c "SELECT * FROM eligibility_rules;"
```

---

## Configuration

### Application Configuration (`application.yml`)

```yaml
server:
  port: 8080

spring:
  application:
    name: drools-reactive-eligibility

  # Database (R2DBC for reactive)
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/drools_eligibility
    username: postgres
    password: postgres

  # Caching
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=600s

# Drools Configuration
drools:
  rule-evaluation-timeout-ms: 500
  thread-pool-size: 10

# Logging
logging:
  level:
    root: INFO
    com.example.eligibility: DEBUG
    org.springframework.r2dbc: DEBUG
```

---

## Running the Application

### Option 1: Maven

```bash
mvn clean spring-boot:run
```

### Option 2: JAR

```bash
mvn clean package
java -jar target/drools-reactive-clean-1.0.0.jar
```

### Option 3: IDE

1. Open project in IntelliJ IDEA
2. Run `EligibilityApplication.java`

---

## API Documentation

### Check Eligibility

**Endpoint:** `GET /api/v1/eligibility`

**Parameters:**
- `customerId` (required) - Customer identifier
- `accountId` (required) - Account identifier
- `arrangementId` (required) - Arrangement identifier

**Example Request:**

```bash
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001"
```

**Example Response:**

```json
{
  "customerId": "CUST001",
  "accountId": "ACC001",
  "eligibleDocuments": [
    "DOC-TNC-GOLD-2024-BENEFITS",
    "DOC-HIGH-BALANCE-GOLD-EXCLUSIVE"
  ],
  "evaluationTimeMs": 245
}
```

### Health Check

**Endpoint:** `GET /actuator/health`

```bash
curl http://localhost:8080/actuator/health
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Manual Testing

Use the provided sample data to test different scenarios:

```bash
# Scenario 1: Gold TNC customer
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001"

# Scenario 2: High balance customer
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST002&accountId=ACC002&arrangementId=ARR002"
```

---

## How It Works

### Step-by-Step Flow

1. **Client Request**
   - Client calls `/api/v1/eligibility` with customer/account/arrangement IDs

2. **Load Configuration** (`ConfigurationLoaderService`)
   - Loads data source configurations from `data_sources` table (cached)
   - Loads eligibility rules from `eligibility_rules` table (cached)

3. **Fetch External Data** (`DataFetcherService`)
   - Executes API calls based on data source configuration
   - Handles dependencies (chained calls)
   - Maps responses to field names using JSONPath

4. **Evaluate Rules** (`RuleEvaluatorService`)
   - Creates Drools facts from fetched data
   - Inserts facts into Drools session
   - Fires all rules
   - Collects eligible documents

5. **Return Response**
   - Returns list of eligible documents to client

### Example Rule Evaluation

**Given:**
- Customer has `cardholderAgreementsTNCCode = "TNC_GOLD_2024"`
- Account has `accountBalance = 75000`

**Rules:**

1. **RULE-001**: If TNC code is "TNC_GOLD_2024" → Add "DOC-TNC-GOLD-2024-BENEFITS"
2. **RULE-004**: If TNC code is "TNC_GOLD_2024" AND balance > 50000 → Add "DOC-HIGH-BALANCE-GOLD-EXCLUSIVE"

**Result:**
Both rules match, so customer gets both documents.

---

## Next Steps

1. **Add More Rules**: Insert new rows into `eligibility_rules` table
2. **Configure APIs**: Add more data sources in `data_sources` table
3. **Monitor**: Use Spring Boot Actuator endpoints
4. **Scale**: Deploy multiple instances (configuration is shared via database)
5. **Admin UI**: Build a React/Angular UI for managing rules

---

## Troubleshooting

### Database Connection Issues

```bash
# Check PostgreSQL is running
pg_isready

# Check connection
psql -U postgres -d drools_eligibility -c "SELECT 1;"
```

### Port Already in Use

Change the port in `application.yml`:

```yaml
server:
  port: 8081
```

### Cache Issues

Clear cache by restarting the application or implementing a cache eviction endpoint.

---

## License

MIT License - Feel free to use this for your projects!
