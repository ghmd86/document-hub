# Deployment Guide - Document Hub Service

## Quick Deployment (Automated)

### Windows
```bash
# Run the deployment script
deploy.bat

# Start the application
mvn spring-boot:run
```

### Linux/Mac
```bash
# Make script executable
chmod +x deploy.sh

# Run the deployment script
./deploy.sh

# Start the application
./mvnw spring-boot:run
```

## What Gets Deployed

### Infrastructure (via Docker Compose)
- **PostgreSQL 15** on port **5433** (custom port to avoid conflicts)
- **Redis 7** on port **6379**

### Database Schema
- `master_template_definition` table (7 templates)
- `storage_index` table (7 documents)
- All necessary indexes for performance

### Sample Data Includes

#### Templates (7 total):
1. **Privacy Policy 2024** - Shared (scope: all)
2. **Cardholder Agreement** - Shared (scope: credit_card_account_only)
3. **Digital Banking User Guide** - Shared (scope: digital_bank_customer_only)
4. **Low Balance Alert** - Shared (scope: custom_rule - balance < $5000)
5. **Loyal Customer Rewards** - Shared (scope: custom_rule - tenure > 5 years)
6. **Monthly Statement Template** - Regular template
7. **Payment Confirmation Letter** - Regular template

#### Documents (7 total):
- **Customer 1** (ID: 880e8400-e29b-41d4-a716-446655440001)
  - **Account 1** (ID: 770e8400-e29b-41d4-a716-446655440001)
    - 3 Monthly Statements (Jan, Feb, Mar 2024)
    - 1 Payment Confirmation
  - **Account 2** (ID: 770e8400-e29b-41d4-a716-446655440002)
    - 1 Monthly Statement (Mar 2024)

- **Customer 2** (ID: 880e8400-e29b-41d4-a716-446655440002)
  - **Account 3** (ID: 770e8400-e29b-41d4-a716-446655440003)
    - 1 Monthly Statement (Mar 2024)
    - 1 Payment Confirmation

## Manual Deployment Steps

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Verify Database
```bash
# Connect to PostgreSQL
docker exec -it documenthub-postgres psql -U postgres -d documenthub

# Check tables
\dt

# Count records
SELECT COUNT(*) FROM master_template_definition;
SELECT COUNT(*) FROM storage_index;

# Exit
\q
```

### 3. Verify Redis
```bash
# Connect to Redis
docker exec -it documenthub-redis redis-cli

# Test connection
PING

# Exit
exit
```

### 4. Build Application
```bash
# Windows
mvn clean package

# Linux/Mac
./mvnw clean package
```

### 5. Run Application
```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using JAR
java -jar target/document-hub-service-1.0.0-SNAPSHOT.jar
```

## Configuration Details

### PostgreSQL Connection
```yaml
Host: localhost
Port: 5433  # Custom port to avoid conflicts
Database: documenthub
Username: postgres
Password: postgres123
```

### Redis Connection
```yaml
Host: localhost
Port: 6379
```

### Application
```yaml
Port: 8080
Context Path: /
```

## Testing the Deployment

### 1. Health Check
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 2. Test Document Enquiry - Customer 1, Account 1
```bash
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

**Expected**: Should return 4 account-specific documents + shared documents

### 3. Test Document Enquiry - Customer 1, All Accounts
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-002" \
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{
    "customerId": "880e8400-e29b-41d4-a716-446655440001",
    "pageNumber": 1,
    "pageSize": 20
  }'
```

**Expected**: Should return 5 account-specific documents (from 2 accounts) + shared documents

### 4. Test Document Enquiry - Customer 2
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-003" \
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440002" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{
    "customerId": "880e8400-e29b-41d4-a716-446655440002",
    "accountId": ["770e8400-e29b-41d4-a716-446655440003"],
    "pageNumber": 1,
    "pageSize": 20
  }'
```

**Expected**: Should return 2 account-specific documents + shared documents

## Verifying Sample Data

### Check Templates
```sql
-- Connect to database
docker exec -it documenthub-postgres psql -U postgres -d documenthub

-- List all templates
SELECT template_id, template_name, is_shared_document, sharing_scope
FROM master_template_definition
ORDER BY template_name;

-- Check shared document templates
SELECT template_id, template_name, sharing_scope,
       CASE WHEN data_extraction_schema IS NOT NULL THEN 'Has Custom Rule' ELSE 'No Custom Rule' END as rule_status
