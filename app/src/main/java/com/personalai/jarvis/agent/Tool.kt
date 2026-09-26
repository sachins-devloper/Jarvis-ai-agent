package com.personalai.jarvis.agent

data class ToolParameter(
    val name: String,
    val type: String, // "string", "number", "boolean", "object"
    val description: String,
    val required: Boolean = true
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>
)

data class ToolResult(
    val success: Boolean,
    val output: String,
    val data: Map<String, Any>? = null
) {
    companion object {
        fun success(output: String, data: Map<String, Any>? = null) = ToolResult(true, output, data)
        fun error(message: String, data: Map<String, Any>? = null) = ToolResult(false, message, data)
    }
}

interface AgentTool {
    val name: String
    val description: String
    val definition: ToolDefinition

    suspend fun execute(arguments: Map<String, Any>): ToolResult
}
