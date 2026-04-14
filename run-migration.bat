@echo off
echo ========================================
echo Running Data Migration
echo ========================================
echo.
echo This will migrate all legacy data from:
echo E:\Action Sheet System\data\
echo.
echo Files to migrate:
echo - employees.dat
echo - projects.dat
echo - actionSheets.dat
echo.
pause

call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=migrate

echo.
echo ========================================
echo Migration Complete
echo ========================================
pause
