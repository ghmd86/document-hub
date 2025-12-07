@echo off
echo ========================================
echo Test Document Enquiry API
echo ========================================

set CUSTOMER_ID=cccc0000-0000-0000-0000-000000000001
set ACCOUNT_ID=aaaa0000-0000-0000-0000-000000000001
set PAGE_SIZE=5
set PAGE_NUM=0

echo.
echo Customer ID: %CUSTOMER_ID%
echo Account ID: %ACCOUNT_ID%
echo Page: %PAGE_NUM%, Size: %PAGE_SIZE%
echo.

curl -s -X POST "http://localhost:8080/api/v1/documents-enquiry" ^
  -H "Content-Type: application/json" ^
  -H "X-version: 1" ^
  -H "X-correlation-id: test-%RANDOM%" ^
  -H "X-requestor-id: %CUSTOMER_ID%" ^
  -H "X-requestor-type: CUSTOMER" ^
  -d "{\"customerId\":\"%CUSTOMER_ID%\",\"accountId\":[\"%ACCOUNT_ID%\"],\"pageNumber\":%PAGE_NUM%,\"pageSize\":%PAGE_SIZE%}"

echo.
echo ========================================
pause
