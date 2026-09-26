package com.personalai.jarvis.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class YouTubeTool(private val context: Context) : AgentTool {
    override val name: String = "play_youtube"
    override val description: String = "Searches and plays music, songs, or videos on YouTube."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "query",
                type = "string",
                description = "Name of the song, artist, video, or topic to play on YouTube.",
                required = true
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val query = (arguments["query"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: query")

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val uri = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // If YouTube app not installed, open via web browser
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
            ToolResult.success("Playing '$query' on YouTube.")
        } catch (e: Exception) {
            ToolResult.error("Could not play on YouTube: ${e.localizedMessage ?: e.message}")
        }
    }
}
