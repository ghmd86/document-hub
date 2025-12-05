@echo off
echo ########################################
echo # Document Hub POC - All Tests
echo ########################################
echo.

set HEADERS=-H "Content-Type: application/json" -H "X-version: 1" -H "X-correlation-id: test-all-001" -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" -H "X-requestor-type: CUSTOMER"

echo ----------------------------------------
echo TEST 1: Disclosure Code D164 (VIP)
echo Expected: Credit_Card_Terms_D164_v1.pdf
echo ----------------------------------------
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-all-d164" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000001\"]}"
echo.
echo.

echo ----------------------------------------
echo TEST 2: Disclosure Code D166 (Standard)
echo Expected: Premium_Credit_Card_Terms_D166_v1.pdf
echo ----------------------------------------
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-all-d166" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000002\"]}"
echo.
echo.

echo ----------------------------------------
echo TEST 3: Conditional - Platinum ($75k)
echo Expected: Platinum_Card_Agreement.pdf
echo ----------------------------------------
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-all-platinum" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000001\"]}"
echo.
echo.

echo ----------------------------------------
echo TEST 4: Conditional - Gold ($35k)
echo Expected: Gold_Card_Agreement.pdf
echo ----------------------------------------
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-all-gold" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000002\"]}"
echo.
echo.

echo ----------------------------------------
echo TEST 5: Conditional - Standard ($15k)
echo Expected: Standard_Card_Agreement.pdf
echo ----------------------------------------
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-all-standard" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000003\"]}"
echo.
echo.

echo ########################################
echo # Tests Complete
echo ########################################
pause
