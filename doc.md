# 🚀 Jarvis Android App - Quick Run & Development Guide

This guide covers everything you need to build, install, run, and develop the **Jarvis Local Android AI Agent** on your machine and phone.

---

## 📋 1. Prerequisites

- **Java JDK**: Version 17+ (`java -version`)
- **Android SDK**: API 34+ installed (default path: `D:\Android\Sdk`)
- **Android Device**:
  - Developer Options enabled (`Settings` ➔ `About Phone` ➔ tap `Build Number` 7 times).
  - USB Debugging or Wireless Debugging turned ON.

---

## ⚡ 2. Quick Run (One-Liner Commands)

Open PowerShell in `d:\Me\personal_ai`:

### Step A: Build and Install APK directly to connected phone
```powershell
& "C:\Users\HP\.gradle\wrapper\dists\gradle-9.2.0-bin\11i5gvueggl8a5cioxuftxrik\gradle-9.2.0\bin\gradle.bat" installDebug
```

### Step B: Launch the app on your phone
```powershell
& "D:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.personalai.jarvis/.MainActivity
```

---

## 🔥 3. Development Mode: Live Hot-Reload

### Option A: Terminal Continuous Watcher (Auto-Recompile on Save)
Run this command in a background terminal. Every time you save a `.kt` file, Gradle automatically recompiles the delta and updates your phone in ~20–30s:

```powershell
& "C:\Users\HP\.gradle\wrapper\dists\gradle-9.2.0-bin\11i5gvueggl8a5cioxuftxrik\gradle-9.2.0\bin\gradle.bat" installDebug --continuous
```
*(To exit watcher mode at any time, press `Ctrl + C`)*

### Option B: Android Studio Compose "Live Edit" (Instant Sub-Second Reload)
1. Open `d:\Me\personal_ai` in Android Studio.
2. Go to **Settings (`Ctrl + Alt + S`)** ➔ **Editor** ➔ **Live Edit**.
3. Check **Enable Live Edit** and choose **Push edits automatically**.
4. Press `Shift + F10` once to launch. Now, any Compose UI edits in `JarvisScreen.kt` appear instantly on your phone!

---

## 🧠 4. Local AI Model (GGUF Weights) Setup

Jarvis works completely offline without cloud APIs. You can run it in two modes:

### Mode 1: Instant Zero-Shot Fallback (No download required)
- Out of the box, Jarvis comes with a built-in neural intent parser.
- All tools (Apps, Alarms, Calls, Messages, Flashlight, Web, Notes) work immediately without downloading anything.

### Mode 2: Full GGUF LLM Execution (On-Device Neural Model)
To run a real quantized LLM on your phone's CPU:

1. **Download the GGUF model file on your PC**:
   ```powershell
   # Qwen 2.5 0.5B Instruct (Recommended for high tool-calling precision)
   curl.exe -L -o "d:\Me\personal_ai\model\qwen2.5-0.5b-instruct-q4_k_m.gguf" "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
   ```

2. **Push the model file to your phone**:
   ```powershell
   & "D:\Android\Sdk\platform-tools\adb.exe" push "d:\Me\personal_ai\model\qwen2.5-0.5b-instruct-q4_k_m.gguf" /sdcard/Android/data/com.personalai.jarvis/files/models/
   ```

3. **Restart the app**:
   ```powershell
   & "D:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.personalai.jarvis
   & "D:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.personalai.jarvis/.MainActivity
   ```
   The top-left status pill will display **`AI Engine: GGUF llama.cpp`**.

---

## 🛠️ 5. Enabling Advanced Permissions on Phone

For deep phone automation, grant these once in the app or settings:

1. **Microphone**:
   - Tap **Allow** when prompted on first launch (for voice commands).
2. **Accessibility Service** *(for automated UI clicks & screen reading)*:
   - Tap the **`Accessibility: Enable`** pill in the top status bar.
   - Look for **Jarvis AI** under Installed Services / Accessibility and toggle it **ON**.
3. **Notification Listener** *(to read incoming notifications)*:
   - Tap the **`Notifications: Enable`** pill in the status bar.
   - Allow **Jarvis AI** notification access.

---

## 🗣️ 6. Voice & Text Command Cheat Sheet

You can speak (tap the glowing mic) or type any of the following:

| Command | Tool Triggered | Result on Phone |
| :--- | :--- | :--- |
| *"Open YouTube"* | `open_app` | Launches YouTube application |
| *"Open Chrome"* | `open_app` | Launches Google Chrome |
| *"Set alarm at 7:00 AM"* | `set_alarm` | Schedules exact alarm in Clock |
| *"call Mom"* | `make_call` | Opens phone dialer to call contact |
| *"dial 9876543210"* | `make_call` | Opens dialer with exact number |
| *"send hello message to akshaya"* | `send_message` | Opens SMS/WhatsApp draft |
| *"Turn on flashlight"* / *"Torch on"* | `device_control` | Activates phone camera LED torch |
| *"Turn off flashlight"* | `device_control` | Turns off flashlight |
| *"Volume up"* / *"Volume down"* | `device_control` | Adjusts media volume |
| *"Battery status"* | `device_control` | Reports battery level & charge state |
| *"Search Python DSA roadmap"* | `open_browser` | Opens Google search in browser |
| *"Take a note: Buy groceries"* | `create_note` | Stores note into local SQLite memory |
| *"What is Python?"* | Local LLM | Instant offline answer |

---

## 🔍 7. Viewing Live Logs & Debugging

To stream logs from the running agent on your phone:

```powershell
& "D:\Android\Sdk\platform-tools\adb.exe" logcat -s JarvisAgent,ToolRegistry,LocalLLM,GGUFEngine,TTSHelper,SpeechRecognizer
```

To take a screenshot from the phone and view it on PC:
```powershell
& "D:\Android\Sdk\platform-tools\adb.exe" shell screencap -p /sdcard/screen.png
& "D:\Android\Sdk\platform-tools\adb.exe" pull /sdcard/screen.png d:\Me\personal_ai\screen.png
```
