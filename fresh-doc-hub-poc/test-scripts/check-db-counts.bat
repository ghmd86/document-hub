@echo off
echo ========================================
echo Check Database Document Counts
echo ========================================

set PGPASSWORD=1qaz#EDC
set PSQL="C:\Program Files\PostgreSQL\18\bin\psql.exe"

echo.
echo Document counts (total vs accessible):
%PSQL% -U postgres -d document_hub -c "SELECT COUNT(*) as total, COUNT(CASE WHEN accessible_flag THEN 1 END) as accessible FROM document_hub.storage_index;"

echo.
echo Template counts:
%PSQL% -U postgres -d document_hub -c "SELECT COUNT(*) as templates FROM document_hub.master_template_definition;"

echo.
echo Documents by account:
%PSQL% -U postgres -d document_hub -c "SELECT account_key, COUNT(*) as doc_count FROM document_hub.storage_index WHERE accessible_flag = true GROUP BY account_key ORDER BY doc_count DESC;"

echo ========================================
pause
