package com.personalai.jarvis.ai

import kotlinx.coroutines.flow.Flow
import java.io.File

interface LocalLLMEngine {
    val name: String
    val isLoaded: Boolean
    val loadedModelFile: File?

    suspend fun loadModel(modelFile: File): Result<Unit>
    suspend fun unloadModel()

    /**
     * Generates text response given a prompt.
     * Can stream tokens via Flow.
     */
    fun generate(
        prompt: String,
        temperature: Float = 0.3f,
        maxTokens: Int = 512,
        stopTokens: List<String> = listOf("</s>", "<|im_end|>", "\nUser:", "Observation:")
    ): Flow<String>

    suspend fun generateComplete(
        prompt: String,
        temperature: Float = 0.3f,
        maxTokens: Int = 512
    ): String
}
