# SimSpec Android Deployment Guide

## Overview
This guide covers deploying the SimSpec Android application with the LEAP SDK for AI-powered engineering component analysis.

## Prerequisites

### Required Files
1. **LEAP SDK**: `leap-sdk-0.5.0.aar` file (place in `simspec-android/app/libs/`)
2. **LFM2-VL Model**: `model.bundle` file (deploy to device)
3. **Android Device**: Physical device with API 31+ and camera

### Development Environment
- Android Studio latest stable version
- Minimum SDK: API 31 (Android 12)
- Target SDK: API 35
- Kotlin 1.9.22+
- Gradle 8.5.1+

## Setup Steps

### 1. Model Deployment
Deploy the LFM2-VL model to your Android device:

```bash
# Create directory on device
adb shell mkdir -p /data/local/tmp/leap

# Push model file (replace path with your actual model location)
adb push path/to/your/model.bundle /data/local/tmp/leap/model.bundle

# Verify deployment
adb shell ls -la /data/local/tmp/leap/
```

### 2. SDK Integration
1. Place the `leap-sdk-0.5.0.aar` file in `simspec-android/app/libs/`
2. Update the filename in `app/build.gradle.kts` if different:
   ```kotlin
   implementation(files("libs/leap-sdk-0.5.0.aar"))
   ```

### 3. Build and Deploy
```bash
cd simspec-android

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install in one step
./gradlew installDebug
```

## Performance Optimizations

### Based on Testing Results
- **Throttle Interval**: Set to 5 seconds (optimized from 3 seconds)
- **Image Resolution**: 512x512 (may reduce to 384x384 if performance issues)
- **Expected Load Time**: ~2.5 seconds for model initialization
- **Expected Inference Time**: 1-4 seconds per analysis (mobile)

### Configuration Updates
The `VideoProcessor` is configured with:
```kotlin
private const val ANALYSIS_INTERVAL_MS = 5000L // 5 seconds
```

Frame resizing in `VideoProcessor`:
```kotlin
val resizedBitmap = FrameExtractor.resizeBitmap(bitmap, 512, 512)
```

## Testing and Validation

### Performance Testing
1. **Model Load Time**: Check LogCat for initialization timing
2. **Inference Performance**: Monitor analysis times in LogCat
3. **Progressive Analysis**: Verify all 3 stages execute correctly
4. **Question Generation**: Confirm engineering questions appear

### LogCat Monitoring
```bash
# Filter for SimSpec logs
adb logcat | grep -E "(LeapService|VideoProcessor|SimSpec)"

# Monitor performance metrics
adb logcat | grep -E "(Analysis complete|Initialized)"
```

### Expected Log Output
```
LeapService: ✅ LEAP SDK Initialized Successfully in 2500ms
VideoProcessor: 🎯 Starting analysis step 1/3
LeapService: 🧠 Analysis complete in 1500ms
VideoProcessor: ✅ Step 1 complete in 1500ms
```

## Troubleshooting

### Common Issues

#### 1. Model Loading Fails
- **Check**: Model file exists at `/data/local/tmp/leap/model.bundle`
- **Fix**: Re-deploy model using adb push command
- **Log**: Look for "LEAP SDK Initialization Failed" in LogCat

#### 2. Camera Permission Denied
- **Check**: Camera permission granted in system settings
- **Fix**: Manually enable camera permission or reinstall app
- **Log**: Look for "Camera permission is required" error

#### 3. Slow Performance
- **Check**: LogCat for inference times > 4000ms
- **Fix**: Reduce image resolution to 384x384 or 256x256
- **Location**: Update `resizeBitmap` call in `VideoProcessor.kt`

#### 4. Analysis Not Starting
- **Check**: LEAP SDK initialization status
- **Fix**: Verify model deployment and SDK integration
- **Log**: Look for initialization success/failure messages

### Performance Optimization Steps
If performance is poor (inference > 4s):

1. **Reduce Image Resolution**:
   ```kotlin
   // In VideoProcessor.kt, change from:
   val resizedBitmap = FrameExtractor.resizeBitmap(bitmap, 512, 512)
   // To:
   val resizedBitmap = FrameExtractor.resizeBitmap(bitmap, 384, 384)
   ```

2. **Increase Throttle Interval**:
   ```kotlin
   // In VideoProcessor.kt, change from:
   private const val ANALYSIS_INTERVAL_MS = 5000L
   // To:
   private const val ANALYSIS_INTERVAL_MS = 7000L
   ```

## Demo Recording

### Screen Recording
```bash
# Start recording
adb shell screenrecord /sdcard/simspec_demo.mp4

# Use the app, then stop recording (Ctrl+C)

# Pull recording to computer
adb pull /sdcard/simspec_demo.mp4 .
```

### Demo Checklist
- [ ] App launches successfully
- [ ] Camera preview appears
- [ ] Progressive analysis begins (3 steps)
- [ ] Analysis results appear in real-time
- [ ] Engineering questions are generated
- [ ] Performance is acceptable (<5s per inference)

## File Structure
```
simspec-android/
├── app/
│   ├── libs/
│   │   └── leap-sdk-0.5.0.aar
│   ├── src/main/
│   │   ├── java/com/simspec/
│   │   │   ├── services/
│   │   │   │   ├── LeapService.kt
│   │   │   │   └── VideoProcessor.kt
│   │   │   ├── utils/
│   │   │   │   ├── FrameExtractor.kt
│   │   │   │   ├── ContextInterpreter.kt
│   │   │   │   └── QuestionGenerator.kt
│   │   │   ├── ui/
│   │   │   │   └── [UI components]
│   │   │   ├── MainActivity.kt
│   │   │   └── MainViewModel.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Next Steps for Production

1. **Real LEAP SDK Integration**: Replace mock implementations with actual SDK calls
2. **Error Handling**: Enhance error recovery and user feedback
3. **Data Persistence**: Save analysis results and export capabilities
4. **Testing**: Add unit tests and instrumentation tests
5. **Performance**: Profile and optimize for various device types
6. **UI Polish**: Enhance visual design and user experience