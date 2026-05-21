@echo off
setlocal

:: Configuration
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
set DHU=%LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe

:: Check ADB
if not exist "%ADB%" (
    echo [ERROR] adb not found at: %ADB%
    echo         Update the ADB path in this script.
    pause
    exit /b 1
)

:: Check DHU
if not exist "%DHU%" (
    echo [ERROR] Desktop Head Unit not found at: %DHU%
    echo         Install it via SDK Manager: Extras ^> Google ^> Android Auto Desktop Head Unit emulator
    pause
    exit /b 1
)

:: Wait for device
echo [*] Waiting for ADB device...
"%ADB%" wait-for-device

:: Launch Android Auto on device (Google Pixel)
echo [*] Opening Android Auto...
"%ADB%" shell am start -n com.google.android.projection.gearhead/.companion.settings.DefaultSettingsActivity

echo.
echo [*] Start the Head Unit Server from the menu on your phone.
echo     Press any key when ready...
pause > nul

:: Forward the port
echo [*] Forwarding port 5277...
"%ADB%" forward tcp:5277 tcp:5277
if errorlevel 1 (
    echo [ERROR] Port forwarding failed. Is your emulator/device running?
    pause
    exit /b 1
)
echo [*] Port forwarded successfully.

:: Launch DHU
echo [*] Launching Desktop Head Unit...
start "" "%DHU%"

echo [*] Done! Android Auto should connect shortly.
exit /b 0
