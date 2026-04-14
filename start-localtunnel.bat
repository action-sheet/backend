@echo off
echo ========================================
echo LocalTunnel Setup (FREE - Better CORS)
echo ========================================
echo.
echo Checking if localtunnel is installed...
echo.

where lt >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo LocalTunnel already installed!
    goto START
)

echo Installing localtunnel via npm...
echo.

npm install -g localtunnel

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] LocalTunnel installed!
) else (
    echo [ERROR] Failed to install. Make sure Node.js is installed.
    echo Download from: https://nodejs.org/
    pause
    exit /b 1
)

:START
echo.
echo Starting tunnel on port 8080...
echo Your URL will appear below:
echo.
echo NOTE: First visit will show a warning page.
echo Click "Continue" to access the backend.
echo.

lt --port 8080

pause
