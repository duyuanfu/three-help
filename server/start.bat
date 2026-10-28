@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ============================================
echo  bbb-server
echo    本机:   http://127.0.0.1:8090/api
echo    局域网: http://192.168.1.7:8090/api
echo  DB: root/root @ 127.0.0.1:3306/bbb
echo ============================================
echo.

where mvn >nul 2>nul
if %errorlevel%==0 (
    mvn -B spring-boot:run
) else (
    if exist "E:\Maven\bin\mvn.cmd" (
        "E:\Maven\bin\mvn.cmd" -B spring-boot:run
    ) else (
        echo [ERROR] mvn not found. Install Maven or edit this file with the full path.
        pause
    )
)
pause
