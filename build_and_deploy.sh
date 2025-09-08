#!/bin/bash
# SimSpec Android Build and Deploy Script
# Builds the APK and installs it on connected Android device

set -e  # Exit on error

PROJECT_DIR="simspec-android"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "🏗️  SimSpec Android Build & Deploy"
echo "========================================="

# Check if project directory exists
if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ Error: Android project directory not found at $PROJECT_DIR"
    exit 1
fi

# Check if device is connected
echo "🔍 Checking device connection..."
if ! adb devices | grep -q device; then
    echo "❌ No Android device found!"
    echo "Please connect your Android device and enable USB debugging"
    exit 1
fi

DEVICE_INFO=$(adb devices | grep device | head -1)
echo "✅ Device connected: $DEVICE_INFO"

# Get device info
echo ""
echo "📱 Device Information:"
DEVICE_MODEL=$(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
ANDROID_VERSION=$(adb shell getprop ro.build.version.release 2>/dev/null || echo "Unknown")
SDK_VERSION=$(adb shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")
ARCH=$(adb shell getprop ro.product.cpu.abi 2>/dev/null || echo "Unknown")

echo "   Model: $DEVICE_MODEL"
echo "   Android: $ANDROID_VERSION (API $SDK_VERSION)"
echo "   Architecture: $ARCH"

# Verify minimum requirements
if [ "$SDK_VERSION" -lt 31 ]; then
    echo "⚠️  Warning: Device API level ($SDK_VERSION) is below minimum requirement (31)"
    echo "App may not run correctly"
fi

if [[ "$ARCH" != *"arm64"* ]] && [[ "$ARCH" != *"aarch64"* ]]; then
    echo "⚠️  Warning: Device architecture ($ARCH) may not support LEAP SDK"
    echo "LEAP SDK requires arm64-v8a ABI"
fi

# Build the project
echo ""
echo "🔨 Building Android project..."
cd "$PROJECT_DIR"

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "❌ Error: gradlew not found. Make sure you're in the correct directory"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean and build
echo "   Cleaning previous build..."
./gradlew clean

echo "   Building debug APK..."
./gradlew assembleDebug

# Check if APK was built
if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: APK not found at $APK_PATH"
    echo "Build may have failed"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "✅ APK built successfully ($APK_SIZE)"

# Install on device
echo ""
echo "📲 Installing on device..."
./gradlew installDebug

echo ""
echo "🎯 Deployment Complete!"
echo ""
echo "📋 Summary:"
echo "   ✅ APK built successfully"
echo "   ✅ Installed on device: $DEVICE_MODEL"
echo "   ✅ Ready for testing"
echo ""
echo "📱 Next Steps:"
echo "   1. Launch SimSpec app on device"
echo "   2. Grant camera permission when prompted"  
echo "   3. Point camera at engineering component"
echo "   4. Monitor LogCat: adb logcat | grep -E '(LeapService|VideoProcessor|SimSpec)'"
echo ""
echo "🔧 Troubleshooting:"
echo "   • If app crashes on launch: Check LogCat for LEAP SDK errors"
echo "   • If no analysis starts: Verify model file deployed correctly"
echo "   • For performance issues: Monitor inference times in LogCat"

cd ..  # Return to original directory