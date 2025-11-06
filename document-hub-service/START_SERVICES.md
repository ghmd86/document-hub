# Start Services Guide

## Prerequisites Check

✅ Docker Desktop is running (verified)
✅ PostgreSQL container is running on port 5433 (verified)
✅ Redis container is running on port 6379 (verified)
✅ Java 17 is installed (verified)
⚠️ Maven needs to be available or use IDE

## Option 1: Using IDE (RECOMMENDED)

This is the easiest way to start both services for testing.

### Step 1: Start Mock Services in IDE

**IntelliJ IDEA / Eclipse / VS Code:**
1. Open project: `C:\Users\ghmd8\Documents\AI\document-hub-service`
2. Navigate to: `mock-services/src/main/java/com/documenthub/mock/MockServicesApplication.java`
3. Right-click on the file → **Run 'MockServicesApplication'** (or click the Run button)
4. Wait for "Started MockServicesApplication" message
5. Verify: http://localhost:8090/actuator/health should return `{"status":"UP"}`

**Expected Output:**
```
2024-XX-XX XX:XX:XX [MOCK-API] Started MockServicesApplication in X.XXX seconds
```

### Step 2: Start Document Hub Service in IDE

**IntelliJ IDEA / Eclipse / VS Code:**
1. Navigate to: `src/main/java/com/documenthub/DocumentHubApplication.java`
2. **IMPORTANT:** Set active profile to `mock`

   **IntelliJ:**
   - Edit Run Configuration
   - Add VM option: `-Dspring.profiles.active=mock`
   - Or set Environment variable: `SPRING_PROFILES_ACTIVE=mock`

   **VS Code:**
   - Edit launch.json
   - Add: `"env": {"SPRING_PROFILES_ACTIVE": "mock"}`

   **Eclipse:**
   - Run Configurations → Arguments
   - Program arguments: `--spring.profiles.active=mock`

3. Right-click → **Run 'DocumentHubApplication'**
4. Wait for "Started DocumentHubApplication" message
5. Verify: http://localhost:8080/actuator/health should return `{"status":"UP"}`

**Expected Output:**
```
2024-XX-XX XX:XX:XX Started DocumentHubApplication in X.XXX seconds
The following 1 profile is active: "mock"
```

---

## Option 2: Using Command Line (Requires Maven)

If Maven is installed and in PATH:

### Terminal 1: Start Mock Services
```bash
cd C:\Users\ghmd8\Documents\AI\document-hub-service\mock-services
mvn spring-boot:run
```

### Terminal 2: Start Document Hub Service
```bash
cd C:\Users\ghmd8\Documents\AI\document-hub-service
mvn spring-boot:run -Dspring-boot.run.profiles=mock
```

---

## Option 3: Install Maven First

If you want to use command line but don't have Maven:

### Install Maven
```powershell
# Using Chocolatey (if installed)
choco install maven

# Or download manually from:
# https://maven.apache.org/download.cgi
```

After installation:
- Restart your terminal
- Run: `mvn -version` to verify

---

## Verification Checklist

After starting both services, verify they're running:

### 1. Check Infrastructure
```bash
docker ps
```
Expected: `documenthub-postgres` and `documenthub-redis` showing as healthy

### 2. Check Mock Services (Port 8090)
```bash
curl http://localhost:8090/actuator/health
```
Expected: `{"status":"UP"}`

Test customer API:
```bash
curl http://localhost:8090/customer-service/customers/880e8400-e29b-41d4-a716-446655440001/profile
```
Expected: Customer profile JSON

### 3. Check Document Hub Service (Port 8080)
```bash
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}`

### 4. Check Database Connection
Look for this in Document Hub Service logs:
```
Successfully acquired connection from r2dbc pool
```

### 5. Test Integration
```bash
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-001" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"],\"pageNumber\":1,\"pageSize\":20}"
```

Expected: JSON response with documents

---

## What's Running

| Service | Port | Status | Purpose |
|---------|------|--------|---------|
| PostgreSQL | 5433 | ✅ Running | Database with sample data |
| Redis | 6379 | ✅ Running | Caching |
| Mock Services | 8090 | Start this | External API mocks |
| Document Hub | 8080 | Start this | Main application |

---

## Troubleshooting

### Port Already in Use

**Check ports:**
```bash
# Windows
netstat -ano | findstr :8090
netstat -ano | findstr :8080

# Kill process if needed
taskkill /PID <PID> /F
```

### Mock Service Won't Start

**Check logs for:**
- Port 8090 availability
- Java version (should be 17+)

**Fix:**
- Close any process using port 8090
- Verify Java 17+ is installed

### Document Hub Service Won't Start

**Common issues:**

1. **Database connection failed**
   - Verify: `docker ps` shows postgres as healthy
   - Check: `docker logs documenthub-postgres`

2. **Redis connection failed**
   - Verify: `docker ps` shows redis as healthy
   - Test: `docker exec documenthub-redis redis-cli PING`

3. **Mock profile not active**
   - Check logs for: `The following 1 profile is active: "mock"`
   - If not, add: `-Dspring.profiles.active=mock`

4. **Can't connect to mock services**
   - Verify mock services are running on 8090
   - Test: `curl http://localhost:8090/actuator/health`

### See Detailed Logs

**Enable debug logging:**

Add to `application.yml` or set as VM argument:
```
-Dlogging.level.com.documenthub=DEBUG
-Dlogging.level.com.documenthub.rules=TRACE
```

---

## Quick Test After Startup

Once both services are running, test a custom rule scenario:

```bash
# This should trigger the low balance rule (Account 1 has $4,500)
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-low-balance" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"]}"
```

**Check Document Hub logs for:**
```
[CustomRuleEngine] Executing data source: getBalance
[CustomRuleEngine] Extracted field currentBalance: 4500.0
[RuleEvaluator] Evaluating rule: currentBalance lessThan 5000
[RuleEvaluator] Rule evaluation result: true
```

This confirms:
1. Document Hub called Mock Service ✓
2. Mock Service returned balance $4,500 ✓
3. Custom rule evaluated successfully ✓
4. Rule matched (balance < $5000) ✓

---

## Next Steps

After both services are running:

1. Run comprehensive test suite:
   ```bash
   test-with-mocks.bat
   ```

2. Try different scenarios:
   - Low balance customer (Account 1)
   - High balance customer (Account 2)
   - Loyal customer (8 years tenure)
   - New customer (2 years tenure)
   - Different locations (CA vs NY)

3. Check logs to see rule evaluation in action

4. Review `MOCK_SERVICES_GUIDE.md` for all test scenarios

---

## Summary

**To start testing:**
1. ✅ Infrastructure already running (PostgreSQL + Redis)
2. ⏭️ Start Mock Services on port 8090 (use IDE)
3. ⏭️ Start Document Hub on port 8080 with `mock` profile (use IDE)
4. ⏭️ Run tests

**Recommended: Use IDE** - It's the easiest way to start and debug both services!

**All files ready at:** `C:\Users\ghmd8\Documents\AI\document-hub-service\`
