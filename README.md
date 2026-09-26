# ⚡ Jarvis - Autonomous Android AI Agent

> **"Jarvis, but local, privacy-first, and Android-native."**  
> An autonomous on-device personal AI assistant built natively with **Kotlin, Android SDK, Jetpack Compose, and an extensible ReAct Agent + Tool Registry architecture**.

---

## 🎯 The Problem

1. **Privacy & Data Exploitation**: Modern smartphone assistants (Siri, Google Assistant, Gemini) transmit sensitive personal data—including private contacts, search history, text messages, photos, and voice recordings—to remote corporate servers.
2. **Offline Helplessness**: Conventional cloud-based assistants fail or degrade significantly without an active internet connection, leaving users without basic device control in low-connectivity areas.
3. **Lack of True Android Autonomy**: Cloud assistants cannot deeply interact with the Android OS. They struggle to search local files directly across storage volumes, control hardware toggles without cloud latency, inspect on-screen UI elements, or execute multi-step deterministic tool workflows.
4. **Cloud Hallucinations**: Standard LLM chat apps generate theoretical answers without the ability to inspect the actual system state, check battery levels, verify contact numbers, or observe execution outcomes.

---

## 💡 The Solution

**Jarvis** bridges the gap between large language models and native Android system capabilities through an autonomous **ReAct (Reason + Act + Observe)** agent loop:

- **Privacy-First Architecture**: Capable of running 100% on-device using local GGUF quantized models (SmolLM2, Qwen2.5, Gemma) via a native `llama.cpp` JNI engine, backed by an instant offline zero-shot rule engine.
- **Autonomous Tool Execution**: The agent breaks complex user requests into discrete, structured tool calls, executes them directly against Android APIs, inspects the returned system observations, and synthesizes accurate responses.
- **Optional Hybrid Intelligence**: For high-order reasoning and vision tasks, users can enable OpenAI Cloud integration (GPT-4o / GPT-4o-mini) with end-to-end user-managed API keys.
- **Deep Operating System Integration**: Direct hardware access (camera, torch, audio), local media file search (`MediaStore`), background accessibility automation, and notification listeners.

---

## ✨ Comprehensive Feature List

### 🧠 Dual Intelligence Engine (Local & Cloud)
- **Local On-Device Engine**: Run quantized GGUF models directly on the mobile GPU/NPU/CPU via `llama-kotlin-android` (`llama.cpp`).
- **Instant Offline Rule-Based Fallback**: Deterministic zero-latency intent engine for offline device actions (alarms, phone calls, device toggles, torch) with zero battery overhead.
- **OpenAI Cloud Integration**: Switch on GPT-4o or GPT-4o-mini with customizable system prompts, temperature controls, and streaming responses.
- **Active Provider Indicator**: Emerald green status badge and subtitle dynamically indicating active cloud reasoning vs. local neural execution.

### 👁️ Multimodal Vision & Camera AI
- **In-Bar Camera Snapshot**: Tap the camera icon directly inside the chat input field to snap photos for real-time visual analysis.
- **Gallery Image Selector**: Pick existing images and documents from the Android Photo Picker.
- **Downscaling & Memory Optimization**: Smart downscaling and base64 compression to preserve memory while maintaining optimal token quality for GPT-4o Vision.
- **Interactive Image Staging**: Removable preview chips above the input bar and embedded photo thumbnails in conversation history.

### 🔍 On-Device File & Media Search
- **Instant Media Indexing**: Search across photos (`IMAGE`), music tracks (`AUDIO`), videos (`VIDEO`), and documents (`PDF`, `DOCX`, `TXT`) using native `MediaStore` content queries.
- **Interactive File Cards**: Terminal-style UI cards displaying file name, size, type, and storage location.
- **Direct System Launch**: Tap **"Open File"** to view any found media with the default Android viewer, or tap **"Folder"** to open the parent directory in the device file manager via secure `FileProvider`.

### 📞 Smart Contacts & Phone Calling
- **Intelligent Address Book Search**: Query device contacts by first name, last name, or nickname with fuzzy matching.
- **Contact Disambiguation**: When multiple matches are found (e.g., "John Home" vs. "John Mobile"), Jarvis displays interactive selection chips to confirm the desired recipient.
- **Direct Call & Dialer Modes**: Support for instant phone dialing or launching the native Android dialer keypad with pre-filled numbers.

### 💬 Messaging & Communication
- **SMS Automation**: Compose and dispatch text messages directly through the Android telephony manager.
- **WhatsApp Integration**: Launch chat threads in WhatsApp with prefilled message drafts for any contact or phone number.

