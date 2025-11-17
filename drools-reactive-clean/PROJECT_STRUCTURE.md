# Project Structure

This document explains the organization and purpose of each component.

---

## Directory Structure

```
drools-reactive-clean/
│
├── README.md                    # Comprehensive project overview
├── GETTING_STARTED.md          # Step-by-step setup guide
├── PROJECT_STRUCTURE.md        # This file - explains structure
├── pom.xml                     # Maven dependencies
├── .gitignore                  # Git ignore rules
│
├── src/main/
│   ├── java/com/example/eligibility/
│   │   │
│   │   ├── EligibilityApplication.java     # Spring Boot main class
│   │   │
│   │   ├── controller/                      # REST API Layer
│   │   │   └── EligibilityController.java  # Eligibility API endpoints
│   │   │
│   │   ├── service/                         # Business Logic Layer
│   │   │   ├── EligibilityService.java           # Main orchestrator
│   │   │   ├── ConfigurationLoaderService.java   # DB config loader
│   │   │   ├── DataFetcherService.java           # External API calls
│   │   │   └── RuleEvaluatorService.java         # Drools rule evaluation
│   │   │
│   │   ├── repository/                      # Data Access Layer
│   │   │   ├── DataSourceRepository.java        # R2DBC for data_sources
│   │   │   └── EligibilityRuleRepository.java   # R2DBC for eligibility_rules
│   │   │
│   │   ├── entity/                          # Database Entities
│   │   │   ├── DataSourceEntity.java            # Maps to data_sources table
│   │   │   └── EligibilityRuleEntity.java       # Maps to eligibility_rules table
│   │   │
│   │   ├── model/                           # DTOs and Domain Models
│   │   │   ├── EligibilityRequest.java          # API request
│   │   │   ├── EligibilityResponse.java         # API response
│   │   │   ├── DataContext.java                 # Container for fetched data
│   │   │   └── RuleFact.java                    # Drools fact
│   │   │
│   │   └── config/                          # Spring Configuration
│   │       ├── DroolsConfig.java                # Drools bean setup
│   │       ├── WebClientConfig.java             # HTTP client setup
│   │       └── CacheConfig.java                 # Caffeine cache setup
│   │
│   └── resources/
│       ├── application.yml                  # Application configuration
│       └── db/                              # Database scripts
│           ├── schema.sql                   # Table creation
│           └── sample-data.sql              # Sample rules and data sources
│
└── src/test/
    └── java/com/example/eligibility/
        └── (tests go here)
```

---

## Layer Responsibilities

### 1. Controller Layer (`controller/`)

**Purpose:** Handle HTTP requests and responses.

**Files:**
- `EligibilityController.java`
  - REST API endpoints
  - Request validation
  - Response formatting

**Key Points:**
- Should NOT contain business logic
- Delegates to Service layer
- Returns Mono/Flux (reactive types)

---

### 2. Service Layer (`service/`)

**Purpose:** Implement business logic and orchestration.

**Files:**

#### `EligibilityService.java` - Main Orchestrator
- Coordinates entire eligibility check
- Calls DataFetcher → RuleEvaluator
- Handles errors and timeouts

#### `ConfigurationLoaderService.java` - Configuration Loader
- Loads data sources from database
- Loads eligibility rules from database
- Caches results for performance

#### `DataFetcherService.java` - External API Integration
- Fetches data from configured APIs
- Handles dependent/chained API calls
- Maps responses using JSONPath
- Fully reactive (non-blocking)

#### `RuleEvaluatorService.java` - Rule Evaluation
- Evaluates rules against fetched data
- Implements condition logic (EQUALS, GREATER_THAN, etc.)
- Returns eligible documents
- Blocking operation (uses dedicated thread pool)

**Key Points:**
- Contains all business logic
- Reusable across different controllers
- Unit testable

---

### 3. Repository Layer (`repository/`)

**Purpose:** Database access using R2DBC (reactive).

**Files:**
- `DataSourceRepository.java` - Access data_sources table
- `EligibilityRuleRepository.java` - Access eligibility_rules table

**Key Points:**
- Extends ReactiveCrudRepository
- Uses R2DBC for non-blocking DB access
- Returns Mono/Flux

---

### 4. Entity Layer (`entity/`)

**Purpose:** Map to database tables.

**Files:**
- `DataSourceEntity.java` - Maps to data_sources table
- `EligibilityRuleEntity.java` - Maps to eligibility_rules table

**Key Points:**
- Use `@Table` annotation
- JSONB columns use `Json` type
- Include audit fields (createdAt, updatedAt)

---

### 5. Model Layer (`model/`)

**Purpose:** DTOs and domain objects.

**Files:**
- `EligibilityRequest.java` - API input
- `EligibilityResponse.java` - API output
- `DataContext.java` - Container for API data
- `RuleFact.java` - Drools fact object

**Key Points:**
- Plain Java objects (POJOs)
- Used for data transfer between layers
- No database annotations

---

### 6. Configuration Layer (`config/`)

