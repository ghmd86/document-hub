@echo off
echo ========================================
echo Restart Document Hub Application
echo ========================================

echo Finding process on port 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Killing process %%a...
    taskkill /F /PID %%a 2>nul
)

echo.
echo Starting application...
cd /d "C:\Users\ghmd8\Documents\AI\fresh-doc-hub-poc"
start "Document Hub" cmd /c "mvn spring-boot:run"

echo.
echo Application starting in new window...
echo Wait about 10 seconds for it to be ready.
echo ========================================
pause
