@echo off
REM ============================================================================
REM Document Hub Service - API Test Script (Windows)
REM Tests the deployed Document Hub Service endpoints
REM ============================================================================

echo ============================================
echo Testing Document Hub Service API
echo ============================================
echo.

echo [1] Testing Health Endpoint...
curl -s http://localhost:8080/actuator/health
echo.
echo.

echo [2] Testing Document Enquiry - Customer 1, Account 1...
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-001" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"],\"pageNumber\":1,\"pageSize\":20}"
echo.
echo.

echo [3] Testing Document Enquiry - Customer 1, All Accounts...
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-002" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"pageNumber\":1,\"pageSize\":20}"
echo.
echo.

echo [4] Testing Document Enquiry - Customer 2...
curl -X POST http://localhost:8080/documents-enquiry ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-003" ^
  -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440002\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440003\"],\"pageNumber\":1,\"pageSize\":20}"
echo.
echo.

echo [5] Testing Metrics Endpoint...
curl -s http://localhost:8080/actuator/metrics | head -n 20
echo.
echo.

echo ============================================
echo API Tests Complete!
echo ============================================
echo.

pause
