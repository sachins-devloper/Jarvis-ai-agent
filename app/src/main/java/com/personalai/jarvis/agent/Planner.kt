package com.personalai.jarvis.agent

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.regex.Pattern

data class ParsedPlan(
    val isToolCall: Boolean,
    val toolName: String? = null,
    val arguments: Map<String, Any> = emptyMap(),
    val directResponse: String? = null,
    val rawText: String
)

class Planner(private val toolRegistry: ToolRegistry) {
    private val gson = Gson()

    fun buildPrompt(
        userMessage: String,
        memoryContext: List<String> = emptyList(),
        history: List<Pair<String, String>> = emptyList(),
        intermediateSteps: List<Pair<String, ToolResult>> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are Jarvis, a capable, local-first Android AI assistant.")
        sb.appendLine("You run directly on the user's Android phone without internet servers.")
        sb.appendLine("You can answer questions and perform phone actions using your tools.")
        sb.appendLine()

        // Tools
        sb.appendLine(toolRegistry.generateSystemPromptToolDescriptions())

        // Memory Facts
        if (memoryContext.isNotEmpty()) {
            sb.appendLine("User Context & Long-Term Memory:")
            memoryContext.forEach { fact ->
                sb.appendLine("- $fact")
            }
            sb.appendLine()
        }

        // Conversation History
        if (history.isNotEmpty()) {
            sb.appendLine("Conversation History:")
            history.takeLast(6).forEach { (speaker, text) ->
                sb.appendLine("$speaker: $text")
            }
            sb.appendLine()
        }

        // Current request
        sb.appendLine("User Request: $userMessage")

        // Intermediate Tool executions if this is in a ReAct loop turn
        if (intermediateSteps.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Actions taken so far:")
            intermediateSteps.forEach { (toolCallStr, result) ->
                sb.appendLine("Action: $toolCallStr")
                sb.appendLine("Observation: ${result.output} (Success: ${result.success})")
            }
            sb.appendLine()
            sb.appendLine("Based on the observation, synthesize a helpful, concise final response or call the next tool if needed.")
        } else {
            sb.appendLine()
            sb.appendLine("Decide whether to execute a tool or answer directly:")
        }

        return sb.toString()
    }

    fun parseOutput(rawOutput: String): ParsedPlan {
        val cleaned = rawOutput.trim()

        // Look for JSON block in markdown ```json ... ``` or plain {...}
        val jsonPattern = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.MULTILINE)
        val matcher = jsonPattern.matcher(cleaned)

        val jsonStr = if (matcher.find()) {
            matcher.group(1)
        } else {
            // Check if entire text or a substring starts with '{' and ends with '}'
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start != -1 && end > start) {
                cleaned.substring(start, end + 1)
            } else {
                null
            }
        }

        if (jsonStr != null) {
            try {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any> = gson.fromJson(jsonStr, mapType)

                val toolName = (map["tool"] as? String) ?: (map["action"] as? String)
                if (!toolName.isNullOrBlank()) {
                    @Suppress("UNCHECKED_CAST")
                    val args = (map["arguments"] as? Map<String, Any>)
                        ?: (map["parameters"] as? Map<String, Any>)
                        ?: emptyMap()

                    return ParsedPlan(
                        isToolCall = true,
                        toolName = toolName.trim(),
                        arguments = args,
                        directResponse = null,
                        rawText = cleaned
                    )
                }
            } catch (ignored: Exception) {
                // Not valid JSON, fall through to direct response
            }
        }

        // Check for line-based "Action: tool_name"
        val actionMatch = Regex("""Action:\s*([a-zA-Z0-9_-]+)""").find(cleaned)
        if (actionMatch != null) {
            val toolName = actionMatch.groupValues[1]
            return ParsedPlan(
                isToolCall = true,
                toolName = toolName,
                arguments = emptyMap(),
                directResponse = null,
                rawText = cleaned
            )
        }

        // Direct natural language response
        return ParsedPlan(
            isToolCall = false,
            toolName = null,
            arguments = emptyMap(),
            directResponse = cleaned,
            rawText = cleaned
        )
    }
}
