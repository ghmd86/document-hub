@echo off
REM ============================================================================
REM Complete Test Suite with Mock Services
REM Tests Document Hub Service with Mock External APIs
REM ============================================================================

echo ======================================================
echo Document Hub Service - Complete Test Suite
echo ======================================================
echo.

echo [TEST SETUP]
echo This test suite will:
echo 1. Verify mock services are running
echo 2. Test all mock API endpoints
echo 3. Test Document Hub Service with custom rules
echo 4. Verify rule evaluation for all scenarios
echo.

REM Check if mock service is running
echo [1/6] Checking if mock services are running on port 8090...
curl -s http://localhost:8090/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Mock services not running!
    echo Please start mock services first:
    echo   cd mock-services
    echo   mvn spring-boot:run
    pause
    exit /b 1
)
echo ✓ Mock services are running
echo.

REM Check if Document Hub Service is running
echo [2/6] Checking if Document Hub Service is running on port 8080...
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Document Hub Service not running!
    echo Please start the service with mock profile:
    echo   mvn spring-boot:run -Dspring-boot.run.profiles=mock
    pause
    exit /b 1
)
echo ✓ Document Hub Service is running
echo.

echo ======================================================
echo TESTING MOCK APIS
echo ======================================================
echo.

echo [3/6] Testing Customer Service Mock APIs...
echo.
echo --- Customer 1 Profile (8 years tenure, CA zipcode 90001) ---
curl -s http://localhost:8090/customer-service/customers/880e8400-e29b-41d4-a716-446655440001/profile | jq .
echo.
echo.

echo --- Customer 2 Profile (2 years tenure, NY zipcode 10001) ---
curl -s http://localhost:8090/customer-service/customers/880e8400-e29b-41d4-a716-446655440002/profile | jq .
echo.
echo.

echo [4/6] Testing Account Service Mock APIs...
echo.
echo --- Account 1 Balance ($4,500 - qualifies for low balance rule) ---
curl -s http://localhost:8090/accounts-service/accounts/770e8400-e29b-41d4-a716-446655440001/balance | jq .
echo.
echo.

echo --- Account 2 Balance ($12,500 - does NOT qualify for low balance rule) ---
curl -s http://localhost:8090/accounts-service/accounts/770e8400-e29b-41d4-a716-446655440002/balance | jq .
echo.
echo.

echo [5/6] Testing Transaction Service Mock APIs...
echo.
echo --- Account 1 Transaction Summary (has international activity) ---
curl -s "http://localhost:8090/transaction-service/accounts/770e8400-e29b-41d4-a716-446655440001/transactions/summary?period=last_90_days&type=international" | jq .
echo.
echo.

echo ======================================================
echo TESTING DOCUMENT HUB SERVICE WITH CUSTOM RULES
echo ======================================================
echo.

echo [6/6] Testing Document Enquiry with Custom Rule Evaluation...
echo.

echo --- Test 1: Customer 1, Account 1 ---
echo Expected: Account documents + Shared documents (should include low balance alert)
echo Customer: John (8 years, CA) | Account: Low balance ($4,500)
echo Rules that should match: tenure>5yrs ✓, balance<$5000 ✓, zipcode in CA ✓
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-rule-001" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"],\"pageNumber\":1,\"pageSize\":20}" | jq .
echo.
echo.

echo --- Test 2: Customer 1, Account 2 ---
echo Expected: Account documents + Shared documents (NO low balance alert)
echo Customer: John (8 years, CA) | Account: High balance ($12,500)
echo Rules that should match: tenure>5yrs ✓, balance<$5000 ✗, zipcode in CA ✓
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-rule-002" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440002\"],\"pageNumber\":1,\"pageSize\":20}" | jq .
echo.
echo.

echo --- Test 3: Customer 1, All Accounts ---
echo Expected: All account documents (5 total) + Shared documents
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-rule-003" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"pageNumber\":1,\"pageSize\":20}" | jq .
echo.
echo.

echo --- Test 4: Customer 2, Account 3 ---
echo Expected: Account documents + Shared documents (digital banking, NO loyalty rewards)
echo Customer: Jane (2 years, NY) | Account: Medium balance ($8,200)
echo Rules that should match: tenure>5yrs ✗, balance<$5000 ✗, digital_banking ✓
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-rule-004" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440002\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440003\"],\"pageNumber\":1,\"pageSize\":20}" | jq .
echo.
echo.

echo ======================================================
echo TEST SUMMARY
echo ======================================================
echo.
echo ✓ Mock services tested
echo ✓ Document Hub Service tested
echo ✓ Custom rule scenarios tested
echo.
echo Review the output above to verify:
echo   - Customer 1 + Account 1: Should get low balance alert (balance $4,500)
echo   - Customer 1 + Account 2: Should NOT get low balance alert (balance $12,500)
echo   - Customer 1: Should get loyalty rewards (8 years tenure)
echo   - Customer 2: Should NOT get loyalty rewards (only 2 years)
echo   - Customer 2: Should get digital banking guide
echo.
echo Check Document Hub Service logs for rule evaluation details
echo.

pause
