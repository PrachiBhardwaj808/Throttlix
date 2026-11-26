@echo off
title Throttlix Gateway Server
cd /d "%~dp0backend"
mvn exec:java "-Dexec.args=server"
pause
