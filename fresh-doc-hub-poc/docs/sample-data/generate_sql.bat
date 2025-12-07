@echo off
echo ========================================
echo Generate SQL from CSV Onboarding Files
echo ========================================

cd /d "%~dp0"

echo.
echo Running Python converter...
python convert_to_sql.py generated_data.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SQL file generated successfully!
    echo Output: %~dp0generated_data.sql
    echo.
    echo To apply to database, run:
    echo   psql -U postgres -d document_hub -f generated_data.sql
) else (
    echo.
    echo ERROR: Failed to generate SQL
)

echo ========================================
pause
