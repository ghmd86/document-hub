@echo off
echo ========================================
echo Test Document Enquiry - All Accounts
echo ========================================

set CUSTOMER_ID=cccc0000-0000-0000-0000-000000000001

echo.
echo === Account 1 (aaaa0000-0000-0000-0000-000000000001) ===
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-acc1" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"aaaa0000-0000-0000-0000-000000000001\"],\"pageNumber\":0,\"pageSize\":5}" | findstr /C:"totalItems" /C:"lineOfBusiness"

echo.
echo === Account 2 (aaaa0000-0000-0000-0000-000000000002) ===
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-acc2" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"aaaa0000-0000-0000-0000-000000000002\"],\"pageNumber\":0,\"pageSize\":5}" | findstr /C:"totalItems" /C:"lineOfBusiness"

echo.
echo === Account 3 (aaaa0000-0000-0000-0000-000000000003) ===
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-acc3" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"aaaa0000-0000-0000-0000-000000000003\"],\"pageNumber\":0,\"pageSize\":5}" | findstr /C:"totalItems" /C:"lineOfBusiness"

echo.
echo ========================================
pause
