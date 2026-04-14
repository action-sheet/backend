@echo off
echo ========================================
echo Serveo Tunnel (FREE - No Installation)
echo ========================================
echo.
echo This uses SSH to create a tunnel (no software needed!)
echo.
echo Starting tunnel...
echo Your public URL will appear below:
echo.

ssh -R 80:localhost:8080 serveo.net

pause
