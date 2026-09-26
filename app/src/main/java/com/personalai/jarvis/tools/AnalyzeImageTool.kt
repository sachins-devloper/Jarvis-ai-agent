package com.personalai.jarvis.tools

import android.content.Context
import android.provider.MediaStore
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import com.personalai.jarvis.ai.LocalLLM
import com.personalai.jarvis.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Agent tool to analyze photos, documents, and screenshots using Multimodal Vision AI.
 */
class AnalyzeImageTool(
    private val context: Context,
    private val localLLM: LocalLLM
) : AgentTool {

    override val name: String = "analyze_image"
    override val description: String = "Analyzes photos, images, or documents using Vision AI to describe contents, read text (OCR), explain diagrams, or identify objects."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "image_path",
                type = "string",
                description = "File path or URI of the image to analyze. Optional: if omitted, inspects the latest photo in device storage.",
                required = false
            ),
            ToolParameter(
                name = "prompt",
                type = "string",
                description = "Question or instructions regarding the image (e.g. 'What is this?', 'Read the text', 'Summarize this receipt').",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val userPrompt = (arguments["prompt"] as? String)?.trim() ?: "Describe and analyze this image in detail."
        var rawPath = (arguments["image_path"] as? String)?.trim() ?: ""

        // If no path was provided, look for the most recently taken photo in MediaStore
        if (rawPath.isBlank()) {
            rawPath = findLatestPhotoPath() ?: ""
        }

        if (rawPath.isBlank()) {
            return@withContext ToolResult.error("No image found to analyze. Please provide a photo or take one using the camera.")
        }

        val base64 = ImageHelper.fileToBase64(rawPath)
            ?: return@withContext ToolResult.error("Unable to load image at: $rawPath")

        val resultText = localLLM.analyzeImage(
            prompt = userPrompt,
            imageBase64 = base64,
            mimeType = "image/jpeg"
        )

        ToolResult.success(resultText)
    }

    private fun findLatestPhotoPath(): String? {
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC LIMIT 1"

        return context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                if (dataIdx != -1) cursor.getString(dataIdx) else null
            } else null
        }
    }
}
