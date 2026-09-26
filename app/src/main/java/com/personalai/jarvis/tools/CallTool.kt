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

class CallTool(private val context: Context) : AgentTool {
    override val name: String = "make_call"
    override val description: String = "Initiates or prepares a phone call to a contact or phone number."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "contact",
                type = "string",
                description = "Name of the contact (e.g. 'Mom', 'John') or phone number to call.",
                required = true
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val contact = (arguments["contact"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: contact")

        try {
            val isNumber = contact.matches(Regex("""^[+0-9\s-]+$"""))
            val uri = if (isNumber) {
                Uri.parse("tel:${contact.replace(" ", "")}")
            } else {
                Uri.parse("tel:")
            }

            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            ToolResult.success(
                output = "Dialer opened for $contact.",
                data = mapOf("contact" to contact)
            )
        } catch (e: Exception) {
            ToolResult.error("Failed to initiate call: ${e.localizedMessage ?: e.message}")
        }
    }
}
