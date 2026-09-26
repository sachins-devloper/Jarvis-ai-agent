# ⚡ Jarvis - Autonomous Local Android AI Agent

> **"Jarvis, but local and Android-first."**
> A completely on-device, offline-first personal AI assistant built natively with **Kotlin, Android SDK, Jetpack Compose, and an extensible Agent + Tool Registry architecture**.

---

## 📱 Live Snapshot

The application has been compiled, packaged, and verified directly on an active Android device:

- **Local Inference Engine**: On-device intent parser & inference runtime + GGUF mobile loader (SmolLM2, Qwen2.5, Gemma).
- **Agent Architecture**: Autonomous ReAct loop (`Planner` → `ToolRegistry` → Android System Execution → Observation → Natural Language Synthesis).
- **Native Android Tools**: 8 registered tools directly controlling OS subsystems.
- **Deep Android Capabilities**: Foreground & Accessibility Services, Notification Listener Service, Camera Hardware, Alarm Manager, Intents.
- **Voice Pipeline**: Streaming Android `SpeechRecognizer` + TTS with voice synthesis.
- **Local Memory**: Persistent conversation history and user preference store.

---

## 🏛️ System Architecture

```
                 ┌────────────────────────────────┐
                 │       JARVIS ANDROID APP       │
                 │                                │
                 │   🎤 Voice (SpeechRecognizer)  │
                 │               ↓                │
                 │        Text-to-Speech          │
                 └───────────────┬────────────────┘
                                 │
                                 ▼
                 ┌────────────────────────────────┐
                 │         LOCAL AI AGENT         │
                 │                                │
                 │      Local LLM (GGUF / JNI)    │
                 │               ↓                │
                 │        Planner / ReAct         │
                 │               ↓                │
                 │      Structured Tool Call      │
                 └───────────────┬────────────────┘
                                 │
                                 ▼
                 ┌────────────────────────────────┐
                 │      ANDROID TOOL REGISTRY     │
                 │                                │
                 │  • OpenAppTool                 │
                 │  • SetAlarmTool                │
                 │  • OpenBrowserTool             │
                 │  • SendMessageTool             │
                 │  • DeviceControlTool (Torch)   │
                 │  • NotificationTool            │
                 │  • UiInteractionTool           │
                 │  • CreateNoteTool              │
                 └───────────────┬────────────────┘
                                 │
         ┌───────────────────────┼────────────────────────┐
         ▼                       ▼                        ▼
┌──────────────────┐  ┌─────────────────────┐  ┌────────────────────┐
│   Android APIs   │  │Accessibility Service│  │    Local Memory    │
│  (Intents/Alarm/ │  │  (On-Screen Click/  │  │(Room/SQLite Facts &│
│  Audio/Camera)   │  │   Type/Inspect)     │  │  Chat Persistence) │
└──────────────────┘  └─────────────────────┘  └────────────────────┘
```

---

## 📁 Project Directory Structure

```
d:\Me\personal_ai\
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/ (strings, colors, themes)
│       │   └── xml/accessibility_service_config.xml
│       └── java/com/personalai/jarvis/
│           ├── MainActivity.kt               # Permission dispatcher & Compose host
│           ├── JarvisApplication.kt          # Global dependency injection container
│           │
│           ├── agent/                        # Autonomous Agent Engine
│           │   ├── Agent.kt                  # ReAct loop (Think -> Act -> Observe)
│           │   ├── Planner.kt                # Prompt generation & JSON tool extraction
│           │   ├── Tool.kt                   # AgentTool interface & ToolResult models
│           │   └── ToolRegistry.kt           # Dynamic tool registration & schema generator
│           │
│           ├── ai/                           # Local AI Runtime
│           │   ├── LocalLLM.kt               # Central coordinator
│           │   ├── LocalLLMEngine.kt         # Engine interface (flow tokens, load/unload)
│           │   ├── GGUFEngine.kt             # GGUF header parser & mobile llama bridge
│           │   ├── RuleBasedFallbackEngine.kt# Instant offline zero-shot intent engine
│           │   └── ModelManager.kt           # Mobile model downloader & disk manager
│           │
│           ├── tools/                        # Android System Action Tools
│           │   ├── AppTool.kt                # Launch installed apps by label/package
│           │   ├── AlarmTool.kt              # Create exact clock alarms
│           │   ├── BrowserTool.kt            # Web browsing & Google search
│           │   ├── MessageTool.kt            # SMS & WhatsApp drafts
│           │   ├── DeviceControlTool.kt      # Flashlight, volume, Wi-Fi settings
│           │   ├── NotificationTool.kt       # Post & read status notifications
│           │   ├── UiInteractionTool.kt      # Accessibility UI automation
│           │   └── NoteTool.kt               # Scratchpad & persistent fact logger
│           │
│           ├── services/                     # Deep Android Background Services
│           │   ├── JarvisAccessibilityService.kt   # Screen text & UI element clicker
│           │   └── JarvisNotificationListenerService.kt # Incoming notifications buffer
│           │
│           ├── voice/                        # Voice Pipeline
│           │   ├── SpeechRecognizerHelper.kt # Audio recording & RMS volume meter
│           │   └── TextToSpeechHelper.kt     # Deep voice audio responses
│           │
│           ├── memory/                       # Local Memory
│           │   └── MemoryRepository.kt       # Facts, user preferences & chat history
│           │
│           └── ui/                           # Modern Jetpack Compose UI
│               ├── JarvisScreen.kt           # Sci-Fi dark interface & chat stream
│               ├── JarvisViewModel.kt        # StateFlow bridge
│               ├── components/
│               │   ├── GlowingMicButton.kt   # Pulsing cyan mic with RMS ripple
│               │   ├── ToolExecutionCard.kt  # Terminal-style execution card
│               │   ├── ChatMessageBubble.kt  # User & Jarvis bubble styling
│               │   └── SystemStatusBar.kt    # Service & engine status pills
│               └── theme/                    # Cyberpunk / Jarvis obsidian theme
│
└── model/
    └── README.md                             # Guide for placing & loading GGUF files
```

---

## 🛠️ How to Add a New Tool

1. Implement `AgentTool` in `tools/`:
   ```kotlin
   class CustomTool(private val context: Context) : AgentTool {
       override val name = "my_custom_tool"
       override val description = "Performs a custom action on the device."
       override val definition = ToolDefinition(
           name = name,
           description = description,
           parameters = listOf(
               ToolParameter("param1", "string", "Description of parameter")
           )
       )

       override suspend fun execute(arguments: Map<String, Any>): ToolResult {
           val param = arguments["param1"] as? String ?: return ToolResult.error("Missing param")
           // Your Android SDK logic here
           return ToolResult.success("Custom tool executed with $param")
       }
   }
   ```

2. Register it in `JarvisApplication.kt`:
   ```kotlin
   toolRegistry.register(CustomTool(this))
   ```
   The `Planner` and `LocalLLM` automatically inject the tool's JSON schema and description into the model's system prompt!

---

## 📦 Building and Deploying

```bash
# Compile debug APK
.\gradlew.bat assembleDebug

# Install on USB or Wi-Fi connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app directly
adb shell am start -n com.personalai.jarvis/.MainActivity
```
