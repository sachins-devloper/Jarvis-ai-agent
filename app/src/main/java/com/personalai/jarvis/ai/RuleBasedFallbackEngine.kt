package com.personalai.jarvis.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.regex.Pattern

/**
 * Built-in zero-dependency local rule and intent engine.
 * Ensures the agent is fully interactive immediately out of the box,
 * even before downloading a large GGUF model file.
 */
class RuleBasedFallbackEngine : LocalLLMEngine {
    override val name: String = "Jarvis Zero-Shot Fallback Engine"
    override val isLoaded: Boolean = true
    override val loadedModelFile: File? = null

    override suspend fun loadModel(modelFile: File): Result<Unit> = Result.success(Unit)
    override suspend fun unloadModel() {}

    override fun generate(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        stopTokens: List<String>
    ): Flow<String> = flow {
        val result = generateComplete(prompt, temperature, maxTokens)
        // Stream token chunks for realistic animation
        val words = result.split(" ")
        for (i in words.indices) {
            val chunk = if (i == words.size - 1) words[i] else words[i] + " "
            emit(chunk)
            kotlinx.coroutines.delay(25)
        }
    }

    override suspend fun generateComplete(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        // Extract the user request from the structured prompt
        val userReqRegex = Regex("""User Request:\s*(.*)""", RegexOption.IGNORE_CASE)
        val match = userReqRegex.find(prompt)
        val userText = (match?.groupValues?.get(1) ?: prompt).trim()

        // Check if this is an observation turn from a previous tool execution
        if (prompt.contains("Observation:")) {
            val obsRegex = Regex("""Observation:\s*(.*)""")
            val obsMatch = obsRegex.findAll(prompt).lastOrNull()
            val observation = obsMatch?.groupValues?.get(1) ?: "Operation completed."
            return "I've handled that for you. $observation"
        }

        // Check if there is recent conversation history indicating an active disambiguation prompt
        val historyRegex = Regex("""jarvis:\s*(.*?Which one would you like to (call|message|send this message to|open|launch)\?)""", RegexOption.DOT_MATCHES_ALL)
        val historyMatch = historyRegex.findAll(prompt).lastOrNull()
        if (historyMatch != null) {
            val lastAssistantMsg = historyMatch.groupValues[1]
            val actionType = historyMatch.groupValues[2]

            // Extract candidate numbered lines: e.g. "1. Arun Frnd (98765)"
            val lineRegex = Regex("""(\d+)\.\s*([^(:\n]+?)(?:\s*\(.*?\))?$""", RegexOption.MULTILINE)
            val candidates = lineRegex.findAll(lastAssistantMsg).map { it.groupValues[1].toInt() to it.groupValues[2].trim() }.toList()

            var chosenCandidate: String? = null
            val userChoiceNum = userText.toIntOrNull()
            if (userChoiceNum != null) {
                chosenCandidate = candidates.firstOrNull { it.first == userChoiceNum }?.second
            } else {
                chosenCandidate = candidates.firstOrNull {
                    it.second.equals(userText, ignoreCase = true) || userText.contains(it.second, ignoreCase = true)
                }?.second
            }

            if (chosenCandidate != null) {
                if (actionType.contains("call")) {
                    return """
                    ```json
                    {
                      "tool": "make_call",
                      "arguments": {
                        "contact": "$chosenCandidate"
                      }
                    }
                    ```
                    """.trimIndent()
                } else if (actionType.contains("message") || actionType.contains("send")) {
                    return """
                    ```json
                    {
                      "tool": "send_message",
                      "arguments": {
                        "recipient": "$chosenCandidate",
                        "message": "Hello"
                      }
                    }
                    ```
                    """.trimIndent()
                } else if (actionType.contains("open") || actionType.contains("launch")) {
                    return """
                    ```json
                    {
                      "tool": "open_app",
                      "arguments": {
                        "app_name": "$chosenCandidate"
                      }
                    }
                    ```
                    """.trimIndent()
                }
            }
        }

        val lower = userText.lowercase()

        // 1. App Launching Intent
        val openAppPattern = Pattern.compile("""(?:open|launch|start|run)\s+(?:the\s+)?([a-zA-Z0-9\s]+?)(?:\s+app)?$""", Pattern.CASE_INSENSITIVE)
        val openAppMatcher = openAppPattern.matcher(userText)
        if (openAppMatcher.find()) {
            val targetApp = openAppMatcher.group(1)?.trim() ?: ""
            if (targetApp.isNotBlank() && !targetApp.equals("flashlight", ignoreCase = true) && !targetApp.equals("alarm", ignoreCase = true)) {
                return """
                ```json
                {
                  "tool": "open_app",
                  "arguments": {
                    "app_name": "$targetApp"
                  }
                }
                ```
                """.trimIndent()
            }
        }

        // 2. Alarm Intent
        if (lower.contains("alarm")) {
            var hour = 7
            var minute = 0
            val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
            val timeMatch = timeRegex.find(userText)
            if (timeMatch != null) {
                val rawH = timeMatch.groupValues[1].toIntOrNull() ?: 7
                val rawM = timeMatch.groupValues[2].toIntOrNull() ?: 0
                val ampm = timeMatch.groupValues[3].lowercase()

                hour = when {
                    ampm == "pm" && rawH < 12 -> rawH + 12
                    ampm == "am" && rawH == 12 -> 0
                    else -> rawH
                }
                minute = rawM
            }
            return """
            ```json
            {
              "tool": "set_alarm",
              "arguments": {
                "hour": $hour,
                "minute": $minute,
                "message": "Jarvis Alarm"
              }
            }
            ```
            """.trimIndent()
        }

        // 3. Search / Web Intent
        val searchRegex = Regex("""(?:search|google|browse|look up)\s+(?:for\s+)?(.*)""", RegexOption.IGNORE_CASE)
        val searchMatch = searchRegex.find(userText)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            return """
            ```json
            {
              "tool": "open_browser",
              "arguments": {
                "query": "$query"
              }
            }
            ```
            """.trimIndent()
        }
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.endsWith(".com")) {
            return """
            ```json
            {
              "tool": "open_browser",
              "arguments": {
                "url": "$userText"
              }
            }
            ```
            """.trimIndent()
        }

        // 4. Device Controls (Flashlight, Volume, Settings)
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val action = if (lower.contains("off")) "flashlight_off" else "flashlight_on"
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "$action"
              }
            }
            ```
            """.trimIndent()
        }

        if (lower.contains("volume")) {
            val action = if (lower.contains("up") || lower.contains("increase")) "volume_up" else "volume_down"
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "$action"
              }
            }
            ```
            """.trimIndent()
        }

        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "open_wifi_settings"
              }
            }
            ```
            """.trimIndent()
        }

        if (lower.contains("battery") || lower.contains("charging") || lower.contains("power level")) {
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "battery_status"
              }
            }
            ```
            """.trimIndent()
        }

        // 5. Note / Memory
        if (lower.contains("note") || lower.contains("remember that") || lower.contains("remind me to")) {
            val noteContent = userText.replace(Regex("""^(?:take a note|add note|note|remember that|remind me to)[:\s]*""", RegexOption.IGNORE_CASE), "").trim()
            return """
            ```json
            {
              "tool": "create_note",
              "arguments": {
                "content": "$noteContent"
              }
            }
            ```
            """.trimIndent()
        }

        // 6. Calling Intent (e.g. "call Mom", "dial 9876543210", "make a call to John")
        val callPattern = Regex("""^(?:call|dial|phone|make a call to)\s+([a-zA-Z0-9\s+]+)$""", RegexOption.IGNORE_CASE)
        val callMatch = callPattern.find(userText.trim())
        if (callMatch != null) {
            val contact = callMatch.groupValues[1].trim()
            return """
            ```json
            {
              "tool": "make_call",
              "arguments": {
                "contact": "$contact"
              }
            }
            ```
            """.trimIndent()
        }

        // 7. Messaging Intent
        // Pattern A: "send hello message to akshaya" or "send a quick message to John"
        val sendMsgToPattern = Regex("""(?:send|write)\s+(?:a\s+)?(.+?)\s+message\s+to\s+([a-zA-Z0-9\s]+)$""", RegexOption.IGNORE_CASE)
        val sendMsgToMatch = sendMsgToPattern.find(userText)
        if (sendMsgToMatch != null) {
            val messageContent = sendMsgToMatch.groupValues[1].trim()
            val recipient = sendMsgToMatch.groupValues[2].trim()
            return """
            ```json
            {
              "tool": "send_message",
              "arguments": {
                "recipient": "$recipient",
                "message": "$messageContent"
              }
            }
            ```
            """.trimIndent()
        }

        // Pattern B: "send message to akshaya saying hello" / "text akshaya hello" / "message John: are you free?"
        if (lower.contains("send message") || lower.contains("text ") || lower.contains("whatsapp") || lower.startsWith("message ")) {
            val msgPatternB = Regex("""(?:send\s+)?(?:message|text|whatsapp)\s+(?:to\s+)?([a-zA-Z0-9\s]+?)(?:\s+(?:saying|that|with message)\s*|[:,-]\s*|\s+)(.*)""", RegexOption.IGNORE_CASE)
            val mMatch = msgPatternB.find(userText)
            val recipient = mMatch?.groupValues?.get(1)?.trim() ?: "Contact"
            val message = mMatch?.groupValues?.get(2)?.trim()?.ifBlank { "Hello" } ?: "Hello"
            return """
            ```json
            {
              "tool": "send_message",
              "arguments": {
                "recipient": "$recipient",
                "message": "$message"
              }
            }
            ```
            """.trimIndent()
        }

        // 8. General Knowledge & Conversational Q&A
        val isGreeting = Regex("""^(?:hello|hi|hey|good\s+morning|good\s+evening|good\s+afternoon)[\s!.,?]*$""", RegexOption.IGNORE_CASE).matches(lower)

        // Built-in Knowledge Bank
        when {
            lower.contains("gravity") ->
                return "Gravity is a fundamental natural force by which physical bodies attract each other with a force proportional to their masses. It gives weight to physical objects on Earth and causes the planets to orbit the Sun."

            lower.contains("what is python") || (lower.contains("python") && lower.contains("language")) ->
                return "Python is a high-level, general-purpose programming language known for its clear, readable syntax. It is widely used in data science, artificial intelligence, backend development, and automation."

            lower.contains("photosynthesis") ->
                return "Photosynthesis is the biological process by which green plants and certain organisms use sunlight, water, and carbon dioxide to create oxygen and energy in the form of sugar (glucose)."

            lower.contains("black hole") ->
                return "A black hole is a region of spacetime where gravity is so intense that nothing—including light or other electromagnetic particles—has sufficient escape velocity to escape its event horizon."

            lower.contains("dna") ->
                return "DNA (Deoxyribonucleic acid) is a double-helix molecule carrying genetic instructions for the development, functioning, growth, and reproduction of all known organisms and viruses."

            lower.contains("quantum computing") ->
                return "Quantum computing uses quantum mechanics principles (superposition and entanglement) to perform calculations exponentially faster than classical supercomputers for specific complex problems."

            lower.contains("artificial intelligence") || lower.contains("what is ai") ->
                return "Artificial Intelligence (AI) is the simulation of human intelligence processes by computer systems, enabling machines to learn, reason, solve problems, and understand human language."

            lower.contains("who are you") || lower.contains("what are you") ->
                return "I am Jarvis, your personal local Android AI agent. I run completely on-device without relying on external cloud APIs, allowing me to execute phone tasks, manage tools, and converse securely."

            lower.contains("what can you do") || lower.contains("help") ->
                return "I can launch applications, set alarms, make phone calls, send messages, search the web, check battery status, control hardware (like flashlight and volume), take notes, and automate UI actions on your phone."

            isGreeting ->
                return "Hello! Jarvis online and ready. What can I help you do on your phone today?"

            Regex("""^(?:thanks|thank you|thx)[\s!.,?]*$""", RegexOption.IGNORE_CASE).matches(lower) ->
                return "You're welcome! Let me know if there's anything else you need."

            Regex("""^(?:ok|okay|got it|sure|alright|cool|fine)[\s!.,?]*$""", RegexOption.IGNORE_CASE).matches(lower) ->
                return "Ready whenever you are!"

            Regex("""^(?:bye|goodbye|see you)[\s!.,?]*$""", RegexOption.IGNORE_CASE).matches(lower) ->
                return "Goodbye! Call me anytime you need assistance."
        }

        // 9. Autonomous Search for any query, question, or unknown search intent (e.g. "cheif minister vijay", "what is X")
        if (userText.isNotBlank()) {
            return """
            ```json
            {
              "tool": "open_browser",
              "arguments": {
                "query": "$userText"
              }
            }
            ```
            """.trimIndent()
        }

        return "I am ready. Ask me to launch an app, make a call, send a message, check battery, or search for any information."
    }
}
