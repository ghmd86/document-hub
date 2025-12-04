# PostgreSQL Setup Guide for Document Hub POC

## Overview

This guide explains how to switch from H2 in-memory database to PostgreSQL for persistent storage.

## Prerequisites

1. **PostgreSQL installed** (version 12 or higher recommended)
2. **PostgreSQL running** on your system
3. **Database access** (username/password)

## Step-by-Step Setup

### Step 1: Install PostgreSQL (if not already installed)

**Windows:**
```bash
# Download from https://www.postgresql.org/download/windows/
# Or use Chocolatey
choco install postgresql
```

**macOS:**
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Step 2: Create Database and User

```bash
# Connect to PostgreSQL
psql -U postgres

# Inside psql prompt:
CREATE DATABASE document_hub;

# Create user (optional - or use existing postgres user)
CREATE USER dochub_user WITH PASSWORD 'your_secure_password';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE document_hub TO dochub_user;

# Connect to the database
\c document_hub

# Create schema
CREATE SCHEMA IF NOT EXISTS document_hub;

# Grant schema privileges
GRANT ALL ON SCHEMA document_hub TO dochub_user;

# Exit
\q
```

### Step 3: Update application.properties

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api/v1

# Spring Application
spring.application.name=document-hub-poc

# ========================================
# OPTION 1: H2 In-Memory (Development)
# ========================================
# Uncomment for H2 in-memory database
#spring.r2dbc.url=r2dbc:h2:mem:///document_hub?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
#spring.r2dbc.username=sa
#spring.r2dbc.password=

# ========================================
# OPTION 2: PostgreSQL (Production/Testing)
# ========================================
# Uncomment for PostgreSQL
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/document_hub
spring.r2dbc.username=postgres
spring.r2dbc.password=your_password_here

# R2DBC Connection Pool
spring.r2dbc.pool.initial-size=10
spring.r2dbc.pool.max-size=50
spring.r2dbc.pool.max-idle-time=30m

# Jackson Configuration
spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false
spring.jackson.serialization.INDENT_OUTPUT=true
spring.jackson.default-property-inclusion=non_null

# SpringDoc OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true

# Application Configuration
app.pagination.default-page-size=20
app.pagination.max-page-size=100

# Logging
logging.level.root=INFO
logging.level.com.documenthub=DEBUG
logging.level.org.springframework.r2dbc=DEBUG
logging.level.io.r2dbc.postgresql.QUERY=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
```

### Step 4: Update DatabaseConfig.java

Edit `src/main/java/com/documenthub/config/DatabaseConfig.java` to use PostgreSQL schema files:

```java
package com.documenthub.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.util.ArrayList;
import java.util.List;

/**
 * R2DBC Database Configuration
 * Supports both H2 and PostgreSQL
 */
