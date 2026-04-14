@echo off
setlocal enabledelayedexpansion
title Action Sheet Backend - Auto Setup
color 0A

:: Change to script directory
cd /d "%~dp0"

echo ========================================
echo  ACTION SHEET BACKEND - AUTO SETUP
echo ========================================
echo.

:: ============================================
:: STEP 1: JAVA INSTALLATION
:: ============================================
echo [1/5] Checking Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Java not found. Installing Java 21...
    
    :: Download Java
    powershell -NoProfile -ExecutionPolicy Bypass -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.msi' -OutFile 'jdk21.msi' -UseBasicParsing}"
    
    if not exist jdk21.msi (
        echo Failed to download Java. Trying alternative source...
        powershell -NoProfile -ExecutionPolicy Bypass -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk' -OutFile 'jdk21.msi' -UseBasicParsing}"
    )
    
    if exist jdk21.msi (
        echo Installing Java 21 silently...
        start /wait msiexec /i jdk21.msi /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome INSTALLDIR="C:\Program Files\Java\jdk-21"
        del jdk21.msi
        
        :: Add Java to PATH for this session
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        
        echo Java installed successfully!
    ) else (
        echo ERROR: Could not download Java installer.
        echo Please install Java 21 manually from: https://adoptium.net
        pause
        exit /b 1
    )
) else (
    echo Java found!
)
echo.

:: ============================================
:: STEP 2: VERIFY MAVEN WRAPPER
:: ============================================
echo [2/5] Checking Maven wrapper...
if not exist "mvnw.cmd" (
    echo ERROR: mvnw.cmd not found!
    echo Make sure you're running this from the backend folder.
    pause
    exit /b 1
)
echo Maven wrapper ready!
echo.

:: ============================================
:: STEP 3: NGROK INSTALLATION
:: ============================================
echo [3/5] Setting up ngrok...
set NGROK_FOUND=0

:: Check if ngrok is in PATH
ngrok version >nul 2>&1
if %errorlevel% equ 0 (
    set NGROK_FOUND=1
    set NGROK_CMD=ngrok
)

:: Check if ngrok.exe is in current directory
if exist "ngrok.exe" (
    set NGROK_FOUND=1
    set NGROK_CMD=ngrok.exe
)

:: Download ngrok if not found
if %NGROK_FOUND% equ 0 (
    echo Downloading ngrok...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-windows-amd64.zip' -OutFile 'ngrok.zip' -UseBasicParsing}"
    
    if exist ngrok.zip (
        echo Extracting ngrok...
        powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path ngrok.zip -DestinationPath . -Force"
        del ngrok.zip
        
        if exist "ngrok.exe" (
            set NGROK_FOUND=1
            set NGROK_CMD=ngrok.exe
            echo ngrok installed!
        )
    )
)

if %NGROK_FOUND% equ 0 (
    echo WARNING: Could not install ngrok. Server will run locally only.
    set NGROK_CMD=
) else (
    echo ngrok ready!
)
echo.

:: ============================================
:: STEP 4: START NGROK TUNNEL
:: ============================================
echo [4/5] Starting ngrok tunnel...
if defined NGROK_CMD (
    echo Configuring ngrok...
    %NGROK_CMD% config add-authtoken 3CCJPvpsQHqQOPRnPaJfS0dulfc_7EYwAEd2Au8BnS1hG1FWS >nul 2>&1
    
    echo Starting tunnel: childcare-scarce-cauterize.ngrok-free.dev
    start "ngrok Tunnel" cmd /k "%NGROK_CMD% http --url=childcare-scarce-cauterize.ngrok-free.dev 8080"
    
    :: Wait for ngrok to start
    timeout /t 5 /nobreak >nul
    echo Tunnel started!
) else (
    echo Skipping ngrok (not available)
)
echo.

:: ============================================
:: STEP 5: START BACKEND SERVER
:: ============================================
echo [5/5] Starting backend server...
echo ========================================
echo.
echo FIRST RUN: Maven will download dependencies
echo This takes 5-10 minutes. Please wait...
echo.
echo Server URL: http://localhost:8080
if defined NGROK_CMD (
    echo Public URL: https://childcare-scarce-cauterize.ngrok-free.dev
)
echo.
echo Press Ctrl+C to stop the server
echo ========================================
echo.

:: Start the server
call mvnw.cmd spring-boot:run

:: If server stops
echo.
echo ========================================
echo Server stopped
echo ========================================
pause
