# Manual Setup Guide (Without Docker)

If you prefer not to use Docker or Docker is not available, follow this guide to set up PostgreSQL and Redis manually.

## Prerequisites

- PostgreSQL 14+ installed
- Redis 6+ installed
- Java 17+ installed
- Maven 3.6+ installed

## Step 1: PostgreSQL Setup

### Install PostgreSQL
Download and install PostgreSQL from: https://www.postgresql.org/download/

### Configure PostgreSQL

1. **Start PostgreSQL service**
   ```bash
   # Windows (as Administrator)
   net start postgresql-x64-14

   # Linux
   sudo systemctl start postgresql

   # Mac
   brew services start postgresql
   ```

2. **Create database and user**
   ```bash
   # Connect as postgres user
   psql -U postgres
   ```

   ```sql
   -- Create database
   CREATE DATABASE documenthub;

   -- Create user (optional, can use postgres user)
   CREATE USER documenthub_user WITH PASSWORD 'postgres123';

   -- Grant privileges
   GRANT ALL PRIVILEGES ON DATABASE documenthub TO documenthub_user;

   -- Exit
   \q
   ```

### Run Database Scripts

1. **Connect to documenthub database**
   ```bash
   psql -U postgres -d documenthub
   ```

2. **Run table creation script**
   ```sql
   \i 'C:/Users/ghmd8/Documents/AI/document-hub-service/database_init/01-create-tables.sql'
   ```

3. **Run sample data script**
   ```sql
   \i 'C:/Users/ghmd8/Documents/AI/document-hub-service/database_init/02-insert-sample-data.sql'
   ```

4. **Verify data**
   ```sql
   -- Check templates
   SELECT COUNT(*) FROM master_template_definition;
   -- Expected: 7

   -- Check documents
   SELECT COUNT(*) FROM storage_index;
   -- Expected: 7

   -- Exit
   \q
   ```

### Update PostgreSQL Port (if needed)

If your PostgreSQL is running on the default port 5432, update `application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/documenthub  # Change from 5433 to 5432
    username: postgres
    password: postgres  # Or your password
```

## Step 2: Redis Setup

### Install Redis

**Windows:**
- Download Redis for Windows: https://github.com/microsoftarchive/redis/releases
- Or use WSL: `wsl --install` then `sudo apt-get install redis-server`

**Linux:**
```bash
sudo apt-get update
sudo apt-get install redis-server
sudo systemctl start redis
```

**Mac:**
```bash
brew install redis
brew services start redis
```

### Test Redis
```bash
redis-cli ping
# Expected response: PONG
```

## Step 3: Update Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/documenthub  # Your PostgreSQL port
    username: postgres  # Your PostgreSQL username
    password: postgres  # Your PostgreSQL password

  data:
    redis:
      host: localhost
      port: 6379  # Your Redis port
```

## Step 4: Build and Run Application

### Build
```bash
cd document-hub-service

# Windows
mvn clean package

# Linux/Mac
./mvnw clean package
```

### Run
```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using JAR
java -jar target/document-hub-service-1.0.0-SNAPSHOT.jar
```

## Step 5: Verify Deployment

### Check Application Started
Look for this in the logs:
```
Started DocumentHubApplication in X.XXX seconds
```

### Test Health Endpoint
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Test API
```bash
curl -X POST http://localhost:8080/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-001" \
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" \
  -H "X-requestor-type: CUSTOMER" \
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"],\"pageNumber\":1,\"pageSize\":20}"
```

## Database Verification Queries

```sql
-- Connect to database
psql -U postgres -d documenthub

-- List all templates
SELECT template_id, template_name, is_shared_document, sharing_scope
FROM master_template_definition
ORDER BY template_name;

-- List all documents
SELECT s.file_name, s.account_key, s.customer_key, t.template_name
FROM storage_index s
JOIN master_template_definition t ON s.template_id = t.template_id
ORDER BY s.doc_creation_date DESC;

-- Check shared documents
SELECT template_name, sharing_scope,
       CASE WHEN data_extraction_schema IS NOT NULL THEN 'Has Custom Rule' ELSE '' END
FROM master_template_definition
WHERE is_shared_document = TRUE;
```

## Sample Data Overview

### Customers & Accounts
- **Customer 1:** `880e8400-e29b-41d4-a716-446655440001`
  - Account 1: `770e8400-e29b-41d4-a716-446655440001` (4 documents)
  - Account 2: `770e8400-e29b-41d4-a716-446655440002` (1 document)

- **Customer 2:** `880e8400-e29b-41d4-a716-446655440002`
  - Account 3: `770e8400-e29b-41d4-a716-446655440003` (2 documents)

### Templates
1. Privacy Policy (shared - all)
2. Cardholder Agreement (shared - credit card only)
3. Digital Banking Guide (shared - digital banking only)
4. Low Balance Alert (shared - custom rule: balance < $5000)
5. Loyal Customer Rewards (shared - custom rule: tenure > 5 years)
6. Monthly Statement (regular)
7. Payment Confirmation (regular)

## Troubleshooting

### PostgreSQL Connection Issues
1. Check if PostgreSQL is running
   ```bash
   # Windows
   sc query postgresql-x64-14

   # Linux
   sudo systemctl status postgresql
   ```

2. Check PostgreSQL port
   ```bash
   psql -U postgres -c "SHOW port;"
   ```

3. Verify credentials in application.yml

### Redis Connection Issues
1. Check if Redis is running
   ```bash
   redis-cli ping
   ```

2. Check Redis port
   ```bash
   redis-cli CONFIG GET port
   ```

### Application Startup Issues
1. Check Java version
   ```bash
   java -version
   # Should be Java 17+
   ```

2. Check port 8080 is not in use
   ```bash
   # Windows
   netstat -ano | findstr :8080

   # Linux/Mac
   lsof -i :8080
   ```

3. Check application logs for errors

## Alternative: Using Docker (Recommended)

If possible, using Docker is much simpler:

1. Install Docker Desktop: https://www.docker.com/products/docker-desktop
2. Start Docker Desktop
3. Run: `docker-compose up -d`
4. Wait 30 seconds
5. Run: `mvn spring-boot:run`

Docker automatically:
- Sets up PostgreSQL on custom port 5433
- Sets up Redis on port 6379
- Creates database and tables
- Inserts sample data
- Handles all configuration
