# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimSpec is an Android application that uses the LEAP SDK and LFM2-VL model for AI-powered engineering component analysis. The app performs progressive visual analysis of mechanical components through camera input, identifying parts, assessing condition, and generating engineering-specific questions.

## Architecture

This project follows a clean Android architecture with these core components:

### Services Layer
- **LeapService**: Singleton service managing LEAP SDK initialization and AI inference operations
- **VideoProcessor**: CameraX ImageAnalysis.Analyzer implementation for real-time frame processing with throttling (5-second intervals, optimized based on testing)

### Utils Layer  
- **FrameExtractor**: Converts CameraX ImageProxy to Bitmap with proper rotation handling
- **ContextInterpreter**: Maps AI responses to engineering analysis types (fatigue, corrosion, welds, etc.)
- **QuestionGenerator**: Generates contextual engineering questions based on detected components

### UI Layer
- **MainActivity**: Main entry point with LEAP initialization and camera permissions
- **MainViewModel**: State management for analysis results and UI state
- Jetpack Compose screens for camera preview and results display

### Key Integrations
- **LEAP SDK**: AI model integration for visual analysis (.aar library)
- **CameraX**: Camera handling with live preview and frame analysis
- **LFM2-VL-1.6B Model**: Enhanced 1.6B vision-language model (.bundle file) deployed to `/data/local/tmp/leap/lfm2-vl-1.6b.bundle`

## Development Commands

### Model Deployment
```bash
# Deploy LFM2-VL-1.6B model for hackathon
adb push /home/peter/simspec_v1/models/LFM2-VL-1_6B_8da4w.bundle /data/local/tmp/leap/lfm2-vl-1.6b.bundle
```

### Demo Recording
```bash
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4
```

## Progressive Analysis System

The app uses a 3-stage progressive prompting system:
1. Complexity Scoping - Scale, materials, and project complexity assessment
2. Technical Counting - Feature counting and modeling effort estimation  
3. Business Justification - Simulation value and risk mitigation analysis

Each prompt is applied with 5-second throttling to prevent system overload and ensure optimal mobile performance.

## Performance Considerations

- Frame analysis is throttled to every 5 seconds (optimized based on testing)
- Images are resized to 512x512 for inference performance (may need further optimization to 384x384)
- All AI operations run on background threads (Dispatchers.IO)
- Model initialization occurs once at app startup (~2.5s load time expected)

## Key Dependencies

- LEAP SDK (.aar file in app/libs/)
- CameraX for camera handling
- Jetpack Compose for UI
- Kotlin Coroutines for async operations
- Minimum SDK: API 31

## Engineering Context Intelligence

The app maps detected components to specific analysis types:
- Fasteners → Fatigue Analysis, Stress Concentration
- Welds → Crack Propagation, Residual Stress Analysis  
- Corrosion → Remaining Life Assessment, Material Degradation
- Pipes/Flanges → Fluid Dynamics, Pressure Drop Analysis
- i will manually execute the powershell commands.
- https://leap.liquid.ai/docs the Leap docs are here
- https://github.com/Liquid4All/LeapSDK-Examples/tree/main/Android/VLMExample This seems like an important example
- I added the example repository here: \\wsl.localhost\Ubuntu\home\peter\simspec_v1\examples