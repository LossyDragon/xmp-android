@echo off

REM Its easier to add this batch file as a configuration.
REM 1. Edit Configurations
REM 2. Plus Icon -> Shell Script
REM 3. Script Path -> This batch file in \resources\

REM Desktop head unit gets buggy, forward a useless port
adb forward tcp:669 tcp:669
REM Wait
timeout 1
REM Forward the proper port for Desktop Head Unit
adb forward tcp:5277 tcp:5277
REM Wait
timeout 1
REM Launch the Head Unit
REM Make sure the Head Unit server is running on Android Auto
echo [ALLOW APP TO RUN AS ADMIN, IF PROMPTED]
cd %localappdata%\Android\Sdk\extras\google\auto
desktop-head-unit.exe
REM This EXE might need to be ran as Admin, again its buggy. 