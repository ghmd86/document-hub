@echo off
echo ========================================
echo Reload Database Data from data.sql
echo ========================================

set PGPASSWORD=1qaz#EDC
set PSQL="C:\Program Files\PostgreSQL\18\bin\psql.exe"
set DATA_FILE=C:\Users\ghmd8\Documents\AI\fresh-doc-hub-poc\src\main\resources\data.sql

echo.
echo WARNING: This will delete and reload all data!
echo.
set /p CONFIRM=Type YES to continue:
if /I not "%CONFIRM%"=="YES" (
    echo Cancelled.
    pause
    exit /b 1
)

echo.
echo Dropping existing data...
%PSQL% -U postgres -d document_hub -c "DELETE FROM document_hub.storage_index;"
%PSQL% -U postgres -d document_hub -c "DELETE FROM document_hub.master_template_definition;"

echo.
echo Loading data from data.sql...
%PSQL% -U postgres -d document_hub -f "%DATA_FILE%"

echo.
echo Verifying counts:
%PSQL% -U postgres -d document_hub -c "SELECT 'templates' as table_name, COUNT(*) as count FROM document_hub.master_template_definition UNION ALL SELECT 'documents', COUNT(*) FROM document_hub.storage_index;"

echo.
echo ========================================
echo Data reload complete!
pause
