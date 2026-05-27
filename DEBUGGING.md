# Debugging TabletKat Revival

This guide explains how to set up an environment for testing and debugging the TabletKat Xposed module.

## 1. Create a Tablet Emulator (API 19)

Since `avdmanager` is not available in the command line on this system, please create the emulator manually in Android Studio:

1.  Open **Device Manager** (Virtual Device Manager).
2.  Click **Create Device**.
3.  Select **Tablet** on the left.
4.  Choose **Nexus 10** (it has the correct resolution for the "Authentic Tablet UI").
5.  Click **Next**.
6.  Select **KitKat (API 19)**. If you don't see it, click the download icon next to it.
    *   *Note: Choose an image with "Google APIs" but WITHOUT "Google Play Store" for easier rooting.*
7.  Finish the wizard and name it `TabletKat_Emulator`.

## 2. Tools Downloaded

I have prepared the following tools in the `tools/` directory:

*   **rootAVD**: A tool to root Android Virtual Devices.
    *   Location: `tools/rootAVD/`
    *   To use it:
        1.  Start your emulator.
        2.  Open a terminal in `tools/rootAVD/`.
        3.  Run: `./rootAVD.sh system-images/android-19/google_apis/x86/ramdisk.img` (Adjust the path based on your system image).

## 3. Install Xposed

You will need to install the Xposed Installer and Framework on the emulator:

1.  **Download**: [Xposed Installer 2.7 experimental1](https://repo.xposed.info/module/de.robv.android.xposed.installer) (Best for KitKat).
2.  **Install**: `adb install XposedInstaller.apk`
3.  **Setup**:
    *   Open Xposed Installer on the emulator.
    *   Go to **Framework** -> **Install/Update**.
    *   Grant Root access.
    *   **Reboot** the emulator.

## 4. Run & Test

1.  Build and install the TabletKat module:
    ```bash
    ./gradlew installDebug
    ```
2.  Open **Xposed Installer** -> **Modules**.
3.  Enable **TabletKat Revival**.
4.  **Soft Reboot** from the Xposed Installer.

## 5. Logcat Debugging

Filter logs in the Android Studio Logcat tab:
`tag:TabletKat`
