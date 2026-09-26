package com.personalai.jarvis.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OpenAIEngine(
    var apiKey: String = "",
    var model: String = "gpt-4o-mini"
) : LocalLLMEngine {

    private val gson = Gson()

    companion object {
        private const val TAG = "OpenAIEngine"
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
    }

    override val name: String
        get() = "OpenAI ($model)"

    override val isLoaded: Boolean
        get() = apiKey.isNotBlank()

    override val loadedModelFile: File? = null

    override suspend fun loadModel(modelFile: File): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun unloadModel() {
        // No-op for remote OpenAI API
    }

    override fun generate(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        stopTokens: List<String>
    ): Flow<String> = flow {
        if (apiKey.isBlank()) {
            emit("Error: OpenAI API key is missing. Please set your API key in settings.")
            return@flow
        }

        try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
            }

            val requestBody = JsonObject().apply {
                addProperty("model", model)
                addProperty("temperature", temperature)
                addProperty("max_tokens", maxTokens)
                addProperty("stream", true)
                val messagesArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", prompt)
                    })
                }
                add("messages", messagesArray)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errorBody = errorStream.bufferedReader().use { it.readText() }
                emit(parseErrorMessage(responseCode, errorBody))
                return@flow
            }

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data: ")) {
                        val data = trimmed.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val choices = json.getAsJsonArray("choices")
                            if (choices != null && choices.size() > 0) {
                                val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                if (delta != null && delta.has("content")) {
                                    val token = delta.get("content").asString
                                    if (token.isNotEmpty()) {
                                        emit(token)
                                    }
                                }
                            }
                        } catch (ignored: Exception) {
                            // ignore malformed SSE delta
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI streaming error", e)
            emit("Error connecting to OpenAI: ${e.localizedMessage ?: e.message}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateComplete(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Error: OpenAI API key is missing. Please tap the toggle to set your API key."
        }

        try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
            }

            val requestBody = JsonObject().apply {
                addProperty("model", model)
                addProperty("temperature", temperature)
                addProperty("max_tokens", maxTokens)
                val messagesArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", prompt)
                    })
                }
                add("messages", messagesArray)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errorBody = errorStream.bufferedReader().use { it.readText() }
                return@withContext parseErrorMessage(responseCode, errorBody)
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val message = choices[0].asJsonObject.getAsJsonObject("message")
                return@withContext message.get("content").asString.trim()
            }

            "No response received from OpenAI."
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI completion error", e)
            "Error connecting to OpenAI: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun parseErrorMessage(code: Int, errorBody: String): String {
        return try {
            val json = gson.fromJson(errorBody, JsonObject::class.java)
            val errorObj = json.getAsJsonObject("error")
            val message = errorObj?.get("message")?.asString ?: "HTTP $code"
            when (code) {
                401 -> "OpenAI Authentication Failed: Invalid API Key. Please verify your OpenAI key."
                429 -> "OpenAI Quota/Rate Limit Exceeded: $message"
                else -> "OpenAI Error ($code): $message"
            }
        } catch (e: Exception) {
            "OpenAI Error ($code): $errorBody"
        }
    }

    /**
     * Performs multimodal image understanding using GPT-4o / GPT-4o-mini vision capabilities.
     */
    suspend fun analyzeImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        temperature: Float = 0.4f,
        maxTokens: Int = 1000
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "OpenAI API key is missing. Please set your API key in Settings to use vision analysis."
        }

        try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
            }

            // Ensure a vision-capable model is used (gpt-4o or gpt-4o-mini)
            val visionModel = if (model.contains("gpt-4")) model else "gpt-4o-mini"

            val requestBody = JsonObject().apply {
                addProperty("model", visionModel)
                addProperty("temperature", temperature)
                addProperty("max_tokens", maxTokens)

                val contentArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", prompt.ifBlank { "Describe and explain what you see in this image in detail. Extract any visible text, key objects, and actionable information." })
                    })
                    add(JsonObject().apply {
                        addProperty("type", "image_url")
                        val imgUrl = JsonObject().apply {
                            addProperty("url", "data:$mimeType;base64,$imageBase64")
                            addProperty("detail", "auto")
                        }
                        add("image_url", imgUrl)
                    })
                }

                val messagesArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        add("content", contentArray)
                    })
                }
                add("messages", messagesArray)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errorBody = errorStream.bufferedReader().use { it.readText() }
                return@withContext parseErrorMessage(responseCode, errorBody)
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val message = choices[0].asJsonObject.getAsJsonObject("message")
                return@withContext message.get("content").asString.trim()
            }

            "No vision analysis response received from OpenAI."
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI vision error", e)
            "Error analyzing image: ${e.localizedMessage ?: e.message}"
        }
    }
}
