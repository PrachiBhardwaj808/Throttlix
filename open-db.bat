@echo off
title Throttlix MySQL Shell
echo =====================================================================
echo              Connecting to MySQL 'throttlix' Database
echo =====================================================================

set MYSQL_CMD="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist %MYSQL_CMD% set MYSQL_CMD=mysql

%MYSQL_CMD% -u root -p throttlix