FROM master_template_definition
WHERE is_shared_document = TRUE;
```

### Check Documents
```sql
-- List all documents
SELECT s.storage_index_id, s.file_name, s.account_key, s.customer_key,
       t.template_name, s.doc_creation_date
FROM storage_index s
JOIN master_template_definition t ON s.template_id = t.template_id
ORDER BY s.doc_creation_date DESC;

-- Count documents by customer
SELECT customer_key, COUNT(*) as document_count
FROM storage_index
WHERE is_accessible = TRUE
GROUP BY customer_key;

-- Count documents by account
SELECT account_key, COUNT(*) as document_count
FROM storage_index
WHERE is_accessible = TRUE
GROUP BY account_key;
```

## Troubleshooting

### Port 5433 Already in Use
```bash
# Check what's using the port
netstat -ano | findstr :5433  # Windows
lsof -i :5433                 # Linux/Mac

# Option 1: Stop the conflicting service
# Option 2: Change port in docker-compose.yml and application.yml
```

### Database Connection Failed
```bash
# Check if container is running
docker ps

# Check container logs
docker logs documenthub-postgres

# Restart container
docker-compose restart postgres
```

### Redis Connection Failed
```bash
# Check if container is running
docker ps

# Check container logs
docker logs documenthub-redis

# Restart container
docker-compose restart redis
```

### Application Won't Start
```bash
# Check application logs
# Look for error messages in console output

# Common issues:
# 1. Database not ready - wait 30 seconds after docker-compose up
# 2. Wrong credentials - check application.yml
# 3. Port 8080 in use - change server.port in application.yml
```

## Useful Commands

### Docker Management
```bash
# View running containers
docker ps

# View all containers (including stopped)
docker ps -a

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Restart services
docker-compose restart
```

### Database Management
```bash
# Connect to database
docker exec -it documenthub-postgres psql -U postgres -d documenthub

# Backup database
docker exec documenthub-postgres pg_dump -U postgres documenthub > backup.sql

# Restore database
docker exec -i documenthub-postgres psql -U postgres documenthub < backup.sql

# Reset database (WARNING: Deletes all data)
docker exec -it documenthub-postgres psql -U postgres -c "DROP DATABASE documenthub;"
docker exec -it documenthub-postgres psql -U postgres -c "CREATE DATABASE documenthub;"
docker-compose restart postgres
```

### Redis Management
```bash
# Connect to Redis
docker exec -it documenthub-redis redis-cli

# Check keys
KEYS *

# Clear all cache
FLUSHALL

# Get specific key
GET customer:profile:880e8400-e29b-41d4-a716-446655440001
```

## Monitoring

### Application Endpoints
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:8080/actuator/prometheus

### Database Monitoring
```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'documenthub';

-- Table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

## Next Steps

1. ✅ Deployment complete
2. ✅ Sample data loaded
3. ⏭️ Test API endpoints
4. ⏭️ Configure external service URLs (if needed)
5. ⏭️ Set up monitoring/alerting
6. ⏭️ Configure production credentials
7. ⏭️ Set up SSL/TLS
8. ⏭️ Configure load balancer

## Production Considerations

Before deploying to production:

1. **Change default passwords** in docker-compose.yml and application.yml
2. **Configure SSL/TLS** for PostgreSQL and Redis
3. **Set up proper backup** strategy
4. **Configure monitoring** and alerting
5. **Review and adjust** connection pool settings
6. **Set up log aggregation** (ELK stack)
7. **Configure rate limiting**
8. **Enable authentication/authorization**
9. **Review security settings**
10. **Load test** the application

## Sample Data Reference

### Customer IDs
- Customer 1: `880e8400-e29b-41d4-a716-446655440001`
- Customer 2: `880e8400-e29b-41d4-a716-446655440002`

### Account IDs
- Account 1 (Customer 1): `770e8400-e29b-41d4-a716-446655440001`
- Account 2 (Customer 1): `770e8400-e29b-41d4-a716-446655440002`
- Account 3 (Customer 2): `770e8400-e29b-41d4-a716-446655440003`

### Template IDs
- Privacy Policy: `550e8400-e29b-41d4-a716-446655440001`
- Cardholder Agreement: `550e8400-e29b-41d4-a716-446655440002`
- Digital Banking Guide: `550e8400-e29b-41d4-a716-446655440003`
- Low Balance Alert: `550e8400-e29b-41d4-a716-446655440004`
- Loyal Customer Rewards: `550e8400-e29b-41d4-a716-446655440005`
- Monthly Statement: `550e8400-e29b-41d4-a716-446655440006`
- Payment Confirmation: `550e8400-e29b-41d4-a716-446655440007`
