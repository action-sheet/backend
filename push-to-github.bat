@echo off
echo ========================================
echo Push to GitHub
echo ========================================
echo.

echo Step 1: Check if git is initialized...
if not exist .git (
    echo Initializing git repository...
    git init
    echo.
)

echo Step 2: Add all files...
git add .
echo.

echo Step 3: Commit changes...
set /p COMMIT_MSG="Enter commit message (or press Enter for default): "
if "%COMMIT_MSG%"=="" set COMMIT_MSG=Update Action Sheet Backend

git commit -m "%COMMIT_MSG%"
echo.

echo Step 4: Check remote...
git remote -v | findstr origin >nul
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo No remote repository configured!
    echo.
    echo Please enter your GitHub repository URL:
    echo Example: https://github.com/YOUR_USERNAME/actionsheet-backend.git
    echo.
    set /p REPO_URL="Repository URL: "
    
    git remote add origin !REPO_URL!
    echo Remote added!
    echo.
)

echo Step 5: Push to GitHub...
git branch -M main
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! Code pushed to GitHub
    echo ========================================
    echo.
    echo Next steps:
    echo 1. Go to https://render.com/
    echo 2. Create new Web Service
    echo 3. Connect your GitHub repository
    echo 4. Deploy!
    echo.
) else (
    echo.
    echo ========================================
    echo PUSH FAILED
    echo ========================================
    echo.
    echo Common issues:
    echo 1. Authentication failed - Use GitHub Desktop or create Personal Access Token
    echo 2. Repository doesn't exist - Create it on GitHub first
    echo 3. Branch protection - Check repository settings
    echo.
)

pause
