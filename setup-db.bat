@echo off
title Throttlix Database Setup
echo =====================================================================
echo                Throttlix MySQL Database Setup
echo =====================================================================
echo Importing database/mysql-schema.sql into MySQL...

set MYSQL_CMD="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist %MYSQL_CMD% set MYSQL_CMD=mysql

%MYSQL_CMD% -u root -p -e "source %~dp0database/mysql-schema.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================================
    echo [SUCCESS] Database 'throttlix' and all tables initialized!
    echo =====================================================================
) else (
    echo.
    echo [ERROR] Setup encountered an issue. Check your MySQL password.
)
pause
