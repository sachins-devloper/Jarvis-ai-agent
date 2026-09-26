package com.personalai.jarvis.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import com.personalai.jarvis.services.JarvisAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UiInteractionTool(private val context: Context) : AgentTool {
    override val name: String = "ui_action"
    override val description: String = "Interacts with on-screen UI elements (clicks buttons, enters text, reads screen) via Android Accessibility Service."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "'click', 'type', or 'read_screen'.",
                required = true
            ),
            ToolParameter(
                name = "target_text",
                type = "string",
                description = "Visible text or label of the button/field to click.",
                required = false
            ),
            ToolParameter(
                name = "input_text",
                type = "string",
                description = "Text to enter into focused field (for 'type' action).",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return@withContext ToolResult.error("Accessibility Service is not enabled. Please enable 'Jarvis AI' in Android Accessibility Settings.")
        }

        val action = (arguments["action"] as? String)?.lowercase()?.trim() ?: "click"

        when (action) {
            "click" -> {
                val targetText = (arguments["target_text"] as? String)
                    ?: return@withContext ToolResult.error("Missing 'target_text' for click action.")
                val clicked = service.clickByText(targetText)
                if (clicked) {
                    ToolResult.success("Clicked UI element with text: '$targetText'")
                } else {
                    ToolResult.error("Could not find or click clickable element with text: '$targetText'")
                }
            }

            "type" -> {
                val inputText = (arguments["input_text"] as? String)
                    ?: return@withContext ToolResult.error("Missing 'input_text' for type action.")
                val typed = service.inputText(inputText)
                if (typed) {
                    ToolResult.success("Entered text '$inputText' into focused field.")
                } else {
                    ToolResult.error("Failed to type text. Make sure an editable field is focused.")
                }
            }

            "read_screen" -> {
                val texts = service.dumpScreenText()
                if (texts.isEmpty()) {
                    ToolResult.success("No text elements found on current screen.")
                } else {
                    ToolResult.success("Visible on screen:\n" + texts.take(15).joinToString("\n") { "• $it" })
                }
            }

            else -> ToolResult.error("Unknown action '$action'. Use 'click', 'type', or 'read_screen'.")
        }
    }
}
