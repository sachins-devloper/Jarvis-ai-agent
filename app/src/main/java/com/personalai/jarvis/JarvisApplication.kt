package com.personalai.jarvis

import android.app.Application
import com.personalai.jarvis.agent.Agent
import com.personalai.jarvis.agent.ToolRegistry
import com.personalai.jarvis.ai.LocalLLM
import com.personalai.jarvis.memory.MemoryRepository
import com.personalai.jarvis.tools.CallTool
import com.personalai.jarvis.tools.CreateNoteTool
import com.personalai.jarvis.tools.DeviceControlTool
import com.personalai.jarvis.tools.NotificationTool
import com.personalai.jarvis.tools.OpenAppTool
import com.personalai.jarvis.tools.OpenBrowserTool
import com.personalai.jarvis.tools.SendMessageTool
import com.personalai.jarvis.tools.SetAlarmTool
import com.personalai.jarvis.tools.UiInteractionTool
import com.personalai.jarvis.voice.SpeechRecognizerHelper
import com.personalai.jarvis.voice.TextToSpeechHelper

class JarvisApplication : Application() {

    lateinit var memoryRepository: MemoryRepository
        private set
    lateinit var toolRegistry: ToolRegistry
        private set
    lateinit var localLLM: LocalLLM
        private set
    lateinit var agent: Agent
        private set
    lateinit var speechRecognizer: SpeechRecognizerHelper
        private set
    lateinit var textToSpeech: TextToSpeechHelper
        private set

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Memory
        memoryRepository = MemoryRepository(this)

        // 2. Initialize Tool Registry and register all Android Tools
        toolRegistry = ToolRegistry().apply {
            register(OpenAppTool(this@JarvisApplication))
            register(SetAlarmTool(this@JarvisApplication))
            register(OpenBrowserTool(this@JarvisApplication))
            register(SendMessageTool(this@JarvisApplication))
            register(DeviceControlTool(this@JarvisApplication))
            register(NotificationTool(this@JarvisApplication))
            register(UiInteractionTool(this@JarvisApplication))
            register(CallTool(this@JarvisApplication))
            register(CreateNoteTool(memoryRepository))
        }

        // 3. Initialize Local AI Runtime
        localLLM = LocalLLM(this)

        // 4. Initialize Agent
        agent = Agent(
            toolRegistry = toolRegistry,
            localLLM = localLLM,
            memoryRepository = memoryRepository
        )

        // 5. Initialize Voice Helpers
        speechRecognizer = SpeechRecognizerHelper(this)
        textToSpeech = TextToSpeechHelper(this)
    }
}
