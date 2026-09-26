package com.personalai.jarvis.agent

import android.util.Log
import com.personalai.jarvis.ai.LocalLLM
import com.personalai.jarvis.memory.ChatMessage
import com.personalai.jarvis.memory.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

sealed class AgentEvent {
    object Idle : AgentEvent()
    data class Thinking(val message: String) : AgentEvent()
    data class ToolExecuting(val toolName: String, val arguments: Map<String, Any>) : AgentEvent()
    data class ToolExecuted(val toolName: String, val result: ToolResult) : AgentEvent()
    data class TokenStream(val token: String) : AgentEvent()
    data class Completed(
        val userPrompt: String,
        val finalResponse: String,
        val toolsExecuted: List<Pair<String, ToolResult>>,
        val suggestions: List<String> = emptyList()
    ) : AgentEvent()
    data class Error(val error: String) : AgentEvent()
}

class Agent(
    val toolRegistry: ToolRegistry,
    val localLLM: LocalLLM,
    val memoryRepository: MemoryRepository
) {
    val planner = Planner(toolRegistry)

    private val _events = MutableSharedFlow<AgentEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    companion object {
        private const val TAG = "JarvisAgent"
        private const val MAX_TOOL_STEPS = 3
    }

    suspend fun execute(userMessage: String): String = withContext(Dispatchers.Default) {
        try {
            _events.emit(AgentEvent.Thinking("Analyzing task with local AI..."))

            // 1. Fetch memory facts and recent conversation
            val facts = memoryRepository.getAllFacts().map { "${it.key}: ${it.value}" }
            val recentMessages = memoryRepository.getRecentMessages(6).map { it.sender to it.text }

            val intermediateSteps = mutableListOf<Pair<String, ToolResult>>()
            var currentIteration = 0
            var finalResponse = ""
            var allSuggestions = mutableListOf<String>()

            while (currentIteration < MAX_TOOL_STEPS) {
                currentIteration++

                // Build prompt
                val prompt = planner.buildPrompt(
                    userMessage = userMessage,
                    memoryContext = facts,
                    history = recentMessages,
                    intermediateSteps = intermediateSteps
                )

                // Run inference
                val rawOutput = localLLM.generateComplete(prompt)
                Log.d(TAG, "LLM Output (turn $currentIteration):\n$rawOutput")

                // Parse output
                val plan = planner.parseOutput(rawOutput)

                if (plan.isToolCall && plan.toolName != null) {
                    val toolName = plan.toolName
                    val args = plan.arguments

                    _events.emit(AgentEvent.ToolExecuting(toolName, args))

                    // Execute tool
                    val result = toolRegistry.execute(toolName, args)
                    intermediateSteps.add(toolName to result)

                    _events.emit(AgentEvent.ToolExecuted(toolName, result))

                    // If tool requires user disambiguation or provided suggestions
                    val toolSuggestions = (result.data?.get("suggestions") as? List<*>)?.filterIsInstance<String>()
                    if (!toolSuggestions.isNullOrEmpty()) {
                        allSuggestions.addAll(toolSuggestions)
                    }

                    if (result.data?.get("requires_disambiguation") == true) {
                        finalResponse = result.output
                        break
                    }

                    // If tool succeeded and we already executed an action, allow LLM to formulate synthesis
                    // Next iteration in loop will feed observation to LLM
                } else {
                    // Direct response reached
                    finalResponse = plan.directResponse ?: rawOutput
                    break
                }
            }

            if (finalResponse.isBlank() && intermediateSteps.isNotEmpty()) {
                val lastResult = intermediateSteps.last().second
                finalResponse = if (lastResult.success) {
                    lastResult.output
                } else {
                    "I encountered an issue: ${lastResult.output}"
                }
            }

            // Save conversation into memory
            val userChatMsg = ChatMessage(sender = "user", text = userMessage)
            val agentChatMsg = ChatMessage(
                sender = "jarvis",
                text = finalResponse,
                toolCall = intermediateSteps.firstOrNull()?.first,
                toolResult = intermediateSteps.firstOrNull()?.second?.output,
                suggestions = allSuggestions
            )
            memoryRepository.saveMessage(userChatMsg)
            memoryRepository.saveMessage(agentChatMsg)

            _events.emit(AgentEvent.Completed(userMessage, finalResponse, intermediateSteps, allSuggestions))
            finalResponse
        } catch (e: Exception) {
            Log.e(TAG, "Agent execution error", e)
            val err = "Error processing request: ${e.localizedMessage ?: e.message}"
            _events.emit(AgentEvent.Error(err))
            err
        }
    }
}
