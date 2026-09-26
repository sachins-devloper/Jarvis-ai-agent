package com.personalai.jarvis.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallTool(private val context: Context) : AgentTool {
    override val name: String = "make_call"
    override val description: String = "Initiates a direct phone call to a contact name or phone number."

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

    data class ContactMatch(val name: String, val number: String)

    private fun findMatchingContacts(contactQuery: String): List<ContactMatch> {
        val cleanNumber = contactQuery.replace(" ", "")
        if (cleanNumber.matches(Regex("""^[+0-9-]+$"""))) {
            return listOf(ContactMatch(name = contactQuery, number = cleanNumber))
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
                arrayOf("%$contactQuery%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (it.moveToNext()) {
                    if (numIdx != -1 && nameIdx != -1) {
                        val number = it.getString(numIdx)?.trim() ?: continue
                        val name = it.getString(nameIdx)?.trim() ?: contactQuery
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
        val contactQuery = (arguments["contact"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: contact")

        try {
            val candidates = findMatchingContacts(contactQuery)

            if (candidates.isEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return@withContext ToolResult.error("Could not find contact '$contactQuery' in phone contacts.")
            }

            // Check if there is an exact single match (e.g. user clicked exact suggestion or typed exact full name)
            val exactMatches = candidates.filter { it.name.equals(contactQuery, ignoreCase = true) }
            val targetContact = when {
                exactMatches.size == 1 -> exactMatches.first()
                candidates.size == 1 -> candidates.first()
                else -> null
            }

            if (targetContact == null) {
                // Ambiguous: multiple matching contacts found (e.g. "Arun Frnd", "Arun Cre8ive")
                val formatted = candidates.take(5).mapIndexed { i, c -> "${i + 1}. ${c.name} (${c.number})" }.joinToString("\n")
                val promptMsg = "Found ${candidates.size} contacts for '$contactQuery':\n$formatted\n\nWhich one would you like to call?"
                val suggestions = candidates.take(4).map { "Call ${it.name}" }

                return@withContext ToolResult.success(
                    output = promptMsg,
                    data = mapOf(
                        "requires_disambiguation" to true,
                        "task_type" to "call",
                        "query" to contactQuery,
                        "suggestions" to suggestions,
                        "candidates" to candidates.map { it.name }
                    )
                )
            }

            val resolvedNumber = targetContact.number
            val displayName = targetContact.name

            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                // Directly place the phone call
                val uri = Uri.parse("tel:${resolvedNumber.replace(" ", "")}")
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                ToolResult.success(
                    output = "Calling $displayName ($resolvedNumber)...",
                    data = mapOf("contact" to displayName, "number" to resolvedNumber, "direct" to true)
                )
            } else {
                // Open dialer with number ready
                val uri = Uri.parse("tel:${resolvedNumber.replace(" ", "")}")
                val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                ToolResult.success(
                    output = "Dialer opened for $displayName ($resolvedNumber). Grant Phone Call permission in Android settings for direct calling.",
                    data = mapOf("contact" to displayName, "number" to resolvedNumber, "direct" to false)
                )
            }
        } catch (e: Exception) {
            ToolResult.error("Failed to place call: ${e.localizedMessage ?: e.message}")
        }
    }
}
