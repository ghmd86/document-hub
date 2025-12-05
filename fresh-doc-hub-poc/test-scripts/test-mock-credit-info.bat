@echo off
echo ========================================
echo TEST: Mock Credit Info API
echo ========================================
echo.

echo Account 1 (VIP - $75k):
curl -s "http://localhost:8080/mock-api/accounts/aaaa0000-0000-0000-0000-000000000001/credit-info"
echo.
echo.

echo Account 2 (Gold - $35k):
curl -s "http://localhost:8080/mock-api/accounts/aaaa0000-0000-0000-0000-000000000002/credit-info"
echo.
echo.

echo Account 3 (Standard - $15k):
curl -s "http://localhost:8080/mock-api/accounts/aaaa0000-0000-0000-0000-000000000003/credit-info"
echo.
echo.

echo ========================================
pause
