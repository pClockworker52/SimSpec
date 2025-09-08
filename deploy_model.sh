#!/bin/bash
# SimSpec Model Deployment Script
# Deploys the LFM2-VL model bundle to Android device

set -e  # Exit on error

MODEL_FILE="models/LFM2-VL-450M_8da4w.bundle"
DEVICE_PATH="/data/local/tmp/leap/model.bundle"

echo "🚀 SimSpec Model Deployment"
echo "========================================="

# Check if model file exists
if [ ! -f "$MODEL_FILE" ]; then
    echo "❌ Error: Model file not found at $MODEL_FILE"
    exit 1
fi

echo "📁 Model file: $MODEL_FILE"
echo "📱 Target device path: $DEVICE_PATH"
echo "📏 Model size: $(du -h $MODEL_FILE | cut -f1)"

# Check if device is connected
echo ""
echo "🔍 Checking device connection..."
if ! adb devices | grep -q device; then
    echo "❌ No Android device found!"
    echo ""
    echo "Please ensure:"
    echo "  1. Android device is connected via USB"
    echo "  2. Developer options are enabled"
    echo "  3. USB debugging is enabled"
    echo "  4. Computer is trusted on the device"
    echo ""
    echo "Run 'adb devices' to verify connection"
    exit 1
fi

DEVICE_INFO=$(adb devices | grep device | head -1)
echo "✅ Device connected: $DEVICE_INFO"

# Create directory on device
echo ""
echo "📂 Creating directory on device..."
adb shell mkdir -p /data/local/tmp/leap/

# Deploy model file
echo ""
echo "📤 Deploying model bundle..."
echo "This may take a few minutes (367 MB file)..."

# Show progress and deploy
adb push "$MODEL_FILE" "$DEVICE_PATH"

# Verify deployment
echo ""
echo "🔍 Verifying deployment..."
DEVICE_SIZE=$(adb shell stat -c%s "$DEVICE_PATH" 2>/dev/null || echo "0")
LOCAL_SIZE=$(stat -c%s "$MODEL_FILE")

if [ "$DEVICE_SIZE" -eq "$LOCAL_SIZE" ]; then
    echo "✅ Model deployed successfully!"
    echo "   Local size:  $(numfmt --to=iec $LOCAL_SIZE)"
    echo "   Device size: $(numfmt --to=iec $DEVICE_SIZE)"
    echo ""
    echo "🎯 Ready to run SimSpec on device!"
else
    echo "❌ Deployment verification failed"
    echo "   Local size:  $(numfmt --to=iec $LOCAL_SIZE)"
    echo "   Device size: $(numfmt --to=iec $DEVICE_SIZE)"
    exit 1
fi

echo ""
echo "📋 Deployment Summary:"
echo "   ✅ Model file deployed"
echo "   ✅ File integrity verified" 
echo "   ✅ Ready for SimSpec app testing"
echo ""
echo "Next steps:"
echo "   1. Build and install SimSpec APK"
echo "   2. Launch app and test analysis"
echo "   3. Monitor LogCat for performance metrics"