**Purpose:** Spring bean configuration.

**Files:**

#### `DroolsConfig.java`
- Configures Drools KieServices
- Creates KieContainer
- Sets up thread pool for rule evaluation

#### `WebClientConfig.java`
- Configures WebClient for HTTP calls
- Sets timeouts and connection pooling

#### `CacheConfig.java`
- Configures Caffeine cache
- Sets cache size and TTL

---

## Data Flow

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP GET /api/v1/eligibility
       ▼
┌─────────────────────────────┐
│  EligibilityController      │
│  - Validate request          │
│  - Call service              │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  EligibilityService         │
│  - Orchestrate process       │
└──────┬──────────────────────┘
       │
       ├──────────────────────────┐
       ▼                          ▼
┌──────────────────┐    ┌──────────────────┐
│ DataFetcher      │    │ ConfigLoader     │
│ - Fetch API data │    │ - Load rules     │
└──────┬───────────┘    └──────┬───────────┘
       │                       │
       ▼                       ▼
┌──────────────────────────────────┐
│  RuleEvaluatorService            │
│  - Evaluate rules                │
│  - Return eligible documents     │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  EligibilityResponse        │
│  - List of documents         │
└─────────────────────────────┘
```

---

## Database Schema

### Table: `data_sources`

Stores external API configuration.

**Key Columns:**
- `id` - Unique identifier (e.g., "arrangements_api")
- `name` - Human-readable name
- `type` - Always "REST_API"
- `configuration` - JSONB with API details
- `enabled` - Is this data source active?

**JSONB Structure:**
```json
{
  "method": "GET",
  "baseUrl": "http://localhost:8081",
  "endpoint": "/api/v1/arrangements/{arrangementId}",
  "timeoutMs": 5000,
  "retryCount": 2,
  "dependsOn": [...],
  "responseMapping": [...]
}
```

---

### Table: `eligibility_rules`

Stores business rules for document eligibility.

**Key Columns:**
- `rule_id` - Unique identifier (e.g., "RULE-001")
- `document_id` - Document to add if rule matches
- `name` - Rule name
- `priority` - Higher priority = evaluated first
- `enabled` - Is this rule active?
- `conditions` - JSONB with rule conditions

**JSONB Structure:**
```json
{
  "type": "ALL",
  "expressions": [
    {
      "source": "cardholder_agreements_api",
      "field": "cardholderAgreementsTNCCode",
      "operator": "EQUALS",
      "value": "TNC_GOLD_2024"
    }
  ]
}
```

---

## Key Design Patterns

### 1. Reactive Programming
- Uses Project Reactor (Mono/Flux)
- Non-blocking I/O
- Efficient resource usage

### 2. Database-Driven Configuration
- Rules stored in PostgreSQL JSONB
- Runtime updates without restart
- Admin UI friendly

### 3. Service Layer Pattern
- Clear separation of concerns
- Reusable business logic
- Easy to test

### 4. Repository Pattern
- Abstraction over data access
- Easy to swap implementations
- Clean interfaces

### 5. DTO Pattern
- Separate API models from entities
- Prevents exposing internal structure
- Version API independently

---

## How to Extend

### Add a New External API

1. Insert row into `data_sources` table
2. Restart app (cache will refresh)
3. Use new data in rules

### Add a New Rule

1. Insert row into `eligibility_rules` table
2. Restart app (cache will refresh)
3. Test with API call

### Add a New Endpoint

1. Create new method in controller
2. Implement logic in service
3. Add tests

### Add Database Migration

1. Create new SQL file in `src/main/resources/db/`
2. Run manually or use Flyway/Liquibase

---

## Testing Strategy

### Unit Tests
- Test each service in isolation
- Mock dependencies
- Test edge cases

### Integration Tests
- Test with real database (H2 or Testcontainers)
- Test full flow end-to-end
- Verify reactive behavior

### Manual Testing
- Use curl or Postman
- Test with sample data
- Verify database changes

---

## Performance Considerations

### Caching
- Configuration cached for 10 minutes
- Reduces database load
- Invalidate on updates

### Thread Pool
- Dedicated pool for Drools (blocking)
- Prevents thread starvation
- Configured in `application.yml`

### Connection Pool
- R2DBC connection pooling
- Max 20 connections
- Adjust based on load

### Timeouts
- API calls: 5 seconds
- Rule evaluation: 500ms
- Database queries: default

---

## Troubleshooting

### Problem: Cache not updating

**Solution:** Restart app or implement cache eviction endpoint

### Problem: Slow rule evaluation

**Solution:** Increase thread pool size in `application.yml`

### Problem: Database connection errors

**Solution:** Check credentials in `application.yml`

### Problem: External API timeouts

**Solution:** Increase timeout in data source configuration

---

## Summary

This project demonstrates a **production-ready** approach to:
- Database-driven business rules
- Reactive programming with Spring WebFlux
- External API integration
- Rule engine (Drools)

All components are **well-organized**, **documented**, and **easy to follow**.
