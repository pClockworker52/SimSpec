# SimSpec APK Build Script for Windows
# Builds the APK and installs it on connected Android device

Write-Host "SimSpec Android Build & Deploy" -ForegroundColor Green
Write-Host "========================================="

$ProjectPath = "\\wsl.localhost\Ubuntu\home\peter\simspec_v1\simspec-android\simspec-android"
$AdbPath = "C:\Users\peter\AppData\Local\Android\sdk\platform-tools\adb.exe"
$AndroidHome = "C:\Users\peter\AppData\Local\Android\Sdk"

# Set Android SDK environment variables
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome

# Check if project exists
if (-not (Test-Path $ProjectPath)) {
    Write-Host "ERROR: Android project not found at $ProjectPath" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

# Check device connection
Write-Host "Checking device connection..." -ForegroundColor Cyan
$DevicesList = & "$AdbPath" devices
$ConnectedDevices = $DevicesList | Where-Object { $_ -match "\tdevice$" }

if (-not $ConnectedDevices) {
    Write-Host "ERROR: No Android device found!" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

$DeviceId = ($ConnectedDevices[0] -split "\t")[0]
Write-Host "SUCCESS: Device connected: $DeviceId" -ForegroundColor Green

# Get device info
$DeviceModel = & "$AdbPath" shell getprop ro.product.model
$AndroidVersion = & "$AdbPath" shell getprop ro.build.version.release
$SdkVersion = & "$AdbPath" shell getprop ro.build.version.sdk
$Arch = & "$AdbPath" shell getprop ro.product.cpu.abi

Write-Host ""
Write-Host "Device Information:" -ForegroundColor Yellow
Write-Host "   Model: $DeviceModel" -ForegroundColor White
Write-Host "   Android: $AndroidVersion (API $SdkVersion)" -ForegroundColor White  
Write-Host "   Architecture: $Arch" -ForegroundColor White

# Check minimum requirements
if ([int]$SdkVersion -lt 31) {
    Write-Host "WARNING: Device API level ($SdkVersion) is below minimum requirement (31)" -ForegroundColor Yellow
}

if ($Arch -notmatch "arm64|aarch64") {
    Write-Host "WARNING: Device architecture ($Arch) may not support LEAP SDK" -ForegroundColor Yellow
    Write-Host "LEAP SDK requires arm64-v8a ABI" -ForegroundColor Yellow
}

# Navigate to project directory
Set-Location $ProjectPath

# Check if gradlew exists
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "ERROR: gradlew.bat not found. Creating Windows wrapper..." -ForegroundColor Yellow
    
    # Create gradlew.bat for Windows
    $gradlewBat = @'
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd_ return code.
if not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
'@

    $gradlewBat | Out-File -FilePath ".\gradlew.bat" -Encoding ascii
}

# Build the project
Write-Host ""
Write-Host "Building Android project..." -ForegroundColor Cyan

Write-Host "   Cleaning previous build..." -ForegroundColor Gray
& ".\gradlew.bat" clean

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Clean failed" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host "   Building debug APK..." -ForegroundColor Gray
& ".\gradlew.bat" assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

# Check if APK was built
$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $ApkPath)) {
    Write-Host "ERROR: APK not found at $ApkPath" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

$ApkSize = [math]::Round((Get-Item $ApkPath).Length / 1MB, 2)
Write-Host "SUCCESS: APK built successfully ($ApkSize MB)" -ForegroundColor Green

# Install on device
Write-Host ""
Write-Host "Installing on device..." -ForegroundColor Cyan
& "$AdbPath" install -r "$ApkPath"

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Installation failed" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "SUCCESS: Deployment Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "   APK built successfully: $ApkSize MB" -ForegroundColor White
Write-Host "   Installed on device: $DeviceModel" -ForegroundColor White
Write-Host "   Ready for testing" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "   1. Launch SimSpec app on device" -ForegroundColor White
Write-Host "   2. Grant camera permission when prompted" -ForegroundColor White
Write-Host "   3. Point camera at engineering component" -ForegroundColor White
Write-Host "   4. Watch real-time analysis with LFM2-VL model!" -ForegroundColor White
Write-Host ""
Write-Host "Monitor performance:" -ForegroundColor Yellow
Write-Host "   adb logcat | findstr SimSpec" -ForegroundColor White

Read-Host "Press Enter to continue"