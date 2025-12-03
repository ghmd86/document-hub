# PostgreSQL Setup Guide

## Quick Start

### 1. Create Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE document_hub;

# Connect to the database
\c document_hub
```

### 2. Run Schema Script

```bash
# Run the schema creation script
psql -U postgres -d document_hub -f src/main/resources/schema.sql
```

### 3. Load Test Data

```bash
# Load mock data for testing
psql -U postgres -d document_hub -f src/main/resources/test-data-postgres.sql
```

### 4. Verify Data

```bash
# Connect and verify
psql -U postgres -d document_hub

# Check templates
SELECT template_type, template_name, is_active
FROM master_template_definition;

# Check documents
SELECT template_type, file_name, is_shared
FROM storage_index;

# View extraction config for a template
SELECT
    template_type,
    jsonb_pretty(data_extraction_config)
FROM master_template_definition
WHERE template_type = 'ACCOUNT_STATEMENT';
```

## Connection Configuration

### Update application.properties

```properties
# PostgreSQL R2DBC configuration
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/document_hub
spring.r2dbc.username=postgres
spring.r2dbc.password=your_password

# Connection pool
spring.r2dbc.pool.initial-size=10
spring.r2dbc.pool.max-size=50
spring.r2dbc.pool.max-idle-time=30m

# Logging
logging.level.io.r2dbc.postgresql.QUERY=DEBUG
logging.level.org.springframework.r2dbc=DEBUG
```

### Add PostgreSQL R2DBC Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Key Differences from H2

### UUID Type
PostgreSQL uses `uuid` type with `::uuid` cast:
```sql
-- PostgreSQL
'11111111-1111-1111-1111-111111111111'::uuid

-- H2 (for reference)
'11111111-1111-1111-1111-111111111111'
```

### JSONB Type
PostgreSQL uses `jsonb` with `::jsonb` cast:
```sql
-- PostgreSQL
'{"key": "value"}'::jsonb

-- H2 (for reference)
'{"key": "value"}'
```

### JSONB Functions
PostgreSQL has rich JSONB support:

```sql
-- Get JSON field
data_extraction_config->'requiredFields'

-- Get JSON field as text
data_extraction_config->>'requiredFields'

-- Array length
jsonb_array_length(data_extraction_config->'requiredFields')

-- Pretty print
jsonb_pretty(data_extraction_config)

-- Check if key exists
data_extraction_config ? 'requiredFields'
```

## Useful Queries

### View Extraction Config Pretty-Printed

```sql
SELECT
    template_type,
    jsonb_pretty(data_extraction_config)
FROM master_template_definition;
```

### Count Required Fields

```sql
SELECT
    template_type,
    jsonb_array_length(data_extraction_config->'requiredFields') as field_count
FROM master_template_definition;
```

### Extract Specific Config Value

```sql
SELECT
    template_type,
    data_extraction_config->'executionStrategy'->>'mode' as execution_mode,
    data_extraction_config->'executionStrategy'->>'timeout' as timeout
FROM master_template_definition;
```

### List All Data Sources

```sql
SELECT
    template_type,
    jsonb_object_keys(data_extraction_config->'dataSources') as data_source_id
FROM master_template_definition;
```

### Check Field Sources

```sql
SELECT
    template_type,
    jsonb_object_keys(data_extraction_config->'fieldSources') as field_name
FROM master_template_definition;
```

## Test Data Verification

### Verify All Templates Loaded

```sql
SELECT
    template_type,
    template_name,
    is_active,
    jsonb_array_length(data_extraction_config->'requiredFields') as required_fields
FROM master_template_definition
ORDER BY template_type;
```

Expected output:
```
    template_type       |        template_name        | is_active | required_fields
------------------------+-----------------------------+-----------+-----------------
 ACCOUNT_STATEMENT      | Monthly Account Statement   | t         | 3
 BRANCH_SPECIFIC_DOCUMENT| Branch Specific Document   | t         | 4
 REGULATORY_DISCLOSURE  | Regulatory Disclosure Doc   | t         | 5
```

### Verify Documents Loaded

```sql
SELECT
    template_type,
    file_name,
    is_shared,
    CASE WHEN account_key IS NULL THEN 'Shared' ELSE 'Account-Specific' END as doc_type
FROM storage_index
ORDER BY template_type;
```

Expected output:
```
       template_type       |              file_name                 | is_shared |     doc_type
--------------------------+----------------------------------------+-----------+------------------
 ACCOUNT_STATEMENT        | Statement_January_2024.pdf             | f         | Account-Specific
 ACCOUNT_STATEMENT        | Statement_February_2024.pdf            | f         | Account-Specific
 BRANCH_SPECIFIC_DOCUMENT | West_Region_Compliance_Guide.pdf       | t         | Shared
 REGULATORY_DISCLOSURE    | Credit_Card_Disclosures_2024.pdf       | t         | Shared
```

## Troubleshooting

### Issue: "relation does not exist"
**Solution**: Run the schema.sql file first before test-data-postgres.sql

```bash
psql -U postgres -d document_hub -f src/main/resources/schema.sql
```

### Issue: "invalid input syntax for type uuid"
**Solution**: Ensure UUID values have `::uuid` cast in PostgreSQL

```sql
-- Correct
'11111111-1111-1111-1111-111111111111'::uuid

-- Incorrect
'11111111-1111-1111-1111-111111111111'
```

### Issue: "invalid input syntax for type json"
**Solution**: Ensure JSON values have `::jsonb` cast

```sql
-- Correct
'{"key": "value"}'::jsonb

-- Incorrect
'{"key": "value"}'
```

### Issue: Connection refused
**Solution**: Check PostgreSQL is running and accepting connections

```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Start PostgreSQL
sudo systemctl start postgresql

# Check listening ports
sudo netstat -plnt | grep 5432
```

### Issue: "permission denied for database"
**Solution**: Grant permissions to the user

```sql
GRANT ALL PRIVILEGES ON DATABASE document_hub TO your_username;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_username;
```

## Clean Up (If Needed)

### Delete All Test Data

```sql
-- Delete all data
DELETE FROM storage_index;
DELETE FROM master_template_definition;
```

### Drop and Recreate Database

```sql
-- Connect to postgres database
\c postgres

-- Drop database
DROP DATABASE IF EXISTS document_hub;

-- Recreate
CREATE DATABASE document_hub;

-- Reconnect and reload
\c document_hub
\i src/main/resources/schema.sql
\i src/main/resources/test-data-postgres.sql
```

## Performance Tips

### Create Indexes on JSONB Columns

```sql
-- Index for searching within JSONB
CREATE INDEX idx_extraction_config_gin
ON master_template_definition USING GIN (data_extraction_config);

-- Index for specific JSONB path
CREATE INDEX idx_required_fields
ON master_template_definition ((data_extraction_config->'requiredFields'));
```

### Analyze Tables

```sql
ANALYZE master_template_definition;
ANALYZE storage_index;
```

## Summary

✅ PostgreSQL-specific SQL script created
✅ UUID and JSONB casting handled
✅ Verification queries included
✅ Connection configuration documented
✅ Troubleshooting guide provided

Run the scripts in this order:
1. Create database
2. Run schema.sql
3. Run test-data-postgres.sql
4. Verify with queries
5. Start testing!
