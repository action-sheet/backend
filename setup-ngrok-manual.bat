@echo off
echo ========================================
echo Ngrok Auth Token Setup
echo ========================================
echo.
echo Please paste your FULL ngrok authtoken below.
echo Get it from: https://dashboard.ngrok.com/get-started/your-authtoken
echo.
echo IMPORTANT: Copy the ENTIRE token (should be 50+ characters)
echo.

set /p TOKEN="Enter your authtoken: "

echo.
echo Configuring ngrok with token: %TOKEN%
echo.

ngrok.exe config add-authtoken %TOKEN%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Auth token configured!
    echo.
    echo You can now start ngrok with:
    echo   ngrok.exe http 8080
    echo.
) else (
    echo.
    echo [ERROR] Failed to configure auth token
    echo Please check that you copied the complete token
    echo.
)

pause
