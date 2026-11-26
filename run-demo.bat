@echo off
title Throttlix Algorithm Demonstration
cd /d "%~dp0backend"
mvn exec:java "-Dexec.args=demo"
pause
