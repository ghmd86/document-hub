# Getting Started Guide

**Drools Reactive Eligibility System**

This guide will walk you through setting up and running the application from scratch.

---

## Step 1: Prerequisites

Make sure you have the following installed:

```bash
# Check Java version (need 17+)
java -version

# Check Maven version (need 3.6+)
mvn -version

# Check PostgreSQL version (need 12+)
psql --version
```

If any are missing:
- **Java 17**: Download from [AdoptOpenJDK](https://adoptium.net/)
- **Maven**: Download from [Maven website](https://maven.apache.org/download.cgi)
- **PostgreSQL**: Download from [PostgreSQL website](https://www.postgresql.org/download/)

---

## Step 2: Create Database

Open a terminal and run:

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE drools_eligibility;

# Verify it was created
\l

# Exit psql
\q
```

---

## Step 3: Set Up Database Schema

Navigate to the project directory and run:

```bash
cd drools-reactive-clean

# Create tables
psql -U postgres -d drools_eligibility -f src/main/resources/db/schema.sql

# Load sample data
psql -U postgres -d drools_eligibility -f src/main/resources/db/sample-data.sql
```

**Verify the setup:**

```bash
# Connect to database
psql -U postgres -d drools_eligibility

# Check tables
\dt

# Check data sources (should see 4 rows)
SELECT id, name FROM data_sources;

# Check rules (should see 7 rows)
SELECT rule_id, name FROM eligibility_rules;

# Exit
\q
```

---

## Step 4: Configure Application

Edit the database credentials in `src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/drools_eligibility
    username: postgres
    password: YOUR_PASSWORD_HERE  # ⬅️ Change this
```

---

## Step 5: Build the Project

```bash
# Clean and build
mvn clean install

# This should complete without errors
# You should see: BUILD SUCCESS
```

If you get errors:
- Make sure Java 17+ is set as default
- Check your internet connection (Maven downloads dependencies)
- Check `pom.xml` for any syntax errors

---

## Step 6: Run the Application

```bash
# Run with Maven
mvn spring-boot:run
```

You should see:

```
===========================================
Drools Reactive Eligibility System Started
===========================================

API: http://localhost:8080/api/v1/eligibility
Health: http://localhost:8080/actuator/health
```

---

## Step 7: Test the API

Open a new terminal (keep the app running) and test:

### Test 1: Health Check

```bash
curl http://localhost:8080/api/v1/eligibility/health
```

**Expected:** `Eligibility Service is running`

### Test 2: Check Eligibility

```bash
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001"
```

**Expected response:**

```json
{
  "customerId": "CUST001",
  "accountId": "ACC001",
  "eligibleDocuments": [],
  "evaluationTimeMs": 123
}
```

**Note:** The `eligibleDocuments` will be empty because external APIs are not running.
This is normal for this POC.

---

## Step 8: Understanding What Happened

When you called the API:

1. **EligibilityController** received the request
2. **EligibilityService** orchestrated the process
3. **ConfigurationLoaderService** loaded rules from database
4. **DataFetcherService** tried to fetch data from external APIs
   - Since APIs aren't running, it returned empty data
5. **RuleEvaluatorService** evaluated rules
   - No rules matched because data was empty
6. **Response** was returned with empty documents

---

## Step 9: View Database Configuration

Let's see what rules and data sources are configured:

```bash
# Connect to database
psql -U postgres -d drools_eligibility

# View data sources (external APIs)
SELECT
    id,
    name,
    jsonb_pretty(configuration) as config
FROM data_sources
WHERE id = 'arrangements_api';

# View rules
SELECT
    rule_id,
    name,
    priority,
    jsonb_pretty(conditions) as conditions
FROM eligibility_rules
WHERE rule_id = 'RULE-001';

# Exit
\q
```

You'll see the JSONB configuration that drives the system!

---

## Step 10: Add a New Rule

Let's add a new rule to test the system:

```bash
# Connect to database
psql -U postgres -d drools_eligibility
```

```sql
-- Add a simple rule that always matches
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions
) VALUES (
    'RULE-TEST',
    'DOC-TEST-ALWAYS-ELIGIBLE',
    'Test Rule - Always Eligible',
    'This rule always matches for testing',
    999,
    true,
    '{
        "type": "ALL",
        "expressions": []
    }'::jsonb
);
```

**Restart the app and test again:**

```bash
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001"
```

You should now see:

```json
{
  "customerId": "CUST001",
  "accountId": "ACC001",
  "eligibleDocuments": ["DOC-TEST-ALWAYS-ELIGIBLE"],
  "evaluationTimeMs": 89
}
```

**Success!** The rule engine is working!

---

## Step 11: Explore the Code

Now that it's running, explore the code:

### 1. Start with the Flow

```
EligibilityController.java
  ↓
EligibilityService.java (orchestrator)
  ↓
DataFetcherService.java (fetch external data)
  ↓
RuleEvaluatorService.java (evaluate rules)
```

### 2. Check the Configuration

- `application.yml` - App configuration
- `DroolsConfig.java` - Drools setup
- `WebClientConfig.java` - HTTP client setup

### 3. Understand the Data Model

- `EligibilityRequest.java` - Input
- `EligibilityResponse.java` - Output
- `DataContext.java` - Fetched data container
- `RuleFact.java` - Drools fact

### 4. Database Entities

- `DataSourceEntity.java` - External API config
- `EligibilityRuleEntity.java` - Business rules

---

## Step 12: Next Steps

### Option A: Set Up Mock External APIs

Create simple mock services to test the full flow:

```bash
# TODO: Add instructions for setting up mock APIs
```

### Option B: Add More Rules

Practice adding rules via database:

```sql
INSERT INTO eligibility_rules (
    rule_id, document_id, name, priority, enabled, conditions
) VALUES (
    'RULE-MY-TEST',
    'DOC-MY-DOCUMENT',
    'My Custom Rule',
    50,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "test_api",
                "field": "testField",
                "operator": "EQUALS",
                "value": "testValue"
            }
        ]
    }'::jsonb
);
```

### Option C: Build an Admin UI

Create a React/Angular UI for managing rules without SQL.

---

## Troubleshooting

### App won't start

1. Check PostgreSQL is running: `pg_isready`
2. Check database exists: `psql -U postgres -l | grep drools`
3. Check credentials in `application.yml`
4. Check port 8080 is free: `lsof -i :8080` (Mac/Linux) or `netstat -ano | findstr :8080` (Windows)

### Database connection errors

```bash
# Test connection manually
psql -U postgres -d drools_eligibility -c "SELECT 1;"
```

If this fails, fix PostgreSQL first before running the app.

### Rules not loading

Check the cache:

```bash
# Restart the app to clear cache
# Or implement cache eviction endpoint
```

---

## Summary

You now have a **fully functional** Drools-based reactive eligibility system!

**What you learned:**
- Set up PostgreSQL with JSONB columns
- Configure Spring Boot WebFlux
- Load configuration from database
- Evaluate rules dynamically
- Build reactive APIs

**Next:** Extend this system for your use case!
