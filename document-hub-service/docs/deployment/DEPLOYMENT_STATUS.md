# Deployment Status - Document Hub Service

## ✅ Infrastructure Deployed Successfully!

### Services Running

✅ **PostgreSQL 15**
- Container: `documenthub-postgres`
- Status: **Healthy**
- Port: **5433** (mapped to internal 5432)
- Database: `documenthub`
- Username: `postgres`
- Password: `postgres123`

✅ **Redis 7**
- Container: `documenthub-redis`
- Status: **Healthy**
- Port: **6379**

### Sample Data Loaded

✅ **Templates: 7 records**
1. Privacy Policy 2024 (shared - scope: all)
2. Cardholder Agreement (shared - scope: credit_card_account_only)
3. Digital Banking User Guide (shared - scope: digital_bank_customer_only)
4. Low Balance Alert Notice (shared - scope: custom_rule)
5. Loyal Customer Rewards (shared - scope: custom_rule)
6. Monthly Statement Template (regular)
7. Payment Confirmation Letter (regular)

✅ **Documents: 7 records**
- Customer 1 (880e8400-e29b-41d4-a716-446655440001)
  - Account 1: 4 documents (3 statements + 1 payment)
  - Account 2: 1 document (1 statement)
- Customer 2 (880e8400-e29b-41d4-a716-446655440002)
  - Account 3: 2 documents (1 statement + 1 payment)

## Next Step: Build and Run Application

### Option 1: Using Maven (if installed)

```bash
# Navigate to project directory
cd document-hub-service

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

### Option 2: Using IDE (Recommended)

**IntelliJ IDEA:**
1. Open the project in IntelliJ IDEA
2. Right-click on `DocumentHubApplication.java`
3. Select "Run 'DocumentHubApplication'"

**VS Code:**
1. Open the project in VS Code
2. Install "Extension Pack for Java"
3. Open `DocumentHubApplication.java`
4. Click "Run" above the main method

**Eclipse:**
1. Import as Maven project
2. Right-click on project → Run As → Spring Boot App

### Option 3: Install Maven

**Windows:**
```powershell
# Using Chocolatey
choco install maven

# Or download from: https://maven.apache.org/download.cgi
```

**Linux:**
```bash
sudo apt-get install maven
```

**Mac:**
```bash
brew install maven
```

## Verify Deployment

### 1. Check Containers
```bash
docker ps
```

Expected output:
```
CONTAINER ID   IMAGE                COMMAND                  CREATED   STATUS                 PORTS                    NAMES
xxxxxxxxxx     redis:7-alpine       "docker-entrypoint.s…"   X min     Up X min (healthy)     0.0.0.0:6379->6379/tcp   documenthub-redis
xxxxxxxxxx     postgres:15-alpine   "docker-entrypoint.s…"   X min     Up X min (healthy)     0.0.0.0:5433->5432/tcp   documenthub-postgres
```

### 2. Check Database
```bash
docker exec documenthub-postgres psql -U postgres -d documenthub -c "SELECT COUNT(*) FROM master_template_definition;"
```

Expected: 7

```bash
docker exec documenthub-postgres psql -U postgres -d documenthub -c "SELECT COUNT(*) FROM storage_index;"
```

Expected: 7

### 3. Check Redis
```bash
docker exec documenthub-redis redis-cli PING
```

Expected: PONG

## Test the Application

Once the application is running on port 8080:

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Get Documents for Customer 1, Account 1
```bash
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-001" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"],\"pageNumber\":1,\"pageSize\":20}"
```

Expected: Returns 4 account-specific documents + shared documents

### Get Documents for Customer 1, All Accounts
```bash
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-002" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"pageNumber\":1,\"pageSize\":20}"
```

Expected: Returns 5 account-specific documents (from both accounts) + shared documents

## Database Connection Details

For manual connection or database tools:

```
Host: localhost
Port: 5433
Database: documenthub
Username: postgres
Password: postgres123
```

### Using psql
```bash
psql -h localhost -p 5433 -U postgres -d documenthub
```

### Using pgAdmin
- Add New Server
- Host: localhost
- Port: 5433
- Database: documenthub
- Username: postgres
- Password: postgres123

## Sample Data Reference

### Customer & Account IDs

**Customer 1:**
- ID: `880e8400-e29b-41d4-a716-446655440001`
- Accounts:
  - Account 1: `770e8400-e29b-41d4-a716-446655440001` (4 documents)
  - Account 2: `770e8400-e29b-41d4-a716-446655440002` (1 document)

**Customer 2:**
- ID: `880e8400-e29b-41d4-a716-446655440002`
- Accounts:
  - Account 3: `770e8400-e29b-41d4-a716-446655440003` (2 documents)

### Useful Queries

```sql
-- View all templates
SELECT template_name, is_shared_document, sharing_scope
FROM master_template_definition
ORDER BY template_name;

