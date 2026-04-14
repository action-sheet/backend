@echo off
echo ========================================
echo Ngrok Setup Script
echo ========================================
echo.

echo Step 1: Configuring ngrok auth token...
ngrok.exe config add-authtoken 2r_3ECzPwyqNrqWJPr9nXFP98bTLcsH

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Auth token configured!
) else (
    echo [ERROR] Failed to configure auth token
    pause
    exit /b 1
)

echo.
echo Step 2: Verifying configuration...
ngrok.exe config check

echo.
echo ========================================
echo Ngrok is ready!
echo ========================================
echo.
echo To start ngrok tunnel, run:
echo   ngrok.exe http 8080
echo.
echo Or use the start-ngrok.bat script
echo.
pause
