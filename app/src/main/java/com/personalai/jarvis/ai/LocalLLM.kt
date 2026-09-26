package com.personalai.jarvis.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.io.File

class LocalLLM(private val context: Context) {
    val modelManager = ModelManager(context)
    private val ggufEngine = GGUFEngine()
    private val fallbackEngine = RuleBasedFallbackEngine()

    private var activeEngine: LocalLLMEngine = fallbackEngine

    companion object {
        private const val TAG = "LocalLLM"
    }

    init {
        // Check if any local GGUF models already exist on disk
        val localModels = modelManager.getLocalModels()
        if (localModels.isNotEmpty()) {
            val first = localModels.first()
            Log.i(TAG, "Found local GGUF model: ${first.name}. Ready to load.")
        }
    }

    fun getActiveEngineName(): String = activeEngine.name
    fun isUsingGGUF(): Boolean = activeEngine is GGUFEngine && activeEngine.isLoaded

    suspend fun loadGGUFModel(file: File): Result<Unit> {
        val result = ggufEngine.loadModel(file)
        if (result.isSuccess) {
            activeEngine = ggufEngine
            Log.i(TAG, "Switched active engine to GGUFEngine (${file.name})")
        }
        return result
    }

    suspend fun useFallbackEngine() {
        if (activeEngine is GGUFEngine) {
            ggufEngine.unloadModel()
        }
        activeEngine = fallbackEngine
        Log.i(TAG, "Switched active engine to fallback engine.")
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
}
