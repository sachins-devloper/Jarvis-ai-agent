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

class SendMessageTool(private val context: Context) : AgentTool {
    override val name: String = "send_message"
    override val description: String = "Composes or sends a message to a recipient via SMS or WhatsApp."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "recipient",
                type = "string",
                description = "Phone number or recipient contact name.",
                required = true
            ),
            ToolParameter(
                name = "message",
                type = "string",
                description = "Text content of the message.",
                required = true
            ),
            ToolParameter(
                name = "app",
                type = "string",
                description = "'sms' or 'whatsapp' (defaults to 'sms').",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val recipient = (arguments["recipient"] as? String)?.trim() ?: ""
        val message = (arguments["message"] as? String)?.trim() ?: ""
        val app = (arguments["app"] as? String)?.lowercase() ?: "sms"

        if (message.isBlank()) {
            return@withContext ToolResult.error("Message content cannot be blank.")
        }

        try {
            if (app == "whatsapp") {
                val cleanPhone = recipient.replace(Regex("[^0-9+]"), "")
                val uri = if (cleanPhone.isNotEmpty()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}")
                } else {
                    Uri.parse("whatsapp://send?text=${URLEncoder.encode(message, "UTF-8")}")
                }
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult.success("WhatsApp opened with draft to '$recipient'.")
            } else {
                // SMS
                val smsUri = if (recipient.isNotBlank()) Uri.parse("smsto:$recipient") else Uri.parse("smsto:")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult.success("Messaging app opened with draft to '$recipient'.")
            }
        } catch (e: Exception) {
            ToolResult.error("Could not send message: ${e.localizedMessage ?: e.message}")
        }
    }
}
