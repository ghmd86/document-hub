@echo off
echo ========================================
echo TEST: Disclosure Code Matching - D164
echo ========================================
echo Account: aaaa0000-0000-0000-0000-000000000001 (VIP)
echo Expected: Credit_Card_Terms_D164_v1.pdf
echo ========================================
echo.

curl -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-d164-001" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000001\"]}"

echo.
echo ========================================
pause