@Configuration
public class DatabaseConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    /**
     * Register custom converters for JSON handling
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new JsonNodeReadingConverter());
        converters.add(new JsonNodeWritingConverter());
        return R2dbcCustomConversions.of(dialect, converters);
    }

    /**
     * Initialize database schema and data on startup
     * Uses PostgreSQL scripts if PostgreSQL URL detected, otherwise H2 scripts
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        // Detect database type and load appropriate scripts
        if (r2dbcUrl.contains("postgresql")) {
            // PostgreSQL mode - load PostgreSQL-specific scripts
            populator.addScript(new ClassPathResource("schema-postgres.sql"));
            populator.addScript(new ClassPathResource("test-data-postgres.sql"));
        } else {
            // H2 mode - load H2-specific scripts
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.addScript(new ClassPathResource("data.sql"));
        }

        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
```

### Step 5: Create PostgreSQL Schema File

Create `src/main/resources/schema-postgres.sql`:

```sql
-- PostgreSQL Schema for Document Hub POC
-- Compatible with PostgreSQL 12+

CREATE SCHEMA IF NOT EXISTS document_hub;

CREATE TABLE IF NOT EXISTS document_hub.master_template_definition
(
    master_template_id uuid NOT NULL,
    template_version integer NOT NULL,
    legacy_template_id varchar,
    template_name varchar NOT NULL,
    template_description varchar,
    line_of_business varchar,
    template_category varchar,
    template_type_old varchar,
    language_code varchar,
    owning_dept varchar,
    notification_needed boolean NOT NULL DEFAULT false,
    regulatory_flag boolean NOT NULL DEFAULT false,
    message_center_doc_flag boolean NOT NULL DEFAULT false,
    document_channel_old jsonb,
    template_variables jsonb,
    start_date bigint,
    end_date bigint,
    created_by varchar NOT NULL,
    created_timestamp timestamp NOT NULL DEFAULT now(),
    updated_by varchar,
    updated_timestamp timestamp,
    archive_indicator boolean NOT NULL DEFAULT false,
    archive_timestamp timestamp,
    version_number bigint NOT NULL DEFAULT 0,
    record_status varchar NOT NULL DEFAULT '1',
    legacy_template_name varchar,
    display_name varchar,
    template_type varchar,
    active_flag boolean,
    shared_document_flag boolean,
    data_extraction_config jsonb,
    access_control jsonb,
    required_fields jsonb,
    template_config jsonb,
    sharing_scope varchar,
    CONSTRAINT master_template_definition_pkey PRIMARY KEY (master_template_id, template_version)
);

CREATE TABLE IF NOT EXISTS document_hub.storage_index
(
    storage_index_id uuid NOT NULL,
    master_template_id uuid NOT NULL,
    storage_vendor varchar,
    reference_key varchar,
    reference_key_type varchar,
    account_key uuid,
    customer_key uuid,
    storage_document_key uuid,
    file_name varchar,
    doc_creation_date bigint,
    accessible_flag boolean NOT NULL DEFAULT true,
    doc_metadata jsonb,
    created_by varchar NOT NULL,
    created_timestamp timestamp NOT NULL DEFAULT now(),
    updated_by varchar,
    updated_timestamp timestamp,
    archive_indicator boolean NOT NULL DEFAULT false,
    archive_timestamp timestamp,
    version_number bigint NOT NULL DEFAULT 0,
    record_status varchar NOT NULL DEFAULT '1',
    template_version integer,
    template_type varchar,
    shared_flag boolean NOT NULL DEFAULT false,
    generation_vendor_id uuid,
    CONSTRAINT storage_index_pkey PRIMARY KEY (storage_index_id)
);

CREATE TABLE IF NOT EXISTS document_hub.template_vendor_mapping
(
    template_vendor_id uuid NOT NULL,
    master_template_id uuid NOT NULL,
    vendor varchar NOT NULL,
    vendor_template_key varchar,
    reference_key_type varchar,
    consumer_id uuid,
    template_content bytea,
    start_date bigint,
    end_date bigint,
    vendor_mapping_version integer NOT NULL DEFAULT 1,
    primary_flag boolean NOT NULL DEFAULT false,
    schema_info jsonb,
    active_flag boolean NOT NULL DEFAULT true,
    created_by varchar NOT NULL,
    created_timestamp timestamp NOT NULL DEFAULT now(),
    updated_by varchar,
    updated_timestamp timestamp,
    archive_indicator boolean NOT NULL DEFAULT false,
    archive_timestamp timestamp,
    record_status varchar NOT NULL DEFAULT '1',
    version_number bigint,
    template_version integer,
    vendor_template_name varchar,
    template_fields jsonb,
    vendor_config jsonb,
    api_config jsonb,
    template_status varchar,
    CONSTRAINT template_vendor_mapping_pkey PRIMARY KEY (template_vendor_id)
);

ALTER TABLE document_hub.storage_index
    DROP CONSTRAINT IF EXISTS storageindex_mastertemplatedef_fkey;

ALTER TABLE document_hub.storage_index
    ADD CONSTRAINT storageindex_mastertemplatedef_fkey
    FOREIGN KEY (master_template_id, template_version)
    REFERENCES document_hub.master_template_definition (master_template_id, template_version);

ALTER TABLE document_hub.template_vendor_mapping
    DROP CONSTRAINT IF EXISTS templatevendmapping_mastertemplatedef_fkey;

ALTER TABLE document_hub.template_vendor_mapping
    ADD CONSTRAINT templatevendmapping_mastertemplatedef_fkey
    FOREIGN KEY (master_template_id, template_version)
    REFERENCES document_hub.master_template_definition (master_template_id, template_version);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_storage_index_template
    ON document_hub.storage_index(master_template_id, template_version);

CREATE INDEX IF NOT EXISTS idx_storage_index_reference_key
    ON document_hub.storage_index(reference_key, reference_key_type);

CREATE INDEX IF NOT EXISTS idx_storage_index_account
    ON document_hub.storage_index(account_key);

CREATE INDEX IF NOT EXISTS idx_template_type
    ON document_hub.master_template_definition(template_type);
```

### Step 6: Verify Test Data Files

Make sure you have PostgreSQL-compatible test data:
- `test-data-postgres.sql` - already exists ✅
- `test-data-disclosure-example-postgres.sql` - already exists ✅

These files use PostgreSQL syntax:
- `'...'::uuid` for UUID casting
- `'...'::jsonb` for JSONB casting
- `true`/`false` for booleans

### Step 7: Start the Application

```bash
# Clean and restart
mvn clean spring-boot:run

# Or if already compiled
mvn spring-boot:run
```

### Step 8: Verify Connection

Check the logs for successful connection:

```
2025-12-04 13:14:41 - o.s.r2dbc.core.DatabaseClient - Executing SQL statement [...]
2025-12-04 13:14:41 - o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080
2025-12-04 13:14:41 - c.documenthub.DocumentHubApplication - Started DocumentHubApplication
```

### Step 9: Test Endpoints

```bash
# Test mock API
curl http://localhost:8080/api/v1/mock-api/creditcard/accounts/550e8400-e29b-41d4-a716-446655440000/arrangements

# Test document enquiry (if implemented)
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
    "customerId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

## Troubleshooting

### Issue: Connection Refused

**Check PostgreSQL is running:**
```bash
# Windows
pg_ctl status

# macOS/Linux
sudo systemctl status postgresql
# or
brew services list | grep postgresql
```

**Start PostgreSQL:**
```bash
# Windows
pg_ctl start

# macOS
brew services start postgresql@14

# Linux
sudo systemctl start postgresql
```

### Issue: Authentication Failed

**Reset password:**
```bash
# Connect as postgres superuser
sudo -u postgres psql

# Reset password
ALTER USER postgres PASSWORD 'new_password';
```

**Update application.properties** with the correct password.

### Issue: Database Does Not Exist

```bash
# Create database
psql -U postgres -c "CREATE DATABASE document_hub;"
```

### Issue: Permission Denied

```bash
# Grant all privileges
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE document_hub TO postgres;"
```

### Issue: Schema Not Found

The schema is created automatically by `schema-postgres.sql`. If issues persist:

```bash
# Manually create schema
psql -U postgres -d document_hub -c "CREATE SCHEMA IF NOT EXISTS document_hub;"
```

### Issue: Port Already in Use

Change the port in `application.properties`:
```properties
server.port=8081
```

## Data Migration (Optional)

If you want to migrate data from H2 to PostgreSQL:

### Option 1: Export/Import via SQL

```bash
# 1. Export data from H2 (requires H2 console or custom script)
# Not straightforward with H2

# 2. Use the existing PostgreSQL test data files
psql -U postgres -d document_hub -f src/main/resources/test-data-postgres.sql
```

### Option 2: Use Application Logic

Write a migration service that reads from H2 and writes to PostgreSQL.

## Performance Optimization

### Enable Connection Pooling

Already configured in `application.properties`:
```properties
spring.r2dbc.pool.initial-size=10
spring.r2dbc.pool.max-size=50
spring.r2dbc.pool.max-idle-time=30m
```

### Add Indexes

Already included in `schema-postgres.sql`:
- Index on template_id and version
- Index on reference_key
- Index on account_key
- Index on template_type

### Monitor Performance

```sql
-- Check active connections
SELECT * FROM pg_stat_activity WHERE datname = 'document_hub';

-- Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'document_hub'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

## Switching Back to H2

To switch back to H2 for development:

1. Comment out PostgreSQL config in `application.properties`
2. Uncomment H2 config
3. Restart application

```properties
# H2 In-Memory
spring.r2dbc.url=r2dbc:h2:mem:///document_hub?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.r2dbc.username=sa
spring.r2dbc.password=

# PostgreSQL (commented)
#spring.r2dbc.url=r2dbc:postgresql://localhost:5432/document_hub
#spring.r2dbc.username=postgres
#spring.r2dbc.password=your_password
```

## Summary Checklist

- [ ] PostgreSQL installed and running
- [ ] Database `document_hub` created
- [ ] User credentials configured
- [ ] `application.properties` updated with PostgreSQL URL
- [ ] `DatabaseConfig.java` updated to detect and load PostgreSQL scripts
- [ ] `schema-postgres.sql` created
- [ ] Application started successfully
- [ ] Endpoints tested and working
- [ ] Data loaded correctly

## Production Considerations

For production deployment:

1. **Security:**
   - Use strong passwords
   - Enable SSL/TLS connections
   - Restrict database access by IP

2. **Backup:**
   - Set up automated backups
   - Test restore procedures

3. **Monitoring:**
   - Enable query logging
   - Monitor connection pool metrics
   - Set up alerts for errors

4. **Configuration:**
   - Use environment variables for sensitive data
   - Consider using Spring profiles (dev, test, prod)
   - Externalize configuration

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring R2DBC Documentation](https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/)
- [R2DBC PostgreSQL Driver](https://github.com/r2dbc/r2dbc-postgresql)
