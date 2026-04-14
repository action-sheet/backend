@echo off
echo ========================================
echo PDF Compression and Data Migration
echo ========================================
echo.
echo This script will:
echo 1. Compress all large PDFs in E:\Action Sheet System\data\
echo 2. Create backups before compression
echo 3. Run the data migration to import everything
echo.
echo ========================================
pause

echo.
echo Step 1: Installing Python dependencies...
echo ========================================
pip install PyPDF2
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to install PyPDF2
    echo Please install Python first: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo.
echo Step 2: Compressing PDFs...
echo ========================================
python compress-pdfs.py
if %errorlevel% neq 0 (
    echo.
    echo ERROR: PDF compression failed
    pause
    exit /b 1
)

echo.
echo Step 3: Running data migration...
echo ========================================
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=migrate

echo.
echo ========================================
echo ALL DONE!
echo ========================================
echo - PDFs have been compressed
echo - Backups are in E:\Action Sheet System\data_backup_*
echo - Data has been migrated to the database
echo.
pause
