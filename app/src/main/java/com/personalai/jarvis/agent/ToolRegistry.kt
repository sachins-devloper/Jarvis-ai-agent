package com.personalai.jarvis.agent

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun register(tool: AgentTool) {
        tools[tool.name.lowercase()] = tool
        Log.d("ToolRegistry", "Registered tool: ${tool.name}")
    }

    fun registerAll(vararg tools: AgentTool) {
        tools.forEach { register(it) }
    }

    fun getTool(name: String): AgentTool? {
        return tools[name.lowercase()]
    }

    fun getAllTools(): List<AgentTool> {
        return tools.values.toList()
    }

    suspend fun execute(name: String, arguments: Map<String, Any>): ToolResult {
        val tool = getTool(name) ?: return ToolResult.error("Tool '$name' not found in registry. Available tools: ${tools.keys.joinToString()}")
        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            Log.e("ToolRegistry", "Error executing tool '$name'", e)
            ToolResult.error("Execution error in tool '$name': ${e.localizedMessage ?: e.message}")
        }
    }

    fun generateSystemPromptToolDescriptions(): String {
        val sb = StringBuilder()
        sb.appendLine("You have access to the following Android tools:")
        sb.appendLine()
        tools.values.forEach { tool ->
            sb.appendLine("- Tool: ${tool.name}")
            sb.appendLine("  Description: ${tool.description}")
            if (tool.definition.parameters.isNotEmpty()) {
                sb.appendLine("  Parameters:")
                tool.definition.parameters.forEach { param ->
                    val req = if (param.required) "required" else "optional"
                    sb.appendLine("    * ${param.name} (${param.type}, $req): ${param.description}")
                }
            } else {
                sb.appendLine("  Parameters: none")
            }
            sb.appendLine()
        }
        sb.appendLine("To use a tool, respond with ONLY a JSON block formatted exactly like this:")
        sb.appendLine("```json")
        sb.appendLine("{")
        sb.appendLine("  \"tool\": \"tool_name\",")
        sb.appendLine("  \"arguments\": {")
        sb.appendLine("    \"param_name\": \"value\"")
        sb.appendLine("  }")
        sb.appendLine("}")
        sb.appendLine("```")
        sb.appendLine("If no tool is needed (e.g. conversational questions), respond naturally with normal text.")
        return sb.toString()
    }
}
