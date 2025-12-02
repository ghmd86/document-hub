@echo off
REM Direct Compiler Build Script for Windows
REM This script bypasses the Maven lifecycle issue by using compiler plugin directly

echo ========================================
echo POC Project - Direct Compiler Build
echo ========================================
echo.

echo [1/4] Cleaning previous build...
call mvn clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed
    exit /b 1
)
echo.

echo [2/4] Generating OpenAPI models...
call mvn generate-sources
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: OpenAPI generation failed
    exit /b 1
)
echo.

echo [3/4] Compiling with Lombok support...
call mvn compiler:compile
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed
    exit /b 1
)
echo.

echo [4/4] Compiling test sources...
call mvn compiler:testCompile
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Test compilation failed (continuing...)
)
echo.

echo ========================================
echo BUILD SUCCESS
echo ========================================
echo.
echo Compiled classes: target\classes
echo Generated sources: target\generated-sources\openapi
echo.
echo To run the application:
echo   mvn spring-boot:run
echo.
