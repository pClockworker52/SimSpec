# SimSpec Model Deployment Script for Windows PowerShell
# Deploys the LFM2-VL model bundle to Android device

Write-Host "SimSpec Model Deployment" -ForegroundColor Green
Write-Host "========================================="

$ModelFile = "\\wsl.localhost\Ubuntu\home\peter\simspec_v1\models\LFM2-VL-450M_8da4w.bundle"
$DevicePath = "/data/local/tmp/leap/model.bundle"
$AdbPath = "C:\Users\peter\AppData\Local\Android\sdk\platform-tools\adb.exe"

# Check if model file exists
if (-not (Test-Path $ModelFile)) {
    Write-Host "ERROR: Model file not found at $ModelFile" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host "Model file: $ModelFile" -ForegroundColor Yellow
Write-Host "Target device path: $DevicePath" -ForegroundColor Yellow

# Get file size
$FileSize = (Get-Item $ModelFile).Length
$FileSizeMB = [math]::Round($FileSize / 1MB, 2)
Write-Host "Model size: $FileSizeMB MB" -ForegroundColor Yellow

# Check device connection
Write-Host ""
Write-Host "Checking device connection..." -ForegroundColor Cyan

$DevicesList = & "$AdbPath" devices
$ConnectedDevices = $DevicesList | Where-Object { $_ -match "\tdevice$" }

if (-not $ConnectedDevices) {
    Write-Host "ERROR: No Android device found!" -ForegroundColor Red
    Write-Host "Please ensure device is connected and USB debugging is enabled" -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
    exit 1
}

$DeviceId = ($ConnectedDevices[0] -split "\t")[0]
Write-Host "SUCCESS: Device connected: $DeviceId" -ForegroundColor Green

# Create directory on device
Write-Host ""
Write-Host "Creating directory on device..." -ForegroundColor Cyan
& "$AdbPath" shell mkdir -p /data/local/tmp/leap/

# Deploy model file
Write-Host ""
Write-Host "Deploying model bundle..." -ForegroundColor Cyan
Write-Host "This may take a few minutes ($FileSizeMB MB file)..." -ForegroundColor Yellow

$StartTime = Get-Date
& "$AdbPath" push "$ModelFile" "$DevicePath"
$EndTime = Get-Date
$Duration = ($EndTime - $StartTime).TotalSeconds

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Model deployment failed!" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

# Verify deployment
Write-Host ""
Write-Host "Verifying deployment..." -ForegroundColor Cyan
$DeviceFileInfo = & "$AdbPath" shell ls -la $DevicePath
Write-Host $DeviceFileInfo -ForegroundColor Gray

Write-Host ""
Write-Host "SUCCESS: Model deployed successfully!" -ForegroundColor Green
Write-Host "Transfer time: $([math]::Round($Duration, 1)) seconds" -ForegroundColor Green
Write-Host ""
Write-Host "Ready to run SimSpec on device!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "   1. Build and install SimSpec APK" -ForegroundColor White
Write-Host "   2. Launch app and test analysis" -ForegroundColor White
Write-Host "   3. Monitor LogCat for performance metrics" -ForegroundColor White

Read-Host "Press Enter to continue"