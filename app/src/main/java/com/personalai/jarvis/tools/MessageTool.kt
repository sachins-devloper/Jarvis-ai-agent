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
        val rawRecipient = (arguments["recipient"] as? String)?.trim() ?: ""
        val message = (arguments["message"] as? String)?.trim() ?: ""
        val rawApp = (arguments["app"] as? String)?.lowercase() ?: "sms"

        if (message.isBlank()) {
            return@withContext ToolResult.error("Message content cannot be blank.")
        }

        // 1. Sanitize app and recipient query
        val isWhatsApp = rawApp == "whatsapp" || rawRecipient.contains("whatsapp", ignoreCase = true)
        val app = if (isWhatsApp) "whatsapp" else "sms"
        val recipientQuery = rawRecipient
            .replace(Regex("""\s*(?:in|on|via|through|using)\s*(?:whatsapp|whats\s*app|sms|text)\s*""", RegexOption.IGNORE_CASE), "")
            .trim()

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
                val appLabel = if (app == "whatsapp") "WhatsApp" else "message"
                val promptMsg = "Found ${candidates.size} contacts for '$recipientQuery':\n$formatted\n\nWhich one would you like to send this $appLabel to?"
                val suggestions = candidates.take(4).map { "Send $appLabel to ${it.name}: $message" }

                return@withContext ToolResult.success(
                    output = promptMsg,
                    data = mapOf(
                        "requires_disambiguation" to true,
                        "task_type" to "message",
                        "app" to app,
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
                var digitsOnly = finalNumber.replace(Regex("[^0-9]"), "")
                if (digitsOnly.length == 10) {
                    // Standard 10-digit Indian number without country code
                    digitsOnly = "91$digitsOnly"
                } else if (digitsOnly.length == 11 && digitsOnly.startsWith("0")) {
                    // 0-prefixed 10-digit number -> replace leading 0 with 91
                    digitsOnly = "91" + digitsOnly.substring(1)
                }

                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val uri = if (digitsOnly.isNotEmpty()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly&text=$encodedMsg")
                } else {
                    Uri.parse("whatsapp://send?text=$encodedMsg")
                }

                val packagesToTry = listOf("com.whatsapp", "com.whatsapp.w4b")
                var started = false
                for (pkg in packagesToTry) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage(pkg)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        started = true
                        break
                    } catch (_: Exception) {}
                }

                if (!started) {
                    val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(genericIntent)
                }

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
