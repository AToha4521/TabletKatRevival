# TabletKat Revival Edition

**TabletKat Revival** is a modernized and stabilized version of the original TabletKat Xposed module. It is designed to restore the authentic Android 4.x (ICS/JB) Tablet UI on Android 4.4 KitKat devices, with a focus on stability, performance, and visual accuracy.

This revival edition introduces several critical bug fixes and features that were missing or unstable in the original fork.

---

## 🚀 Key Improvements & Features

### 1. Enhanced Stability (The "Zero-Crash" Focus)
*   **Dual-Pane Settings Stabilization**: Re-implemented the Tablet settings layout to use a modern Fragment-based architecture. This completely eliminates the "flip-back" bug where toggles would reset while scrolling in horizontal mode.
*   **Safety-First Hooking**: All system hooks now use a `SafeHook` wrapper. This prevents SystemUI from bootlooping or crashing if specific methods (like `TvStatusBar`) are missing on your ROM.
*   **Null-Safety**: Hardened the Search Panel and Recent Apps logic to prevent NullPointerExceptions during system startup.

### 2. Authentic Navigation Experience
*   **Custom Firmware Icons**: Integrated high-resolution navigation bar icons extracted directly from tablet firmware for Volume Up, Volume Down, Screenshot, and Menu.
*   **Standardized Spacing**: All navigation buttons are now forced to **80dip** width, ensuring perfect alignment and spacing that matches the stock Back and Home buttons.
*   **Smart Menu Button**: The 3-dots Menu button now behaves like a native system component. It automatically appears only when an app has a menu available and triggers the actual app menu (`KEYCODE_MENU`).

### 3. Visual Accuracy & Polishing
*   **Consolidated "Holo" Theme**: Merged the fragmented ICS/JB options into a single, unified "Holo" theme for a cleaner configuration experience.
*   **High-Visibility Status Icons**: Re-engineered `SignalClusterView` to force blue Holo icons to fill the status bar. Icons are now large, clear, and no longer appear "washed out" or small.
*   **ICS/JB Clock Seconds**: Added a high-frequency clock ticker that supports seconds with authentic scaling (AM/PM is automatically scaled to 0.7x size to match the original Tablet UI metrics).

### 4. System-Level Integration
*   **Native Screenshot Service**: Tapping the Screenshot button now connects directly to the Android `TakeScreenshotService`. 
    *   **No root required** for the screenshot action.
    *   Full support for native shutter animations and status bar notifications.
*   **System-Wide Resource Swapping**: Status icons and battery styles are now replaced at the resource level, ensuring they appear consistently across the entire OS.

---

## 👥 Credits

*   **Revival Developer**: Toha Abdullah
*   **Original Author**: Exalm (alice-mkh)

---

## 🛠️ Configuration
All features can be toggled via the **TabletKat Settings** app. Changes to the Navigation Bar and Clock Seconds apply instantly, while deep theme changes (like Battery styles) may require a SystemUI restart using the built-in "Hard Kill" tool in the About section.