### ⚙️ Hardware & Device Controls
- **Flashlight Control**: Toggle the device torch on or off using `CameraManager`.
- **Volume Management**: Adjust media, ring, and notification volume sliders hands-free.
- **Battery & Power Diagnostic**: Query battery percentage, charging status, and power-saving state in real-time.
- **System Settings Launchers**: Instant shortcuts to Android Wi-Fi, Bluetooth, Display, and Accessibility settings.

### ⏰ Alarms, Timers & Scratchpad Notes
- **Exact Clock Alarms**: Set verified clock alarms at specific times using `AlarmManager`.
- **Persistent Notes**: Save quick thoughts, checklists, or key facts to an on-device SQLite database via Room.
- **Web Navigation**: Trigger Google searches or open specific URLs with Chrome or the user's default browser.

### 🎙️ Voice Pipeline & Sci-Fi Cyberpunk UI
- **Live RMS Mic Animation**: Real-time microphone wave pulsation reacting to the user's voice intensity.
- **Text-to-Speech (TTS)**: Natural voice synthesis with automatic playback and a top-bar instant mute button.
- **Rich Markdown Formatting**: Bold headings, bullet points, monospace code blocks, and clickable hyperlinks.
- **Transparent Reasoning Cards**: Expandable terminal cards showing tool invocation parameters, execution duration, and raw system outputs.

### 🛡️ Deep Background Services
- **Accessibility Service (`JarvisAccessibilityService`)**: Reads on-screen text and automates button clicks on third-party apps for hands-free device navigation.
- **Notification Listener (`JarvisNotificationListenerService`)**: Reads incoming status notifications so Jarvis can answer questions about recent alerts and messages.

---

## 🏛️ System Architecture

```
                 ┌────────────────────────────────┐
                 │       JARVIS ANDROID APP       │
                 │                                │
                 │   🎤 Voice (SpeechRecognizer)  │
                 │   📷 Camera & Gallery Picker   │
                 │   💬 Jetpack Compose UI        │
                 └───────────────┬────────────────┘
                                 │
                                 ▼
                 ┌────────────────────────────────┐
                 │         AGENT ENGINE           │
                 │                                │
                 │  Local LLM (GGUF) / OpenAI API │
                 │               ↓                │
                 │      ReAct Loop (Planner)      │
                 │               ↓                │
                 │      Structured Tool Calls     │
                 └───────────────┬────────────────┘
                                 │
                                 ▼
                 ┌────────────────────────────────┐
                 │      ANDROID TOOL REGISTRY     │
                 │                                │
                 │  • ContactTool                 │
                 │  • CallTool                    │
                 │  • MessageTool (SMS / WhatsApp)│
                 │  • FileSearchTool (MediaStore) │
                 │  • AnalyzeImageTool (Vision)   │
                 │  • DeviceControlTool (Torch)   │
                 │  • BatteryTool                 │
                 │  • AlarmTool                   │
                 │  • BrowserTool                 │
                 │  • NoteTool                    │
                 │  • UiInteractionTool           │
                 └───────────────┬────────────────┘
                                 │
         ┌───────────────────────┼────────────────────────┐
         ▼                       ▼                        ▼
┌──────────────────┐  ┌─────────────────────┐  ┌────────────────────┐
│   Android APIs   │  │Accessibility Service│  │    Local Memory    │
│(Camera, MediaStore│  │  (On-Screen Click, │  │(Room/SQLite Facts &│
│ Telephony, Alarm)│  │   Text Inspection)  │  │  Chat Persistence) │
└──────────────────┘  └─────────────────────┘  └────────────────────┘
```

---

## 📁 Project Directory Structure

