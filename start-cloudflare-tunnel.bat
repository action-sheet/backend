@echo off
echo ========================================
echo Starting Cloudflare Tunnel (FREE)
echo ========================================
echo.
echo Backend: http://localhost:8080
echo.
echo Starting tunnel...
echo Your public URL will appear below:
echo.

cloudflared.exe tunnel --url http://localhost:8080

pause
