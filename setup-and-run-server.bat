@echo off
setlocal
title Action Sheet System Backend
echo ====================================================
echo   Action Sheet Backend Server Auto-Setup
echo ====================================================

:: 1. Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Java is not installed. Downloading Java 21 (Eclipse Adoptium Temurin)...
    winget install -e --id EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
    
    echo.
    echo ====================================================
    echo SUCCESS! Java 21 has been installed.
    echo IMPORTANT: PLEASE CLOSE THIS WINDOW AND DOUBLE-CLICK 
    echo THIS SCRIPT AGAIN to refresh the system variables!
    echo ====================================================
    pause
    exit /b
)

:: 2. Check if ngrok is installed
ngrok --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] ngrok is not installed. Downloading ngrok...
    winget install -e --id ngrok.ngrok --accept-package-agreements --accept-source-agreements
    
    echo.
    echo ====================================================
    echo SUCCESS! ngrok has been installed.
    echo IMPORTANT: PLEASE CLOSE THIS WINDOW AND DOUBLE-CLICK 
    echo THIS SCRIPT AGAIN to refresh the system variables!
    echo ====================================================
    pause
    exit /b
)

:: 3. Configure Ngrok Automatically using provided Auth Token
echo [INFO] Configuring ngrok authentication...
ngrok config add-authtoken 3CCJPvpsQHqQOPRnPaJfS0dulfc_7EYwAEd2Au8BnS1hG1FWS >nul 2>&1

:: 4. Start ngrok in a separate window
echo [INFO] Starting ngrok static tunnel (childcare-scarce-cauterize)...
start cmd /k "title ngrok Tunnel && ngrok http --url=childcare-scarce-cauterize.ngrok-free.dev 8080"

:: 5. Start the backend
echo [INFO] Java is installed. Starting the Spring Boot backend...
echo [INFO] First run will automatically download Maven and dependencies. This may take a bit...
echo.

call mvnw.cmd spring-boot:run

echo.
echo Server stopped or an error occurred.
pause
