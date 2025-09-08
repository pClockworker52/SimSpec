# SimSpec: AI-Powered Engineering Component Analysis

<div align="center">

![SimSpec Logo](https://img.shields.io/badge/SimSpec-v1.0-blue?style=for-the-badge)
![LEAP SDK](https://img.shields.io/badge/LEAP%20SDK-LFM2--VL--1.6B-green?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-API%2031+-orange?style=for-the-badge)

*Transform any mechanical component into actionable CAE simulation requirements*

[Features](#features) • [Installation](#installation) • [Usage](#usage) • [Architecture](#architecture) • [Hackathon](#hackathon-submission)

</div>

## Overview

SimSpec is a revolutionary Android application that uses advanced AI vision analysis to transform photos of mechanical components into professional engineering simulation requests. Built with Liquid AI's LFM2-VL-1.6B vision-language model, SimSpec bridges the gap between field observation and CAE engineering analysis.

### What SimSpec Does

🔍 **Capture**: Take 3 photos of any mechanical component  
🤖 **Analyze**: AI performs systematic engineering decomposition  
📋 **Generate**: Creates professional simulation scoping reports  
📤 **Share**: Export ready-to-use CAE project requests  

## Features

### 🎯 **3-Stage Progressive Analysis**
- **Stage 1: Technical Decomposition** - Component identification, materials, assembly methods
- **Stage 2: Failure Mode Analysis** - Stress concentrations, wear points, critical joints  
- **Stage 3: Simulation Questions** - Engineering questions, analysis types, complexity estimation

### 📱 **Modern Android Architecture**
- **Jetpack Compose** - Modern, responsive UI
- **CameraX Integration** - Seamless photo capture workflow
- **LEAP SDK** - Edge AI processing with LFM2-VL-1.6B model
- **Clean Architecture** - Maintainable, testable codebase

### 📊 **Professional Outputs**
- **Simulation Scoping Reports** - Industry-standard format
- **Business Justification** - ROI and risk assessment
- **Technical Specifications** - Load paths, constraints, stress risers
- **Complexity Estimates** - Realistic project scope and hours

### ⚡ **Performance Optimized**
- **Edge Processing** - No cloud dependency, complete privacy
- **5-Second Analysis** - Rapid component assessment
- **Mobile-First** - Optimized for field use by engineers

## Installation

### Prerequisites
- Android device with **API 31+** (Android 12+)
- **Camera permission** for component photography
- **4GB+ RAM** recommended for AI model processing

### Model Deployment
Deploy the LFM2-VL-1.6B model to your Android device:

```bash
adb push path/to/LFM2-VL-1_6B_8da4w.bundle /data/local/tmp/leap/lfm2-vl-1.6b.bundle
```

### Build & Install
```bash
# Clone repository
git clone [repository-url]
cd simspec_v1

# Build APK
./gradlew assembleDebug

# Install to device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

### Quick Start
1. **Launch SimSpec** - Grant camera permissions when prompted
2. **Position Component** - Frame mechanical part in camera view
3. **Take 3 Photos**:
   - Photo 1: Overall view showing full component
   - Photo 2: Close-up of connections and joints
   - Photo 3: Areas of concern or complex features
4. **Review Analysis** - AI performs 3-stage technical breakdown
5. **Export Report** - Share professional simulation request

### Example Workflow: Office Chair Analysis

**Input**: 3 photos of an office chair
**Stage 1**: Identifies pneumatic cylinder, welded base, caster wheels
**Stage 2**: Notes stress concentrations at arm mounts, wear on casters
**Stage 3**: Recommends fatigue analysis for base welds, suggests BIFMA compliance testing
**Output**: Professional simulation scoping report ready for CAE team

### Supported Component Types
- ✅ **Mechanical Assemblies** - Brackets, frames, fixtures
- ✅ **Welded Structures** - Joints, seams, fabrications
- ✅ **Fastened Components** - Bolted, screwed, riveted assemblies
- ✅ **Consumer Products** - Chairs, appliances, tools (with engineering context)
- ✅ **Industrial Equipment** - Pipes, flanges, machinery components

## Architecture

### Core Components

```
SimSpec Architecture
├── 📱 UI Layer (Jetpack Compose)
│   ├── PhotoCaptureScreen - 3-photo workflow
│   ├── AnalysisResultsView - Real-time AI output
│   └── ExportShareFlow - Professional report generation
├── 🧠 Services Layer
│   ├── LeapService - LFM2-VL-1.6B integration
│   ├── PhotoAnalysisProcessor - 3-stage pipeline
│   └── ExportService - Report generation
├── 🔧 Utils Layer
│   ├── FrameExtractor - Image processing
│   ├── ContextInterpreter - Engineering analysis mapping
│   └── Templates - Professional report formats
└── 📊 Data Layer
    ├── Analysis Results - Structured AI outputs
    └── Export Templates - Industry-standard formats
```

### Key Technologies
- **Liquid AI LEAP SDK** - Edge AI inference
- **LFM2-VL-1.6B Model** - 1.6B parameter vision-language model (367MB)
- **Android CameraX** - Modern camera API
- **Kotlin Coroutines** - Asynchronous processing
- **Material 3 Design** - Modern Android UI

## Performance Metrics

- **Model Load Time**: ~4.8 seconds (one-time initialization)
- **Analysis Speed**: ~20 seconds per stage
- **Model Size**: 1134 MB (deployed to device)
- **Memory Usage**: Optimized for mobile devices
- **Processing**: 100% on-device, no cloud dependency

## Professional Output Example

```markdown
Simulation Scoping Report - Office Chair Gas Cylinder

1. Initial Scoping Assessment
Request ID: SIMSPEC-2025-B7A9
System Function: Pneumatic height adjustment mechanism
Apparent Complexity: Small Assembly (2-5 parts)
Key Components: Steel cylinder, polymer seat interface, pressure seal

2. FEA Model Complexity Analysis  
Connections: Pressure-fit interface, threaded mounting
Load Path: User weight → seat → cylinder → base transfer
Stress Risers: Seal groove, thread engagement, mounting interface

3. Simulation Value Proposition
Primary Question: Will cylinder maintain seal integrity under cyclic loading?
Analysis Type: Fatigue Life Analysis with contact pressure assessment
Business Impact: Prevent warranty claims, ensure BIFMA compliance

4. Recommendation
Medium complexity analysis. Recommend fatigue study with 100k cycle validation.
```

## Development

### Building from Source
```bash
# Setup
git clone [repository]
cd simspec_v1/simspec-android/simspec-android

# Dependencies
./gradlew build

# Deploy model
adb push models/LFM2-VL-1_6B_8da4w.bundle /data/local/tmp/leap/lfm2-vl-1.6b.bundle

# Run
./gradlew installDebug
```

### Configuration
Key configuration files:
- `CLAUDE.md` - Development guidelines and deployment commands  
- `gradle.properties` - Build configuration
- `AndroidManifest.xml` - Permissions and app configuration

## Hackathon Submission

**Created for**: Liquid AI Hackathon 2025  
**Category**: Edge AI Applications  
**Model Used**: LFM2-VL-1.6B Vision-Language Model  
**Unique Value**: First mobile app to transform mechanical component photos into professional CAE simulation requests  

### Innovation Highlights
- **Novel Application**: CAE engineering + mobile AI vision
- **Professional Output**: Industry-ready simulation scoping reports  
- **Edge Processing**: Complete analysis on mobile device
- **Field-Ready**: Designed for engineers working on-site

## License

This project was developed for the Liquid AI Hackathon 2025.

---

<div align="center">

**Built with ❤️ using Liquid AI's LFM2-VL-1.6B**  
*Transforming Engineering Analysis, One Photo at a Time*

</div>