-- View all documents with template info
SELECT s.file_name, s.account_key, s.customer_key, t.template_name, s.doc_creation_date
FROM storage_index s
JOIN master_template_definition t ON s.template_id = t.template_id
ORDER BY s.doc_creation_date DESC;

-- View shared documents with custom rules
SELECT template_name, sharing_scope,
       CASE WHEN data_extraction_schema IS NOT NULL THEN 'Has Custom Rule' ELSE '' END as has_rule
FROM master_template_definition
WHERE is_shared_document = TRUE;

-- Count documents per customer
SELECT customer_key, COUNT(*) as document_count
FROM storage_index
GROUP BY customer_key;

-- Count documents per account
SELECT account_key, COUNT(*) as document_count
FROM storage_index
GROUP BY account_key;
```

## Troubleshooting

### Containers Not Running
```bash
# Check container status
docker ps -a

# View logs
docker logs documenthub-postgres
docker logs documenthub-redis

# Restart containers
docker-compose restart
```

### Database Connection Failed
```bash
# Check if database is accessible
docker exec documenthub-postgres pg_isready -U postgres

# Check database exists
docker exec documenthub-postgres psql -U postgres -c "\l"
```

### Reset Everything
```bash
# Stop and remove containers, volumes
docker-compose down -v

# Start fresh
docker-compose up -d

# Wait for initialization
# Check data loaded
```

## Management Commands

### Docker Commands
```bash
# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Restart services
docker-compose restart

# Remove everything (including data)
docker-compose down -v
```

### Database Commands
```bash
# Connect to database
docker exec -it documenthub-postgres psql -U postgres -d documenthub

# Backup database
docker exec documenthub-postgres pg_dump -U postgres documenthub > backup.sql

# View database size
docker exec documenthub-postgres psql -U postgres -d documenthub -c "SELECT pg_size_pretty(pg_database_size('documenthub'));"
```

### Redis Commands
```bash
# Connect to Redis
docker exec -it documenthub-redis redis-cli

# Check keys
docker exec documenthub-redis redis-cli KEYS "*"

# Clear cache
docker exec documenthub-redis redis-cli FLUSHALL
```

## Next Steps

1. ✅ Infrastructure deployed (PostgreSQL + Redis)
2. ✅ Sample data loaded (7 templates + 7 documents)
3. ⏭️ Build application (using Maven or IDE)
4. ⏭️ Run application
5. ⏭️ Test API endpoints
6. ⏭️ Review logs
7. ⏭️ Test custom rule scenarios

## Project Location

```
C:\Users\ghmd8\Documents\AI\document-hub-service
```

## Key Files

- `README.md` - Complete documentation
- `QUICKSTART.md` - Quick start guide
- `DEPLOYMENT_GUIDE.md` - Detailed deployment instructions
- `MANUAL_SETUP.md` - Manual setup without Docker
- `docker-compose.yml` - Docker configuration
- `database_init/` - Database scripts with sample data
- `src/main/java/com/documenthub/` - Application source code
- `src/main/resources/application.yml` - Application configuration

## Support

For issues:
1. Check logs: `docker-compose logs -f`
2. Verify containers: `docker ps`
3. Check database: Connect with psql
4. Review README.md and DEPLOYMENT_GUIDE.md

---

**Status:** ✅ Infrastructure Ready | ⏳ Application Build Pending

**Last Updated:** 2025-11-05