```
d:\Me\personal_ai\
├── app/
│   ├── build.gradle.kts                      # Build configuration, NDK packaging, dependencies
│   └── src/main/
│       ├── AndroidManifest.xml               # Permissions (Camera, Contacts, Calls, Storage)
│       ├── res/
│       │   ├── drawable/ic_ai_logo.xml       # Custom vector logo
│       │   └── xml/accessibility_service_config.xml
│       └── java/com/personalai/jarvis/
│           ├── MainActivity.kt               # Permission dispatcher & Compose container
│           ├── JarvisApplication.kt          # Global dependency injection container
│           │
│           ├── agent/                        # Autonomous Agent Engine
│           │   ├── Agent.kt                  # ReAct loop (Think -> Act -> Observe)
│           │   ├── Planner.kt                # Prompt generation & JSON tool extraction
│           │   ├── Tool.kt                   # AgentTool interface & ToolResult models
│           │   └── ToolRegistry.kt           # Dynamic tool registration & schema generator
│           │
│           ├── ai/                           # Local & Cloud AI Runtime
│           │   ├── LocalLLM.kt               # Central coordinator & fallback selector
│           │   ├── OpenAIEngine.kt           # OpenAI GPT-4o / GPT-4o-mini & Vision client
│           │   ├── GGUFEngine.kt             # Native llama.cpp GGUF loader & inference
│           │   ├── RuleBasedFallbackEngine.kt# Instant offline zero-shot intent engine
│           │   └── ModelManager.kt           # Model file downloader & storage manager
│           │
│           ├── tools/                        # Android System Action Tools
│           │   ├── ContactTool.kt            # Contact lookup & fuzzy search
│           │   ├── CallTool.kt               # Phone call dispatcher
│           │   ├── MessageTool.kt            # SMS & WhatsApp draft launcher
│           │   ├── FileSearchTool.kt         # MediaStore search for media & documents
│           │   ├── AnalyzeImageTool.kt       # Multimodal visual analysis
│           │   ├── BatteryTool.kt            # Power & charging status query
│           │   ├── DeviceControlTool.kt      # Flashlight, volume, and settings toggles
│           │   ├── AlarmTool.kt              # Exact clock alarms
│           │   ├── BrowserTool.kt            # Web browsing & search queries
│           │   ├── NoteTool.kt               # Scratchpad & persistent fact logger
│           │   └── UiInteractionTool.kt      # Accessibility UI automation
│           │
│           ├── services/                     # Deep Android Background Services
│           │   ├── JarvisAccessibilityService.kt   # Screen text & UI element clicker
│           │   └── JarvisNotificationListenerService.kt # Incoming notifications buffer
│           │
│           ├── voice/                        # Voice Pipeline
│           │   ├── SpeechRecognizerHelper.kt # Audio recording & RMS volume meter
│           │   └── TextToSpeechHelper.kt     # Natural TTS voice responses
│           │
│           ├── memory/                       # Local Memory
│           │   └── MemoryRepository.kt       # Facts, user preferences & chat history
│           │
│           └── ui/                           # Modern Jetpack Compose UI
│               ├── JarvisScreen.kt           # Main chat interface & input pill
│               ├── SettingsScreen.kt         # Model config, API keys & diagnostics
│               ├── AboutFeaturesScreen.kt    # In-app feature guide & roadmap
│               ├── JarvisViewModel.kt        # StateFlow bridge
│               └── components/
│                   ├── GlowingMicButton.kt   # Pulsing cyan mic with RMS ripple
│                   ├── ChatMessageBubble.kt  # User/Jarvis bubbles & image previews
│                   ├── MarkdownText.kt       # Markdown renderer & file action cards
│                   └── ToolExecutionCard.kt  # Terminal-style execution card
```

---

## 🛠️ Adding a New Tool

1. Implement the `AgentTool` interface in `tools/`:
   ```kotlin
   class CustomTool(private val context: Context) : AgentTool {
       override val name = "my_custom_tool"
       override val description = "Performs a custom action on the device."
       override val definition = ToolDefinition(
           name = name,
           description = description,
           parameters = listOf(
               ToolParameter("query", "string", "The input query", required = true)
           )
       )

       override suspend fun execute(arguments: Map<String, Any>): ToolResult {
           val query = arguments["query"] as? String ?: return ToolResult.error("Missing query parameter")
           // Android SDK logic here
           return ToolResult.success("Custom tool executed successfully: $query")
       }
   }
   ```

2. Register it in `JarvisApplication.kt`:
   ```kotlin
   toolRegistry.register(CustomTool(this))
   ```
   The `Planner` automatically injects the new tool's JSON schema and description into the model's system prompt!

---

## 🚀 Building & Deploying

### Debug Build (Development)
```powershell
# Compile debug APK
.\gradlew assembleDebug

# Install on connected device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch the app
adb shell am start -n com.personalai.jarvis/.MainActivity
```

### Production Build (Release)
1. **Generate a Keystore**:
   ```powershell
   keytool -genkey -v -keystore release-key.jks -alias jarvis-key -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Compile Release APK** (for direct distribution):
   ```powershell
   .\gradlew assembleRelease
   # Output: app\build\outputs\apk\release\app-release.apk
   ```
3. **Compile Release App Bundle** (for Google Play Store):
   ```powershell
   .\gradlew bundleRelease
   # Output: app\build\outputs\bundle\release\app-release.aab
   ```
