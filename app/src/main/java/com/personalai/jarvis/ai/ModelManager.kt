package com.personalai.jarvis.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ModelInfo(
    val id: String,
    val name: String,
    val filename: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val description: String,
    val recommendedRamMb: Int
)

class ModelManager(private val context: Context) {
    private val modelsDir: File = File(context.filesDir, "models").apply { mkdirs() }

    companion object {
        private const val TAG = "ModelManager"

        val PRESET_MODELS = listOf(
            ModelInfo(
                id = "smollm2_360m",
                name = "SmolLM2 360M Instruct (Q8_0)",
                filename = "smollm2-360m-instruct-q8_0.gguf",
                sizeBytes = 380_000_000L,
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
                description = "Ultra-lightweight mobile LLM. Extremely fast inference, <500MB RAM.",
                recommendedRamMb = 1024
            ),
            ModelInfo(
                id = "qwen2.5_0.5b",
                name = "Qwen2.5 0.5B Instruct (Q4_K_M)",
                filename = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                sizeBytes = 390_000_000L,
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                description = "Great reasoning and structured tool calling in a compact size.",
                recommendedRamMb = 1536
            ),
            ModelInfo(
                id = "llama_3.2_1b",
                name = "Llama 3.2 1B Instruct (Q4_K_M)",
                filename = "llama-3.2-1b-instruct-q4_k_m.gguf",
                sizeBytes = 800_000_000L,
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                description = "High capability agent model with deep context understanding.",
                recommendedRamMb = 2048
            )
        )
    }

    fun getModelsDirectory(): File = modelsDir

    fun getLocalModels(): List<File> {
        val searchDirs = listOfNotNull(
            modelsDir,
            context.getExternalFilesDir("models"),
            File(android.os.Environment.getExternalStorageDirectory(), "Download"),
            File(android.os.Environment.getExternalStorageDirectory(), "Download/models")
        )

        val results = mutableListOf<File>()
        searchDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file -> file.isFile && file.extension.lowercase() == "gguf" }?.let {
                    results.addAll(it)
                }
            }
        }
        return results.distinctBy { it.name }.sortedByDescending { it.lastModified() }
    }

    fun isModelDownloaded(modelInfo: ModelInfo): Boolean {
        return getLocalModels().any { it.name.equals(modelInfo.filename, ignoreCase = true) }
    }

    fun getModelFile(modelInfo: ModelInfo): File {
        return getLocalModels().firstOrNull { it.name.equals(modelInfo.filename, ignoreCase = true) }
            ?: File(modelsDir, modelInfo.filename)
    }

    /**
     * Downloads model file with download progress emitted as percentage (0..100)
     */
    fun downloadModel(modelInfo: ModelInfo): Flow<Int> = flow {
        val targetFile = File(modelsDir, modelInfo.filename)
        val tempFile = File(modelsDir, "${modelInfo.filename}.tmp")

        Log.i(TAG, "Starting download of ${modelInfo.name} from ${modelInfo.downloadUrl}")
        emit(0)

        val url = URL(modelInfo.downloadUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true

        connection.connect()
        val totalBytes = connection.contentLengthLong.let { if (it > 0) it else modelInfo.sizeBytes }

        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var downloadedBytes: Long = 0
                var lastProgress = 0

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                    if (progress != lastProgress) {
                        lastProgress = progress
                        emit(progress)
                    }
                }
            }
        }

        if (tempFile.renameTo(targetFile)) {
            Log.i(TAG, "Model download complete: ${targetFile.absolutePath}")
            emit(100)
        } else {
            throw IllegalStateException("Failed to finalize downloaded model file.")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteModel(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete() else false
    }
}
