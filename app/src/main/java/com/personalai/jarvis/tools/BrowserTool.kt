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

class OpenBrowserTool(private val context: Context) : AgentTool {
    override val name: String = "open_browser"
    override val description: String = "Opens a web page or performs a web search in the Android web browser."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "url",
                type = "string",
                description = "Direct website URL (e.g. 'https://github.com').",
                required = false
            ),
            ToolParameter(
                name = "query",
                type = "string",
                description = "Search query to look up on the web (e.g. 'Python DSA tutorials').",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val urlParam = arguments["url"] as? String
        val queryParam = arguments["query"] as? String

        val targetUri: Uri = when {
            !urlParam.isNullOrBlank() -> {
                val fullUrl = if (!urlParam.startsWith("http://") && !urlParam.startsWith("https://")) {
                    "https://$urlParam"
                } else {
                    urlParam
                }
                Uri.parse(fullUrl)
            }
            !queryParam.isNullOrBlank() -> {
                val encoded = URLEncoder.encode(queryParam, "UTF-8")
                Uri.parse("https://www.google.com/search?q=$encoded")
            }
            else -> {
                return@withContext ToolResult.error("Either 'url' or 'query' must be provided.")
            }
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val displayTarget = queryParam?.let { "search for '$it'" } ?: targetUri.toString()
            ToolResult.success(
                output = "Opened browser to $displayTarget.",
                data = mapOf("uri" to targetUri.toString())
            )
        } catch (e: Exception) {
            ToolResult.error("Failed to open browser: ${e.localizedMessage ?: e.message}")
        }
    }
}
