@echo off
echo ========================================
echo TEST: Conditional Matching - STANDARD
echo ========================================
echo Account: aaaa0000-0000-0000-0000-000000000003
echo Credit Limit: $15,000 (< $25,000)
echo Expected: Standard_Card_Agreement.pdf
echo ========================================
echo.

curl -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-standard-001" ^
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000003\"]}"

echo.
echo ========================================
pause
