package com.personalai.jarvis.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.io.File

class LocalLLM(private val context: Context) {
    val modelManager = ModelManager(context)
    val ggufEngine = GGUFEngine()
    val fallbackEngine = RuleBasedFallbackEngine()
    val openAiEngine = OpenAIEngine()

    private var activeEngine: LocalLLMEngine = fallbackEngine
    private var openAiEnabled: Boolean = false

    companion object {
        private const val TAG = "LocalLLM"
        private const val PREFS_NAME = "jarvis_ai_prefs"
        private const val KEY_OPENAI_ENABLED = "openai_enabled"
        private const val KEY_OPENAI_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
    }

    init {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_OPENAI_KEY, "") ?: ""
        val savedModel = prefs.getString(KEY_OPENAI_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
        val savedEnabled = prefs.getBoolean(KEY_OPENAI_ENABLED, false)

        openAiEngine.apiKey = savedKey
        openAiEngine.model = savedModel
        openAiEnabled = savedEnabled && savedKey.isNotBlank()

        if (openAiEnabled) {
            activeEngine = openAiEngine
            Log.i(TAG, "OpenAI initialized as active engine ($savedModel)")
        } else {
            // Check if any local GGUF models already exist on disk
            val localModels = modelManager.getLocalModels()
            if (localModels.isNotEmpty()) {
                val first = localModels.first()
                Log.i(TAG, "Found local GGUF model: ${first.name}. Ready to load.")
            }
        }
    }

    fun getActiveEngineName(): String = activeEngine.name
    fun isUsingGGUF(): Boolean = activeEngine is GGUFEngine && activeEngine.isLoaded
    fun isOpenAiEnabled(): Boolean = openAiEnabled
    fun getOpenAiKey(): String = openAiEngine.apiKey
    fun getOpenAiModel(): String = openAiEngine.model

    fun setOpenAiConfig(apiKey: String, model: String = "gpt-4o-mini", enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_OPENAI_KEY, apiKey)
            .putString(KEY_OPENAI_MODEL, model)
            .putBoolean(KEY_OPENAI_ENABLED, enabled)
            .apply()

        openAiEngine.apiKey = apiKey
        openAiEngine.model = model
        openAiEnabled = enabled

        if (enabled && apiKey.isNotBlank()) {
            activeEngine = openAiEngine
            Log.i(TAG, "Switched active engine to OpenAI ($model)")
        } else {
            if (ggufEngine.isLoaded) {
                activeEngine = ggufEngine
                Log.i(TAG, "Switched active engine back to GGUFEngine")
            } else {
                activeEngine = fallbackEngine
                Log.i(TAG, "Switched active engine back to fallback engine")
            }
        }
    }

    suspend fun loadGGUFModel(file: File): Result<Unit> {
        val result = ggufEngine.loadModel(file)
        if (result.isSuccess && !openAiEnabled) {
            activeEngine = ggufEngine
            Log.i(TAG, "Switched active engine to GGUFEngine (${file.name})")
        }
        return result
    }

    suspend fun useFallbackEngine() {
        if (activeEngine is GGUFEngine) {
            ggufEngine.unloadModel()
        }
        if (!openAiEnabled) {
            activeEngine = fallbackEngine
            Log.i(TAG, "Switched active engine to fallback engine.")
        }
    }

    fun generate(
        prompt: String,
        temperature: Float = 0.3f,
        maxTokens: Int = 512,
        stopTokens: List<String> = listOf("</s>", "<|im_end|>", "\nUser:", "Observation:")
    ): Flow<String> {
        return activeEngine.generate(prompt, temperature, maxTokens, stopTokens)
    }

    suspend fun generateComplete(
        prompt: String,
        temperature: Float = 0.3f,
        maxTokens: Int = 512
    ): String {
        return activeEngine.generateComplete(prompt, temperature, maxTokens)
    }

    suspend fun analyzeImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg"
    ): String {
        return if (openAiEnabled && openAiEngine.isLoaded) {
            openAiEngine.analyzeImage(prompt, imageBase64, mimeType)
        } else {
            "Vision analysis requires OpenAI Cloud Intelligence (GPT-4o or GPT-4o-mini). Please enable OpenAI with your API key in Settings to inspect photos and camera shots."
        }
    }
}
