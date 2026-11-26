@echo off
title Throttlix API Client
cd /d "%~dp0backend"
mvn exec:java "-Dexec.args=client"
pause
