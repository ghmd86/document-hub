# Quick Reference Guide

Quick commands and examples for common tasks.

---

## Database Commands

### Connect to Database
```bash
psql -U postgres -d drools_eligibility
```

### View All Data Sources
```sql
SELECT id, name, enabled FROM data_sources ORDER BY id;
```

### View All Rules
```sql
SELECT rule_id, name, priority, enabled
FROM eligibility_rules
ORDER BY priority DESC;
```

### View Rule Conditions (Pretty Print)
```sql
SELECT rule_id, name, jsonb_pretty(conditions)
FROM eligibility_rules
WHERE rule_id = 'RULE-001';
```

### Add New Data Source
```sql
INSERT INTO data_sources (id, name, type, configuration, enabled) VALUES (
    'my_api',
    'My Custom API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8085",
        "endpoint": "/api/v1/data/{customerId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "responseMapping": [
            {
                "fieldName": "myField",
                "jsonPath": "$.myField",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true
);
```

### Add New Rule
```sql
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions
) VALUES (
    'RULE-CUSTOM',
    'DOC-MY-DOCUMENT',
    'My Custom Rule',
    'Description of what this rule does',
    75,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "my_api",
                "field": "myField",
                "operator": "EQUALS",
                "value": "expectedValue"
            }
        ]
    }'::jsonb
);
```

### Disable a Rule
```sql
UPDATE eligibility_rules
SET enabled = false
WHERE rule_id = 'RULE-001';
```

### Enable a Rule
```sql
UPDATE eligibility_rules
SET enabled = true
WHERE rule_id = 'RULE-001';
```

### Delete a Rule
```sql
DELETE FROM eligibility_rules
WHERE rule_id = 'RULE-TEST';
```

---

## Maven Commands

### Clean and Build
```bash
mvn clean install
```

### Run Application
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

### Package as JAR
```bash
mvn clean package
java -jar target/drools-reactive-clean-1.0.0.jar
```

### Skip Tests
```bash
mvn clean install -DskipTests
```

---

## API Testing Commands

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Check Eligibility
```bash
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001"
```

### Pretty Print JSON Response
```bash
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST001&accountId=ACC001&arrangementId=ARR001" | jq
```

### Check with Different Parameters
```bash
curl "http://localhost:8080/api/v1/eligibility?customerId=CUST002&accountId=ACC002&arrangementId=ARR002"
```

---

## Condition Operators

Available operators for rule conditions:

| Operator | Description | Example Value |
|----------|-------------|---------------|
| EQUALS | Exact match | "TNC_GOLD_2024" |
| NOT_EQUALS | Not equal to | "INACTIVE" |
| GREATER_THAN | Numeric > | 50000 |
| GREATER_THAN_OR_EQUAL | Numeric >= | 50000 |
| LESS_THAN | Numeric < | 100000 |
| LESS_THAN_OR_EQUAL | Numeric <= | 100000 |
| IN | Value in list | ["GOLD", "PLATINUM"] |

---

## Rule Condition Examples

### Single Condition (Simple)
```json
{
  "type": "ALL",
  "expressions": [
    {
      "source": "account_service_api",
      "field": "accountStatus",
      "operator": "EQUALS",
      "value": "ACTIVE"
    }
  ]
}
```

### Multiple Conditions (AND Logic)
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

### Multiple Conditions (OR Logic)
```json
{
  "type": "ANY",
  "expressions": [
    {
      "source": "customer_service_api",
      "field": "customerTier",
      "operator": "EQUALS",
      "value": "GOLD"
    },
    {
      "source": "customer_service_api",
      "field": "customerTier",
      "operator": "EQUALS",
      "value": "PLATINUM"
    }
  ]
}
```

### IN Operator (Value in List)
```json
{
  "type": "ALL",
  "expressions": [
    {
      "source": "account_service_api",
      "field": "accountType",
      "operator": "IN",
      "value": ["CHECKING", "SAVINGS", "CREDIT_CARD"]
    }
  ]
}
```

---

## Troubleshooting Commands

### Check if PostgreSQL is Running
```bash
pg_isready
```

### Check if Port 8080 is in Use (Linux/Mac)
```bash
lsof -i :8080
```

### Check if Port 8080 is in Use (Windows)
```bash
netstat -ano | findstr :8080
```

### View Application Logs
```bash
tail -f logs/application.log
```

### Test Database Connection
```bash
psql -U postgres -d drools_eligibility -c "SELECT 1;"
```

---

## Common Scenarios

### Scenario 1: Add Rule for High Balance Customers
```sql
INSERT INTO eligibility_rules (
    rule_id, document_id, name, priority, enabled, conditions
) VALUES (
    'RULE-HIGH-BALANCE',
    'DOC-VIP-BENEFITS',
    'High Balance VIP Benefits',
    95,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "account_service_api",
                "field": "accountBalance",
                "operator": "GREATER_THAN",
                "value": 1000000
            }
        ]
    }'::jsonb
);
```

### Scenario 2: Add Rule for Premium Customers with Active Accounts
```sql
INSERT INTO eligibility_rules (
    rule_id, document_id, name, priority, enabled, conditions
) VALUES (
    'RULE-PREMIUM-ACTIVE',
    'DOC-PREMIUM-PERKS',
    'Premium Active Customer Perks',
    90,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "customer_service_api",
                "field": "customerTier",
                "operator": "EQUALS",
                "value": "PREMIUM"
            },
            {
                "source": "account_service_api",
                "field": "accountStatus",
                "operator": "EQUALS",
                "value": "ACTIVE"
            }
        ]
    }'::jsonb
);
```

---

## Configuration Files

### application.yml Locations
- Default: `src/main/resources/application.yml`
- Local override: `src/main/resources/application-local.yml`

### Database Schema
- Location: `src/main/resources/db/schema.sql`

### Sample Data
- Location: `src/main/resources/db/sample-data.sql`

---

## Useful Database Queries

### Count Rules by Priority Range
```sql
SELECT
    CASE
        WHEN priority >= 100 THEN 'Critical (100+)'
        WHEN priority >= 80 THEN 'High (80-99)'
        WHEN priority >= 50 THEN 'Medium (50-79)'
        ELSE 'Low (< 50)'
    END as priority_level,
    COUNT(*) as count
FROM eligibility_rules
WHERE enabled = true
GROUP BY priority_level
ORDER BY MIN(priority) DESC;
```

### Find Rules for Specific Document
```sql
SELECT rule_id, name, priority, enabled
FROM eligibility_rules
WHERE document_id = 'DOC-TNC-GOLD-2024-BENEFITS';
```

### List Data Sources by Type
```sql
SELECT type, COUNT(*) as count
FROM data_sources
WHERE enabled = true
GROUP BY type;
```

---

## Performance Tuning

### Increase Thread Pool Size
Edit `application.yml`:
```yaml
drools:
  thread-pool-size: 20  # Default: 10
```

### Adjust Cache Settings
Edit `application.yml`:
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=1200s
```

### Adjust Database Connection Pool
Edit `application.yml`:
```yaml
spring:
  r2dbc:
    pool:
      initial-size: 20
      max-size: 50
```

---

## Next Steps

1. **Read GETTING_STARTED.md** - Step-by-step setup guide
2. **Read PROJECT_STRUCTURE.md** - Understand the architecture
3. **Read README.md** - Comprehensive overview
4. **Experiment with rules** - Add/modify rules in database
5. **Build an Admin UI** - Create UI for managing rules

---

## Support

For issues or questions:
- Check the README.md
- Review the code comments
- Test with sample data
- Check PostgreSQL logs
- Review Spring Boot logs
