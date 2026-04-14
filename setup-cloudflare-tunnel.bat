@echo off
echo ========================================
echo Cloudflare Tunnel Setup (FREE)
echo ========================================
echo.
echo This is a FREE alternative to ngrok with NO data limits!
echo.

echo Step 1: Downloading Cloudflare Tunnel (cloudflared)...
echo.

curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe -o cloudflared.exe

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Downloaded cloudflared.exe
) else (
    echo [ERROR] Failed to download. Please download manually from:
    echo https://github.com/cloudflare/cloudflared/releases/latest
    pause
    exit /b 1
)

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo To start the tunnel, run:
echo   .\start-cloudflare-tunnel.bat
echo.
echo Or manually:
echo   .\cloudflared.exe tunnel --url http://localhost:8080
echo.
pause
