package com.personalai.jarvis.tools

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ContactTool — Resolves a contact name or relationship alias ("Mom", "Dad")
 * to one or more phone numbers from the device contact book.
 *
 * Used as a prerequisite step before CallTool when only a name is known.
 *
 * Tool call JSON:
 *   { "tool": "lookup_contact", "arguments": { "name": "Mom" } }
 *
 * Returns:
 *   - Single match  → { "name": "...", "number": "...", "resolved": true }
 *   - Multiple      → { "requires_disambiguation": true, "candidates": [...] }
 *   - Not found     → error
 */
class ContactTool(private val context: Context) : AgentTool {

    override val name = "lookup_contact"
    override val description = "Looks up a contact by name or relationship (Mom, Dad, brother) in the device phone book and returns their phone number."

    override val definition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "name",
                type = "string",
                description = "Contact name or relationship alias (e.g. 'Mom', 'John', 'Dad').",
                required = true
            )
        )
    )

    data class ContactEntry(val displayName: String, val number: String)

    // ─── Family alias map ──────────────────────────────────────────────────────
    private val familyAliases: Map<String, List<String>> = mapOf(
        "mom"     to listOf("mom", "mother", "mummy", "mommy", "maa", "ammi", "amma"),
        "mother"  to listOf("mom", "mother", "mummy", "mommy", "maa", "ammi", "amma"),
        "mummy"   to listOf("mom", "mother", "mummy", "mommy", "maa", "ammi", "amma"),
        "maa"     to listOf("mom", "mother", "mummy", "mommy", "maa", "ammi", "amma"),
        "ammi"    to listOf("mom", "mother", "mummy", "mommy", "maa", "ammi", "amma"),
        "amma"    to listOf("mom", "mother", "mummy", "mommy", "maa", "ammi", "amma"),
        "dad"     to listOf("dad", "father", "daddy", "papa", "appa", "abbu", "abba"),
        "father"  to listOf("dad", "father", "daddy", "papa", "appa", "abbu", "abba"),
        "papa"    to listOf("dad", "father", "daddy", "papa", "appa", "abbu", "abba"),
        "abbu"    to listOf("dad", "father", "daddy", "papa", "appa", "abbu", "abba"),
        "bro"     to listOf("bro", "brother"),
        "brother" to listOf("bro", "brother"),
        "sis"     to listOf("sis", "sister"),
        "sister"  to listOf("sis", "sister"),
        "wife"    to listOf("wife", "wifey", "jaan", "honey"),
        "husband" to listOf("husband", "hubby")
    )

    // ─── Contact query ─────────────────────────────────────────────────────────
    private fun queryByTerm(term: String): List<ContactEntry> {
        val results = mutableListOf<ContactEntry>()
        val seen = mutableSetOf<String>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$term%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx  = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val name   = c.getString(nameIdx)?.trim() ?: continue
                    val number = c.getString(numIdx)?.trim()  ?: continue
                    val key = "${name.lowercase()}|${number.replace(" ", "")}"
                    if (seen.add(key)) results.add(ContactEntry(name, number))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Query failed for term='$term'", e)
        }
        return results
    }

    fun resolve(name: String): List<ContactEntry> {
        val lower = name.lowercase().trim()

        // Direct number — no lookup needed
        if (lower.replace(" ", "").matches(Regex("""^[+0-9\-]+$"""))) {
            return listOf(ContactEntry(displayName = name, number = name))
        }

        // Direct name query first
        val direct = queryByTerm(name)
        if (direct.isNotEmpty()) return direct

        // Alias expansion
        val aliases = familyAliases[lower] ?: emptyList()
        for (alias in aliases) {
            if (alias != lower) {
                val hit = queryByTerm(alias)
                if (hit.isNotEmpty()) return hit
            }
        }

        return emptyList()
    }

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val name = (arguments["name"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing parameter: name")

        val candidates = resolve(name)

        when {
            candidates.isEmpty() -> ToolResult.error(
                "No contact found for '$name'. Check spelling or confirm contact is saved on this device."
            )

            candidates.size == 1 -> ToolResult.success(
                output = "Found: ${candidates[0].displayName} (${candidates[0].number})",
                data = mapOf(
                    "resolved" to true,
                    "name"   to candidates[0].displayName,
                    "number" to candidates[0].number
                )
            )

            else -> {
                // Check for single exact match
                val exact = candidates.filter { it.displayName.equals(name, ignoreCase = true) }
                if (exact.size == 1) {
                    ToolResult.success(
                        output = "Found: ${exact[0].displayName} (${exact[0].number})",
                        data = mapOf(
                            "resolved" to true,
                            "name"   to exact[0].displayName,
                            "number" to exact[0].number
                        )
                    )
                } else {
                    val list = candidates.take(5)
                        .mapIndexed { i, c -> "${i + 1}. ${c.displayName} (${c.number})" }
                        .joinToString("\n")
                    ToolResult.success(
                        output = "Multiple contacts found for '$name':\n$list\nWhich one would you like to call?",
                        data = mapOf(
                            "requires_disambiguation" to true,
                            "suggestions" to candidates.take(4).map { "Call ${it.displayName}" },
                            "candidates"  to candidates.map { mapOf("name" to it.displayName, "number" to it.number) }
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "ContactTool"
    }
}
