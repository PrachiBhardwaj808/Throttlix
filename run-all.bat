@echo off
title Throttlix Launcher
echo =====================================================================
echo           Launching Throttlix API Gateway and Client
echo =====================================================================
echo Starting Gateway Server (Terminal 1)...
start "Throttlix Gateway Server" cmd /k "cd /d "%~dp0backend" && mvn exec:java "-Dexec.args=server""

echo Waiting for Server initialization...
timeout /t 3 /nobreak >nul

echo Starting API Client Console (Terminal 2)...
start "Throttlix API Client" cmd /k "cd /d "%~dp0backend" && mvn exec:java "-Dexec.args=client""
