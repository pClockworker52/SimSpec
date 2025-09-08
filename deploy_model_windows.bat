@echo off
REM SimSpec Model Deployment Script for Windows
REM Deploys the LFM2-VL model bundle to Android device

echo 🚀 SimSpec Model Deployment
echo =========================================

set MODEL_FILE=models\LFM2-VL-450M_8da4w.bundle
set DEVICE_PATH=/data/local/tmp/leap/model.bundle
set ADB_PATH=C:\Users\peter\AppData\Local\Android\sdk\platform-tools\adb.exe

REM Check if model file exists
if not exist "%MODEL_FILE%" (
    echo ❌ Error: Model file not found at %MODEL_FILE%
    pause
    exit /b 1
)

echo 📁 Model file: %MODEL_FILE%
echo 📱 Target device path: %DEVICE_PATH%

REM Check device connection
echo.
echo 🔍 Checking device connection...
"%ADB_PATH%" devices | findstr "device" >nul
if errorlevel 1 (
    echo ❌ No Android device found!
    echo Please ensure device is connected and USB debugging is enabled
    pause
    exit /b 1
)

for /f "tokens=1" %%i in ('"%ADB_PATH%" devices ^| findstr "device"') do set DEVICE_ID=%%i
echo ✅ Device connected: %DEVICE_ID%

REM Create directory on device
echo.
echo 📂 Creating directory on device...
"%ADB_PATH%" shell mkdir -p /data/local/tmp/leap/

REM Deploy model file
echo.
echo 📤 Deploying model bundle...
echo This may take a few minutes (367 MB file)...

"%ADB_PATH%" push "%MODEL_FILE%" "%DEVICE_PATH%"

if errorlevel 1 (
    echo ❌ Model deployment failed!
    pause
    exit /b 1
)

REM Verify deployment
echo.
echo 🔍 Verifying deployment...
"%ADB_PATH%" shell ls -la "%DEVICE_PATH%"

echo.
echo ✅ Model deployed successfully!
echo.
echo 🎯 Ready to run SimSpec on device!
echo.
echo Next steps:
echo   1. Build and install SimSpec APK
echo   2. Launch app and test analysis
echo   3. Monitor LogCat for performance metrics

pause