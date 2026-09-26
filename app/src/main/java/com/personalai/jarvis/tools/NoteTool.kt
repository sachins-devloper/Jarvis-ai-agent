package com.personalai.jarvis.tools

import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import com.personalai.jarvis.memory.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateNoteTool(private val memoryRepository: MemoryRepository) : AgentTool {
    override val name: String = "create_note"
    override val description: String = "Saves a note, reminder, or personal fact to the local memory database."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "content",
                type = "string",
                description = "Text content of the note or memory item to save.",
                required = true
            ),
            ToolParameter(
                name = "category",
                type = "string",
                description = "Category for organization (e.g. 'notes', 'preferences', 'tasks').",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val content = (arguments["content"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: content")
        val category = (arguments["category"] as? String)?.trim() ?: "notes"

        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val key = "note_$timestampStr"

        memoryRepository.saveFact(key = key, value = content, category = category)

        ToolResult.success("Note saved successfully: \"$content\" (Category: $category)")
    }
}
