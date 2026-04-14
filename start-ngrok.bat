@echo off
echo ========================================
echo Starting Ngrok Tunnel
echo ========================================
echo.
echo Backend URL: http://localhost:8080
echo.
echo Starting ngrok tunnel...
echo Press Ctrl+C to stop
echo.

ngrok.exe http 8080

pause
