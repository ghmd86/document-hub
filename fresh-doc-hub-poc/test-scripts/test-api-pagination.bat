@echo off
echo ========================================
echo Test Document Enquiry API - Pagination
echo ========================================

set CUSTOMER_ID=cccc0000-0000-0000-0000-000000000001
set ACCOUNT_ID=aaaa0000-0000-0000-0000-000000000001

echo.
echo === Page 0, Size 5 ===
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-page0" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"%ACCOUNT_ID%\"],\"pageNumber\":0,\"pageSize\":5}" | findstr /C:"pagination"

echo.
echo === Page 1, Size 5 ===
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-page1" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"%ACCOUNT_ID%\"],\"pageNumber\":1,\"pageSize\":5}" | findstr /C:"pagination"

echo.
echo === Page 0, Size 10 ===
curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-size10" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"%ACCOUNT_ID%\"],\"pageNumber\":0,\"pageSize\":10}" | findstr /C:"pagination"

echo.
echo ========================================
pause
