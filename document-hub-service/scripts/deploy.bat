@echo off
REM ============================================================================
REM Document Hub Service - Windows Deployment Script
REM This script sets up and runs the Document Hub Service with Docker
REM ============================================================================

echo ============================================
echo Document Hub Service - Deployment Script
echo ============================================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not installed or not in PATH
    echo Please install Docker Desktop from https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

echo [1/5] Stopping existing containers...
docker-compose down

echo.
echo [2/5] Starting PostgreSQL and Redis...
docker-compose up -d

echo.
echo [3/5] Waiting for database to be ready (30 seconds)...
timeout /t 30 /nobreak >nul

echo.
echo [4/5] Building the application...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)

echo.
echo [5/5] Application built successfully!
echo.
echo ============================================
echo Deployment Complete!
echo ============================================
echo.
echo Services running:
echo   - PostgreSQL: localhost:5433
echo   - Redis:      localhost:6379
echo.
echo To start the application, run:
echo   mvn spring-boot:run
echo.
echo Or run the JAR file:
echo   java -jar target\document-hub-service-1.0.0-SNAPSHOT.jar
echo.
echo Useful commands:
echo   - Check database: docker exec -it documenthub-postgres psql -U postgres -d documenthub
echo   - Check Redis:    docker exec -it documenthub-redis redis-cli
echo   - View logs:      docker-compose logs -f
echo   - Stop services:  docker-compose down
echo.
echo Sample API test:
echo   curl -X POST http://localhost:8080/documents-enquiry ^
echo     -H "Content-Type: application/json" ^
echo     -H "X-version: 1" ^
echo     -H "X-correlation-id: test-123" ^
echo     -H "X-requestor-id: 880e8400-e29b-41d4-a716-446655440001" ^
echo     -H "X-requestor-type: CUSTOMER" ^
echo     -d "{\"customerId\":\"880e8400-e29b-41d4-a716-446655440001\",\"accountId\":[\"770e8400-e29b-41d4-a716-446655440001\"],\"pageNumber\":1,\"pageSize\":20}"
echo.

pause
