# Quick Start Guide

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+
- Redis 6+

## 1. Database Setup

```sql
-- Create database
CREATE DATABASE documenthub;

-- Connect to database
\c documenthub

-- Create storage_index table
CREATE TABLE storage_index (
    storage_index_id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    doc_type VARCHAR(255),
    storage_vendor VARCHAR(255),
    reference_key VARCHAR(255),
    reference_key_type VARCHAR(255),
    account_key UUID,
    customer_key UUID,
    storage_document_key UUID,
    file_name VARCHAR(500),
    doc_creation_date BIGINT,
    is_accessible BOOLEAN DEFAULT TRUE,
    last_referenced BIGINT,
    time_referenced INT DEFAULT 0,
    doc_info JSONB,
    created_by VARCHAR(255),
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(255),
    updated_timestamp TIMESTAMP,
    archive_indicator BOOLEAN DEFAULT FALSE,
    archive_timestamp TIMESTAMP,
    version_number BIGINT DEFAULT 1,
    record_status BIGINT DEFAULT 1
);

-- Create master_template_definition table
CREATE TABLE master_template_definition (
    template_id UUID PRIMARY KEY,
    version INT,
    legacy_template_id VARCHAR(255),
    template_name VARCHAR(255),
    description TEXT,
    line_of_business VARCHAR(255),
    category VARCHAR(255),
    doc_type VARCHAR(255),
    language_code VARCHAR(50),
    owning_dept VARCHAR(255),
    notification_needed BOOLEAN,
    doc_supporting_data JSONB,
    is_regulatory BOOLEAN,
    is_message_center_doc BOOLEAN,
    document_channel JSONB,
    template_variables JSONB,
    template_status VARCHAR(50),
    effective_date BIGINT,
    valid_until BIGINT,
    is_shared_document BOOLEAN DEFAULT FALSE,
    sharing_scope VARCHAR(100),
    data_extraction_schema TEXT,
    created_by VARCHAR(255),
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(255),
    updated_timestamp TIMESTAMP,
    archive_indicator BOOLEAN DEFAULT FALSE,
    archive_timestamp TIMESTAMP,
    version_number BIGINT DEFAULT 1,
    record_status BIGINT DEFAULT 1
);

-- Create indexes
CREATE INDEX idx_storage_index_account ON storage_index(account_key);
CREATE INDEX idx_storage_index_customer ON storage_index(customer_key);
CREATE INDEX idx_storage_index_template ON storage_index(template_id);
CREATE INDEX idx_storage_index_creation_date ON storage_index(doc_creation_date);
CREATE INDEX idx_template_shared ON master_template_definition(is_shared_document);
CREATE INDEX idx_template_scope ON master_template_definition(sharing_scope);
```

## 2. Insert Sample Data

```sql
-- Insert sample template
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, sharing_scope, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    1,
    'Privacy Policy 2024',
    'Annual Privacy Policy Document',
    'credit_card',
    'PrivacyPolicy',
    'PrivacyNotice',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    'all',
    'system'
);

-- Insert sample document
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440000',
    '550e8400-e29b-41d4-a716-446655440000',
    'PrivacyNotice',
    '770e8400-e29b-41d4-a716-446655440000',
    '880e8400-e29b-41d4-a716-446655440000',
    '990e8400-e29b-41d4-a716-446655440000',
    'privacy_policy_2024.pdf',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    'system'
);

-- Insert shared document template with custom rule
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, sharing_scope, data_extraction_schema, created_by
) VALUES (
    '551e8400-e29b-41d4-a716-446655440000',
    1,
    'Balance Alert Notice',
    'Low Balance Alert for Specific Customers',
    'credit_card',
    'GeneralCommunications',
    'AlertNotice',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    'custom_rule',
    '{
      "ruleType": "balance_based",
      "extractionStrategy": [{
        "id": "getBalance",
        "endpoint": {
          "url": "/accounts-service/accounts/${$input.accountId}/balance",
          "method": "GET",
          "timeout": 3000
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
    }',
    'system'
);
```

## 3. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/documenthub
    username: your_username
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

integration:
  customer-service:
    base-url: http://localhost:8081  # Update with actual URL
  account-service:
    base-url: http://localhost:8082  # Update with actual URL
  transaction-service:
    base-url: http://localhost:8083  # Update with actual URL
```

## 4. Start Redis

```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest

# Or start locally
redis-server
```

## 5. Build the Application

```bash
mvn clean package
```

## 6. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/document-hub-service-1.0.0-SNAPSHOT.jar
```

## 7. Test the Endpoint

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Get Documents
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: 123e4567-e89b-12d3-a456-426614174000" \
  -H "X-requestor-id: 123e4567-e89b-12d3-a456-426614174001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{
    "customerId": "880e8400-e29b-41d4-a716-446655440000",
    "accountId": ["770e8400-e29b-41d4-a716-446655440000"],
    "pageNumber": 1,
    "pageSize": 20
  }'
```

## 8. Monitor the Application

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Prometheus Endpoint
```bash
curl http://localhost:8080/actuator/prometheus
```

## Troubleshooting

### Issue: Database Connection Failed
- Check PostgreSQL is running: `pg_isready`
- Verify credentials in application.yml
- Check database exists: `psql -l`

### Issue: Redis Connection Failed
- Check Redis is running: `redis-cli ping`
- Verify Redis host/port in application.yml

### Issue: External Service Calls Failing
- Update external service URLs in application.yml
- For testing, you can mock external services

## Development Mode

For local development without external services, you can:

1. Mock external API responses
2. Use H2 in-memory database (change R2DBC config)
3. Disable specific features temporarily

## Next Steps

1. Review the README.md for detailed documentation
2. Check IMPLEMENTATION_SUMMARY.md for architecture details
3. Explore sample-rules/ directory for rule examples
4. Run tests: `mvn test`
5. Review logs in console for debugging

## Useful Commands

```bash
# Build without tests
mvn clean package -DskipTests

# Run specific test
mvn test -Dtest=RuleEvaluatorTest

# Clean Redis cache
redis-cli FLUSHALL

# Check application logs
tail -f logs/application.log

# Generate API documentation (if using SpringDoc)
# Access: http://localhost:8080/swagger-ui.html
```

## Support

For issues or questions, refer to:
- README.md - Complete documentation
- IMPLEMENTATION_SUMMARY.md - Architecture and components
- Sample rule files in src/main/resources/sample-rules/
