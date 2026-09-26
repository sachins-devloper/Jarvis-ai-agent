package com.personalai.jarvis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personalai.jarvis.JarvisApplication
import com.personalai.jarvis.agent.AgentEvent
import com.personalai.jarvis.ai.ModelInfo
import com.personalai.jarvis.ai.ModelManager
import com.personalai.jarvis.memory.ChatMessage
import com.personalai.jarvis.services.JarvisAccessibilityService
import com.personalai.jarvis.services.JarvisNotificationListenerService
import com.personalai.jarvis.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val messages: List<ChatMessage> = emptyList(),
    val activeEvent: AgentEvent = AgentEvent.Idle,
    val isListening: Boolean = false,
    val rmsLevel: Float = 0f,
    val isSpeaking: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val isNotificationListenerActive: Boolean = false,
    val engineName: String = "RuleBasedFallbackEngine",
    val isOpenAiEnabled: Boolean = false,
    val openAiKey: String = "",
    val openAiModel: String = "gpt-4o-mini",
    val showApiKeyDialog: Boolean = false,
    val toolCount: Int = 0,
    val downloadProgress: Map<String, Int> = emptyMap(),
    val dynamicSuggestions: List<String> = emptyList(),
    val speechError: String? = null,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.05f,
    val ttsPitch: Float = 0.95f
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val agent = app.agent
    private val localLLM = app.localLLM
    private val memoryRepo = app.memoryRepository
    private val speechRecognizer = app.speechRecognizer
    private val tts = app.textToSpeech

    private val _uiState = MutableStateFlow(
        UiState(
            isOpenAiEnabled = localLLM.isOpenAiEnabled(),
            openAiKey = localLLM.getOpenAiKey(),
            openAiModel = localLLM.getOpenAiModel(),
            autoSpeak = tts.autoSpeak,
            speechRate = tts.speechRate,
            ttsPitch = tts.pitch
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Load initial messages and check for local GGUF models
        viewModelScope.launch {
            if (!localLLM.isOpenAiEnabled()) {
                val localModels = localLLM.modelManager.getLocalModels()
                if (localModels.isNotEmpty() && !localLLM.isUsingGGUF()) {
                    val modelFile = localModels.first()
                    localLLM.loadGGUFModel(modelFile)
                }
            }
            val initialMsgs = memoryRepo.getRecentMessages(30)
            _uiState.value = _uiState.value.copy(
                messages = initialMsgs,
                engineName = localLLM.getActiveEngineName(),
                toolCount = app.toolRegistry.getAllTools().size,
                isAccessibilityActive = JarvisAccessibilityService.isRunning(),
                isNotificationListenerActive = JarvisNotificationListenerService.isRunning()
            )
        }

        // Collect agent events
        viewModelScope.launch {
            agent.events.collect { event ->
                _uiState.value = _uiState.value.copy(activeEvent = event)
                if (event is AgentEvent.Completed) {
                    val updated = memoryRepo.getRecentMessages(30)
                    _uiState.value = _uiState.value.copy(
                        messages = updated,
                        dynamicSuggestions = event.suggestions
                    )
                }
            }
        }

        // Collect speech recognizer voice states
        viewModelScope.launch {
            speechRecognizer.voiceState.collect { voiceState ->
                when (voiceState) {
                    is VoiceState.Listening -> _uiState.value = _uiState.value.copy(isListening = true, speechError = null)
                    is VoiceState.Idle -> _uiState.value = _uiState.value.copy(isListening = false)
                    is VoiceState.Recognized -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                        submitQuery(voiceState.text, speakResult = true)
                    }
                    is VoiceState.Error -> {
                        _uiState.value = _uiState.value.copy(isListening = false, speechError = voiceState.message)
                    }
                }
            }
        }

        // Collect RMS audio level
        viewModelScope.launch {
            speechRecognizer.rmsLevel.collect { rms ->
                _uiState.value = _uiState.value.copy(rmsLevel = rms)
            }
        }

        // Collect TTS state
        viewModelScope.launch {
            tts.isSpeaking.collect { speaking ->
                _uiState.value = _uiState.value.copy(isSpeaking = speaking)
            }
        }
    }

    fun toggleOpenAi(enabled: Boolean) {
        if (enabled && localLLM.getOpenAiKey().isBlank()) {
            _uiState.value = _uiState.value.copy(showApiKeyDialog = true)
        } else {
            localLLM.setOpenAiConfig(localLLM.getOpenAiKey(), localLLM.getOpenAiModel(), enabled)
            _uiState.value = _uiState.value.copy(
                isOpenAiEnabled = enabled,
                engineName = localLLM.getActiveEngineName()
            )
        }
    }

    fun openApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = true)
    }

    fun dismissApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = false)
    }

    fun saveOpenAiConfig(apiKey: String, model: String, enabled: Boolean) {
        localLLM.setOpenAiConfig(apiKey, model, enabled)
        _uiState.value = _uiState.value.copy(
            isOpenAiEnabled = enabled && apiKey.isNotBlank(),
            openAiKey = apiKey,
            openAiModel = model,
            showApiKeyDialog = false,
            engineName = localLLM.getActiveEngineName()
        )
    }

    fun refreshServicesStatus() {
        _uiState.value = _uiState.value.copy(
            isAccessibilityActive = JarvisAccessibilityService.isRunning(),
            isNotificationListenerActive = JarvisNotificationListenerService.isRunning(),
            engineName = localLLM.getActiveEngineName()
        )
    }

    fun submitQuery(userText: String, speakResult: Boolean = false) {
        if (userText.isBlank()) return

        val trimmed = userText.trim()
        val tempUserMsg = ChatMessage(sender = "user", text = trimmed)
        val currentList = _uiState.value.messages.toMutableList().apply { add(tempUserMsg) }
        _uiState.value = _uiState.value.copy(
            messages = currentList,
            dynamicSuggestions = emptyList()
        )

        viewModelScope.launch {
            val response = agent.execute(trimmed)
            if (_uiState.value.autoSpeak && response.isNotBlank()) {
                tts.speak(response)
            }
        }
    }

    fun toggleVoiceInput() {
        if (_uiState.value.isListening) {
            speechRecognizer.stopListening()
        } else {
            tts.stop()
            // No lambda here — voiceState collector (VoiceState.Recognized) handles submission
            speechRecognizer.startListening()
        }
    }

    fun stopSpeaking() {
        tts.stop()
    }

    fun setAutoSpeak(enabled: Boolean) {
        tts.setAutoSpeak(enabled)
        _uiState.value = _uiState.value.copy(autoSpeak = enabled)
    }

    fun setSpeechRate(rate: Float) {
        tts.setSpeechRate(rate)
        _uiState.value = _uiState.value.copy(speechRate = rate)
    }

    fun setTtsPitch(pitch: Float) {
        tts.setPitch(pitch)
        _uiState.value = _uiState.value.copy(ttsPitch = pitch)
    }

    fun testVoice() {
        tts.testVoice()
    }

    fun getAllTools() = app.toolRegistry.getAllTools()

    fun getLocalModels() = localLLM.modelManager.getLocalModels()

    fun downloadAndLoadModel(modelInfo: ModelInfo) {
        viewModelScope.launch {
            localLLM.modelManager.downloadModel(modelInfo).collect { progress ->
                val current = _uiState.value.downloadProgress.toMutableMap()
                current[modelInfo.id] = progress
                _uiState.value = _uiState.value.copy(downloadProgress = current)

                if (progress == 100) {
                    val file = localLLM.modelManager.getModelFile(modelInfo)
                    val result = localLLM.loadGGUFModel(file)
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(engineName = localLLM.getActiveEngineName())
                    }
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            memoryRepo.clearHistory()
            _uiState.value = _uiState.value.copy(messages = emptyList())
        }
    }
}
