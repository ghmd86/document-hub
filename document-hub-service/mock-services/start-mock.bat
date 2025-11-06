@echo off
REM ============================================================================
REM Start Mock Services
REM ============================================================================

echo ================================================
echo Starting Mock Services on port 8090
echo ================================================
echo.

echo Building mock services...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo Starting mock services...
echo Mock APIs will be available at: http://localhost:8090
echo.
echo Press Ctrl+C to stop
echo.

java -jar target\mock-services-1.0.0-SNAPSHOT.jar

pause
