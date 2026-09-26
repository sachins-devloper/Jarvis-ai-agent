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

import android.provider.ContactsContract

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

    data class ContactMatch(val name: String, val number: String)

    private fun findMatchingContacts(query: String): List<ContactMatch> {
        val cleanNumber = query.replace(" ", "")
        if (cleanNumber.matches(Regex("""^[+0-9-]+$"""))) {
            return listOf(ContactMatch(name = query, number = cleanNumber))
        }

        val contentResolver = context.contentResolver
        val list = mutableListOf<ContactMatch>()
        val seen = mutableSetOf<String>()

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (it.moveToNext()) {
                    if (numIdx != -1 && nameIdx != -1) {
                        val number = it.getString(numIdx)?.trim() ?: continue
                        val name = it.getString(nameIdx)?.trim() ?: query
                        val key = "${name.lowercase()}|${number.replace(" ", "")}"
                        if (seen.add(key)) {
                            list.add(ContactMatch(name, number))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Contacts provider error
        }
        return list
    }

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val recipientQuery = (arguments["recipient"] as? String)?.trim() ?: ""
        val message = (arguments["message"] as? String)?.trim() ?: ""
        val app = (arguments["app"] as? String)?.lowercase() ?: "sms"

        if (message.isBlank()) {
            return@withContext ToolResult.error("Message content cannot be blank.")
        }

        try {
            val candidates = if (recipientQuery.isNotBlank()) findMatchingContacts(recipientQuery) else emptyList()

            val exactMatches = candidates.filter { it.name.equals(recipientQuery, ignoreCase = true) }
            val targetContact = when {
                exactMatches.size == 1 -> exactMatches.first()
                candidates.size == 1 -> candidates.first()
                else -> null
            }

            if (candidates.size > 1 && targetContact == null) {
                // Ambiguous: multiple matching contacts found
                val formatted = candidates.take(5).mapIndexed { i, c -> "${i + 1}. ${c.name} (${c.number})" }.joinToString("\n")
                val promptMsg = "Found ${candidates.size} contacts for '$recipientQuery':\n$formatted\n\nWhich one would you like to message?"
                val suggestions = candidates.take(4).map { "Send message to ${it.name}: $message" }

                return@withContext ToolResult.success(
                    output = promptMsg,
                    data = mapOf(
                        "requires_disambiguation" to true,
                        "task_type" to "message",
                        "query" to recipientQuery,
                        "message_body" to message,
                        "suggestions" to suggestions,
                        "candidates" to candidates.map { it.name }
                    )
                )
            }

            val finalNumber = targetContact?.number ?: recipientQuery.replace(Regex("[^0-9+]"), "")
            val displayName = targetContact?.name ?: recipientQuery

            if (app == "whatsapp") {
                val cleanPhone = finalNumber.replace(Regex("[^0-9+]"), "")
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
                ToolResult.success("WhatsApp opened with draft to $displayName ($finalNumber).")
            } else {
                // SMS
                val smsUri = if (finalNumber.isNotBlank()) Uri.parse("smsto:$finalNumber") else Uri.parse("smsto:")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult.success("Messaging app opened with draft to $displayName ($finalNumber).")
            }
        } catch (e: Exception) {
            ToolResult.error("Could not send message: ${e.localizedMessage ?: e.message}")
        }
    }
}
