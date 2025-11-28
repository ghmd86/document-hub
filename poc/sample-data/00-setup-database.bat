@echo off
REM ##############################################################################
REM Document Hub POC - Database Setup Script (Windows)
REM ##############################################################################
REM Purpose: Automated database setup and sample data loading for Windows
REM Version: 1.0
REM Date: 2025-11-27
REM ##############################################################################

setlocal enabledelayedexpansion

REM Database configuration (can be overridden by environment variables)
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=5432
if not defined DB_NAME set DB_NAME=dochub_poc
if not defined DB_USER set DB_USER=postgres

REM Script directory
set SCRIPT_DIR=%~dp0
set SQL_DIR=%SCRIPT_DIR%

echo.
echo ========================================
echo Document Hub POC - Database Setup
echo ========================================
echo.
echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Database: %DB_NAME%
echo   User: %DB_USER%
echo.

REM Check if psql is available
where psql >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: PostgreSQL client ^(psql^) not found
    echo.
    echo Please install PostgreSQL client:
    echo   Download from: https://www.postgresql.org/download/windows/
    echo   Or install via chocolatey: choco install postgresql
    echo.
    pause
    exit /b 1
)

echo [OK] PostgreSQL client found
echo.

REM Prompt for password if not set
if not defined PGPASSWORD (
    set /p PGPASSWORD=Enter PostgreSQL password for user %DB_USER%:
)

echo.
echo ========================================
echo Step 1: Testing Database Connection
echo ========================================
echo.

REM Test connection
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "SELECT 1" >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Cannot connect to database
    echo.
    echo Connection details:
    echo   Host: %DB_HOST%
    echo   Port: %DB_PORT%
    echo   User: %DB_USER%
    echo.
    echo Please check your connection settings and password.
    pause
    exit /b 1
)

echo [OK] Database connection successful
echo.

REM Check if database exists
echo ========================================
echo Step 2: Creating Database
echo ========================================
echo.

for /f "delims=" %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='%DB_NAME%'"') do set DB_EXISTS=%%i

if "%DB_EXISTS%"=="1" (
    echo WARNING: Database '%DB_NAME%' already exists
    set /p RECREATE=Do you want to drop and recreate it? ^(yes/no^):
    if /i "!RECREATE!"=="yes" (
        echo Dropping existing database...
        psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "DROP DATABASE IF EXISTS %DB_NAME%;"
        psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE %DB_NAME%;"
        echo [OK] Database recreated
    ) else (
        echo Using existing database
    )
) else (
    echo Creating database '%DB_NAME%'...
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE %DB_NAME%;"
    echo [OK] Database created
)
echo.

REM Create schema
echo ========================================
echo Step 3: Creating Database Schema
echo ========================================
echo.

set SCHEMA_FILE=%SCRIPT_DIR%..\..\database\schemas\document_hub_schema.sql

if not exist "%SCHEMA_FILE%" (
    echo ERROR: Schema file not found: %SCHEMA_FILE%
    echo.
    echo Please ensure document_hub_schema.sql exists in:
    echo   database\schemas\document_hub_schema.sql
    pause
    exit /b 1
)

echo Loading schema from: %SCHEMA_FILE%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%SCHEMA_FILE%"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to create schema
    pause
    exit /b 1
)
echo [OK] Schema created successfully
echo.

REM Load templates
echo ========================================
echo Step 4: Loading Template Definitions
echo ========================================
echo.

set TEMPLATE_FILE=%SQL_DIR%01-templates.sql

if not exist "%TEMPLATE_FILE%" (
    echo ERROR: Template file not found: %TEMPLATE_FILE%
    pause
    exit /b 1
)

echo Loading templates from: %TEMPLATE_FILE%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%TEMPLATE_FILE%"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to load templates
    pause
    exit /b 1
)

for /f "delims=" %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -tAc "SELECT COUNT(*) FROM master_template_definition;"') do set TEMPLATE_COUNT=%%i
echo [OK] Loaded %TEMPLATE_COUNT% template^(s^)
echo.

REM Load documents
echo ========================================
echo Step 5: Loading Sample Documents
echo ========================================
echo.

set DOCUMENT_FILE=%SQL_DIR%02-documents.sql

if not exist "%DOCUMENT_FILE%" (
    echo ERROR: Document file not found: %DOCUMENT_FILE%
    pause
    exit /b 1
)

echo Loading documents from: %DOCUMENT_FILE%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%DOCUMENT_FILE%"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to load documents
    pause
    exit /b 1
)

for /f "delims=" %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -tAc "SELECT COUNT(*) FROM storage_index;"') do set DOCUMENT_COUNT=%%i
echo [OK] Loaded %DOCUMENT_COUNT% document^(s^)
echo.

REM Create indices
echo ========================================
echo Step 6: Creating Performance Indices
echo ========================================
echo.

echo Creating indices on storage_index table...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_storage_index_account_key ON storage_index(account_key) WHERE account_key IS NOT NULL;"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_storage_index_customer_key ON storage_index(customer_key) WHERE customer_key IS NOT NULL;"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_storage_index_reference_key ON storage_index(reference_key, reference_key_type) WHERE reference_key IS NOT NULL;"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_storage_index_template_type ON storage_index(template_type);"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_storage_index_shared ON storage_index(is_shared) WHERE is_shared = true;"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_storage_index_metadata ON storage_index USING GIN(doc_metadata);"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE INDEX IF NOT EXISTS idx_master_template_active ON master_template_definition(template_type, is_active) WHERE is_active = true;"

echo [OK] Indices created successfully
echo.

REM Verify data
echo ========================================
echo Step 7: Verifying Data
echo ========================================
echo.

echo Template Summary:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT template_type, template_version, template_name, is_active, is_shared_document FROM master_template_definition ORDER BY template_type;"
echo.

echo Document Summary:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT template_type, COUNT(*) as document_count, SUM(CASE WHEN is_shared THEN 1 ELSE 0 END) as shared_count, SUM(CASE WHEN is_shared THEN 0 ELSE 1 END) as customer_specific_count FROM storage_index GROUP BY template_type ORDER BY template_type;"
echo.

REM Display summary
echo ========================================
echo Setup Complete
echo ========================================
echo.
echo [OK] Database setup successful!
echo.
echo Database Details:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Database: %DB_NAME%
echo   User: %DB_USER%
echo.
echo Next Steps:
echo   1. Review test scenarios:
echo      type %SQL_DIR%03-test-scenarios.md
echo.
echo   2. Start the POC application:
echo      cd ..\doc-hub-poc
echo      mvn spring-boot:run
echo.
echo   3. Test the API:
echo      curl -X POST http://localhost:8080/api/documents/enquiry ^
echo        -H "Content-Type: application/json" ^
echo        -d "{\"customerId\":\"C001\",\"accountId\":\"A001\",\"templateType\":\"MONTHLY_STATEMENT\"}"
echo.
echo For detailed test scenarios, see: 03-test-scenarios.md
echo.

pause
