# 📱 TabletKat Revival
### Restoring the authentic ICS/JB Tablet UI to Android 4.4 KitKat

[![Platform](https://img.shields.io/badge/Platform-Android%204.4-green.svg)](https://developer.android.com/about/versions/kitkat)
[![Xposed](https://img.shields.io/badge/Framework-Xposed-orange.svg)](https://repo.xposed.info/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Download](https://img.shields.io/badge/Download-APK-orange.svg)](https://github.com/AToha4521/TabletKatRevival/releases)

**TabletKat Revival** is a modernized and stabilized version of the original TabletKat Xposed module. It restores the authentic Android 4.x (ICS/JB) Tablet UI on Android 4.4 KitKat devices, focusing on stability, performance, and visual accuracy.

---

## 📸 Previews

<img src="screenshots/1.png?raw=true"> 
<img src="screenshots/2.png?raw=true"> 
<img src="screenshots/3.png?raw=true"> 
<img src="screenshots/4.png?raw=true">
<img src="screenshots/5.png?raw=true"> 
<img src="screenshots/6.png?raw=true"> 
<img src="screenshots/7.png?raw=true"> 
<img src="screenshots/8.png?raw=true"> 

---

## Table of Contents
- [What is this?](#what-is-this)
- [Requirements](#requirements)
- [Key Improvements](#key-improvements)
- [Features](#features)
- [ROM Compatibility](#rom-compatibility)
- [Downloads](#downloads)
- [Building and Installation](#building-and-installation)
- [Debugging](#debugging)
- [Project Structure](#project-structure)
- [License and Credits](#license-and-credits)

---

## What is this?
TabletKat is an **Xposed module** that overrides the phone UI on KitKat. It repositions the system bar to the bottom and reconstructs the notification and recent apps panels to match the classic tablet interface that Google deprecated after Android 4.3.

This revival addresses critical stability issues and adds polished features like high-res navigation icons and a native screenshot service.

---

## Requirements
| Component | Requirement |
|:---|:---|
| **Android Version** | 4.4 KitKat (API 19) **only** |
| **Framework** | Xposed Framework (v54 or newer) |
| **Access** | Root Access required |
| **Device Type** | Optimized for 7" and 10" tablets |

---

## Key Improvements

### Stability & Safety
*   **SafeHook Engine**: All system hooks run through a wrapper that catches `NoSuchMethodError`, preventing SystemUI crashes on custom ROMs or missing components like `TvStatusBar`.
*   **Dual-Pane Stabilization**: Re-implemented the Tablet settings layout using a Fragment-based architecture. 
*   **TouchWiz Protection**: Automatically detects Samsung's framework and bails safely to prevent bootloops.

### Visual & Logic Fixes
*   **Standardized Spacing**: Navigation buttons are forced to **80dip** width for perfect alignment with stock buttons.
*   **Smart Menu Button**: The 3-dots Menu button behaves like a native component—it only appears when an app has a menu and triggers the actual `KEYCODE_MENU`.
*   **High-Visibility Icons**: Re-engineered `SignalClusterView` to force blue Holo icons to fill the status bar properly.

---

## Features

### Theme Styles
- **Holo Unified**: A consolidated theme for ICS/JB aesthetics with holographic dark navy backgrounds (`#CC1A1F2E`).
- **KitKat Stock**: Keeps the system-default colors and transparency while maintaining the tablet layout.

### Status Bar & Notifications
- **Compact Panel**: Right-anchored narrow notification area (classic ICS tablet style).
- **Clock Seconds**: Supports a high-frequency ticker for seconds with authentic scaling (AM/PM scaled to 0.7x).
- **Battery Customization**: Display percentage and choose between Bar, Circle, or Text styles.

### Navigation Bar Extra Buttons
Add functional shortcuts to the right side of the system bar:
- **Volume +/-**: Quick audio control with custom high-res icons.
- **Screenshot**: Native service connection (No root required for action, includes shutter animation).
- **Sleep/Power**: Screen off (short press) or Power Menu (long press).

---

## ROM Compatibility

| ROM | Status | Notes |
|:---|:---:|:---|
| **AOSP KitKat 4.4.x** | ✅ | Perfect compatibility. |
| **CyanogenMod 11** | ⚠️ | Works, but some advanced hooks skip silently. |
| **OmniROM / AOSPA** | ⚠️ | SafeHook prevents crashes; mostly functional. |
| **TouchWiz (Samsung)** | ❌ | **Incompatible** due to framework rewrites. |
| **Lenovo VibeUI** | ❌ | **Incompatible**; causes UI glitches. |

---

## Downloads
You can find the latest stable APKs in the **[Releases](https://github.com/AToha4521/TabletKatRevival/releases)** section.

---

## Building and Installation

### Build from Source
```bash
# Clone the repository
git clone https://github.com/AToha4521/TabletKatRevival.git
cd tabletkat-revival

# Compile the APK
./gradlew assembleDebug
```
*Requires Android Studio Giraffe+ or Gradle 7.5+.*

### Installation
1. Install the APK and enable it in the **Xposed Installer**.
2. Reboot your device or use the **"Restart SystemUI"** button in TabletKat Settings.

---

## Debugging

### Emulator Setup
Filter logs using these tags for targeted debugging:
```bash
adb logcat -s "TabletKat" "TabletKat/SafeHook" "TabletKat/StatusBar"
```
For a full tablet experience, use a **Nexus 10** emulator image with **Android 4.4 (API 19)**.

---

## Project Structure
- `TabletKatModule.java`: Main Xposed entry point.
- `SafeHook.java`: The crash-prevention engine.
- `statusbar/tablet/`: Tablet system bar and notification logic.
- `recent/`: Reconstructed tablet Recent Apps interface.
- `navbuttons/`: Logic for extra navigation bar buttons.

---

## License and Credits
- **License**: Apache License 2.0 (See `LICENSE` file).
- **Original Author**: Exalm (alice-mkh)
- **Revival Developer**: Toha Abdullah
