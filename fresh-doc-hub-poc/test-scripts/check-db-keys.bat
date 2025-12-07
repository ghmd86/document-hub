@echo off
echo ========================================
echo Check Database Keys (Customer/Account)
echo ========================================

set PGPASSWORD=1qaz#EDC
set PSQL="C:\Program Files\PostgreSQL\18\bin\psql.exe"

echo.
echo Customer keys:
%PSQL% -U postgres -d document_hub -c "SELECT DISTINCT customer_key FROM document_hub.storage_index WHERE customer_key IS NOT NULL;"

echo.
echo Account keys:
%PSQL% -U postgres -d document_hub -c "SELECT DISTINCT account_key FROM document_hub.storage_index WHERE account_key IS NOT NULL;"

echo.
echo Template types:
%PSQL% -U postgres -d document_hub -c "SELECT DISTINCT template_type, template_version FROM document_hub.storage_index ORDER BY template_type, template_version;"

echo ========================================
pause
