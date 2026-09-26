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
        val rawText = (match?.groupValues?.get(1) ?: prompt).trim()
        // Strip wake words like "Jarvis," or "Hey Jarvis," so commands like "Jarvis, call Mom" map directly
        val userText = rawText.replace(Regex("""^(?:hey\s+)?jarvis[\s,:]+""", RegexOption.IGNORE_CASE), "").trim()

        // Check if this is an observation turn from a previous tool execution
        if (prompt.contains("Observation:")) {
            val obsRegex = Regex("""Observation:\s*([\s\S]*?)(?:\s*\(Success:|\n\nBased on|$)""")
            val obsMatch = obsRegex.find(prompt)
            val observation = obsMatch?.groupValues?.get(1)?.trim()
                ?: prompt.substringAfter("Observation:").substringBefore("(Success:").trim()
            return if (observation.isNotBlank()) observation else "Operation completed."
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
                    val prevIsWhatsApp = prompt.contains("whatsapp", ignoreCase = true)
                    val appJson = if (prevIsWhatsApp) """,\n    "app": "whatsapp"""" else ""
                    // Try to preserve original message content from earlier history
                    val bodyRegex = Regex("""(?:message|body|saying|text)[:=]?\s*["']?([^"'\n]+)["']?""", RegexOption.IGNORE_CASE)
                    val origMsg = bodyRegex.findAll(prompt).lastOrNull()?.groupValues?.get(1)?.trim()?.ifBlank { "Hello" } ?: "Hello"
                    return """
                    ```json
                    {
                      "tool": "send_message",
                      "arguments": {
                        "recipient": "$chosenCandidate",
                        "message": "$origMsg"$appJson
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

        // 0. Quick Math Calculation (Answer immediately without invoking device tools)
        val mathPattern = Regex("""^(?:what is|calculate|solve|evaluate)?\s*(\d+(?:\.\d+)?)\s*([\+\-\*\/xX×÷])\s*(\d+(?:\.\d+)?)\s*\??$""", RegexOption.IGNORE_CASE)
        val mathMatch = mathPattern.find(userText.trim())
        if (mathMatch != null) {
            val a = mathMatch.groupValues[1].toDoubleOrNull()
            val op = mathMatch.groupValues[2].lowercase()
            val b = mathMatch.groupValues[3].toDoubleOrNull()
            if (a != null && b != null) {
                val res = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*", "x", "×" -> a * b
                    "/", "÷" -> if (b != 0.0) a / b else null
                    else -> null
                }
                if (res != null) {
                    val formattedRes = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                    val formattedA = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                    val formattedB = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
                    val opSymbol = if (op == "x" || op == "X") "×" else op
                    return "$formattedA $opSymbol $formattedB = $formattedRes"
                }
            }
        }

        // 0.1 YouTube Video / Music Playback Intent
        if (lower.startsWith("play ") || (lower.contains("youtube") && !lower.startsWith("open youtube")) || lower.startsWith("watch ")) {
            val ytQuery = userText
                .replace(Regex("""^(?:play|watch|search\s+for|search)\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s+(?:on|in|using)\s+youtube\b""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (ytQuery.isNotBlank() && !ytQuery.equals("youtube", ignoreCase = true)) {
                return """
                ```json
                {
                  "tool": "play_youtube",
                  "arguments": {
                    "query": "$ytQuery"
                  }
                }
                ```
                """.trimIndent()
            }
        }

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

        // 2.9 Local Device File / Media Search Intent (Images, Music, Video, Documents, Files)
        val fileTypePattern = Regex("""\b(find|search|show|locate|list|get)\s+(?:all\s+)?(?:my\s+)?(files?|images?|imgs?|photos?|musics?|songs?|audio|videos?|movies?|documents?|docs?|pdfs?)(?:\s+(?:such\s+as|like|including|named|with|called|for|of|matching)\s+)?(.*)?$""", RegexOption.IGNORE_CASE)
        val fileMatch = fileTypePattern.find(userText)

        if (fileMatch != null || lower.startsWith("search file") || lower.startsWith("find file") || lower.startsWith("search img") || lower.startsWith("search photo") || lower.startsWith("search music") || lower.startsWith("search song") || lower.startsWith("search video")) {
            val matchedTypeRaw = fileMatch?.groupValues?.get(2)?.lowercase() ?: ""
            var searchKeyword = fileMatch?.groupValues?.get(3)?.trim() ?: ""

            // Clean common phrases like "such as img, music, video"
            if (searchKeyword.contains("such as", ignoreCase = true) || searchKeyword.contains("img, music", ignoreCase = true)) {
                searchKeyword = ""
            }

            val resolvedType = when {
                matchedTypeRaw.startsWith("img") || matchedTypeRaw.startsWith("image") || matchedTypeRaw.startsWith("photo") || lower.contains("image") || lower.contains("photo") || lower.contains("img") -> "image"
                matchedTypeRaw.startsWith("music") || matchedTypeRaw.startsWith("song") || matchedTypeRaw.startsWith("audio") || lower.contains("music") || lower.contains("song") || lower.contains("audio") -> "audio"
                matchedTypeRaw.startsWith("video") || matchedTypeRaw.startsWith("movie") || lower.contains("video") || lower.contains("movie") -> "video"
                matchedTypeRaw.startsWith("doc") || matchedTypeRaw.startsWith("pdf") || lower.contains("document") || lower.contains("pdf") -> "document"
                else -> "all"
            }

            if (searchKeyword.isBlank()) {
                val clean = userText
                    .replace(Regex("""^(?:search|find|show|locate|list|get)\s+""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""(?:my\s+)?(?:files?|images?|imgs?|photos?|musics?|songs?|audio|videos?|documents?|docs?|pdfs?)\b""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""^(?:named|with|for|called|matching|of|such as .*)\s*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (clean.isNotBlank() && !clean.equals("all", ignoreCase = true) && !clean.equals("local", ignoreCase = true)) {
                    searchKeyword = clean
                }
            }

            return """
            ```json
            {
              "tool": "search_files",
              "arguments": {
                "query": "$searchKeyword",
                "type": "$resolvedType",
                "limit": 10
              }
            }
            ```
            """.trimIndent()
        }

        // 3. Search / Web Intent
        val searchRegex = Regex("""^(?:search|searh|google|browse|look up)(?:\s+for|:|\s+)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val searchMatch = searchRegex.find(userText)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            if (query.isNotBlank()) {
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
            } else {
                return "What would you like me to search for? For example: 'search for latest tech news'."
            }
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

        // 4. Device Controls (Storage, Apps, Screenshot, Flashlight, Volume, Battery, Settings)
        if (lower.contains("storage") || lower.contains("disk space") || lower.contains("memory space") || lower.contains("internal memory") || lower.contains("free space")) {
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "storage_status"
              }
            }
            ```
            """.trimIndent()
        }

        if (lower.contains("what apps are installed") || lower.contains("list installed apps") || lower.contains("list apps") || lower.contains("show installed apps") || lower.contains("all apps")) {
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "list_installed_apps"
              }
            }
            ```
            """.trimIndent()
        }

        if (lower.contains("screenshot") || lower.contains("capture screen") || lower.contains("take a screen shot")) {
            return """
            ```json
            {
              "tool": "device_control",
              "arguments": {
                "action": "take_screenshot"
              }
            }
            ```
            """.trimIndent()
        }

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

        // 4.1 Device Info / Specs / About Intent (e.g. "what is my mobile device about", "phone specs")
        if (lower.contains("mobile device about") || lower.contains("about my mobile") || lower.contains("about my phone") ||
            lower.contains("about device") || lower.contains("about phone") || lower.contains("device info") ||
            lower.contains("phone info") || lower.contains("phone specs") || lower.contains("device specs") ||
            lower.contains("mobile specs") || lower.contains("what is my mobile") || lower.contains("what is my device") ||
            lower.contains("what phone is this") || lower.contains("phone details") || lower.contains("device details") ||
            lower.contains("system info") || lower.contains("ram status") || lower.contains("how much ram") ||
            lower.contains("ram usage") || lower.contains("android version") || lower.contains("os version") ||
            lower.contains("specs of my phone") || lower.contains("specs of this phone") ||
            (lower.contains("device") && lower.contains("about")) || (lower.contains("phone") && lower.contains("about"))
        ) {
            val category = when {
                lower.contains("ram") || lower.contains("memory") -> "ram"
                lower.contains("battery") -> "battery"
                lower.contains("storage") -> "storage"
                lower.contains("network") || lower.contains("wifi") -> "network"
                lower.contains("hardware") -> "specs"
                else -> "all"
            }
            return """
            ```json
            {
              "tool": "get_device_info",
              "arguments": {
                "category": "$category"
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

        // 5.1 Call Logs & Missed Calls Intent (e.g. "any missed calls", "who called me", "call history")
        if (lower.contains("missed call") || lower.contains("missed calls") || lower.contains("who called") ||
            lower.contains("any missed") || lower.contains("call log") || lower.contains("call logs") ||
            lower.contains("call history") || lower.contains("recent calls") || lower.contains("incoming calls") ||
            lower.contains("outgoing calls") || lower.contains("did anyone call") || lower.contains("did i miss any call")
        ) {
            val callType = when {
                lower.contains("incoming") -> "incoming"
                lower.contains("outgoing") -> "outgoing"
                lower.contains("all") || lower.contains("history") || lower.contains("log") -> "all"
                else -> "missed"
            }
            return """
            ```json
            {
              "tool": "get_call_log",
              "arguments": {
                "type": "$callType",
                "limit": 5
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
            if (!contact.equals("log", ignoreCase = true) && !contact.equals("logs", ignoreCase = true) && !contact.equals("history", ignoreCase = true)) {
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
        }

        // 7. Messaging Intent (SMS / WhatsApp)
        val isWhatsApp = lower.contains("whatsapp") || lower.contains("whats app")
        val appValue = if (isWhatsApp) "whatsapp" else "sms"

        // Strip app/platform markers from query text to cleanly isolate recipient and message
        // e.g. "send hello message to Akshaya in WhatsApp" -> "send hello message to Akshaya"
        val cleanMsgUserText = userText.replace(
            Regex("""\s*(?:in|on|via|through|using)\s*(?:whatsapp|whats\s*app|sms|text)\s*""", RegexOption.IGNORE_CASE),
            ""
        ).trim()

        // Pattern 1: Explicit "send message to <recipient> saying/colon/that <message>"
        // e.g. "send message to Akshaya saying hello", "send whatsapp to Akshaya: where are you"
        val patternSaying = Regex(
            """^(?:send\s+)?(?:a\s+)?(?:message|msg|text|whatsapp)\s+to\s+([a-zA-Z0-9\s+]+?)(?:\s+(?:saying|that|with message)\s*|[:,-]\s*|\s+)(.+)$""",
            RegexOption.IGNORE_CASE
        )
        val matchSaying = patternSaying.find(cleanMsgUserText)
        if (matchSaying != null) {
            val r = matchSaying.groupValues[1].trim()
            val m = matchSaying.groupValues[2].trim().ifBlank { "Hello" }
            return """
            ```json
            {
              "tool": "send_message",
              "arguments": {
                "recipient": "$r",
                "message": "$m",
                "app": "$appValue"
              }
            }
            ```
            """.trimIndent()
        }

        // Pattern 2: "send <message> message/msg to <recipient>"
        // e.g. "send hello message to Akshaya", "send good morning msg to Mom"
        val patternMsgTo = Regex(
            """^(?:send|write)\s+(?:a\s+)?(.+?)\s+(?:message|msg)\s+to\s+([a-zA-Z0-9\s+]+)$""",
            RegexOption.IGNORE_CASE
        )
        val matchMsgTo = patternMsgTo.find(cleanMsgUserText)
        if (matchMsgTo != null) {
            val m = matchMsgTo.groupValues[1].trim().ifBlank { "Hello" }
            val r = matchMsgTo.groupValues[2].trim()
            return """
            ```json
            {
              "tool": "send_message",
              "arguments": {
                "recipient": "$r",
                "message": "$m",
                "app": "$appValue"
              }
            }
            ```
            """.trimIndent()
        }

        // Pattern 3: "whatsapp/text/message <recipient> <message>"
        // e.g. "whatsapp Akshaya hello", "text Akshaya are you free", "message Mom I am home"
        val patternDirect = Regex(
            """^(?:whatsapp|text|message|msg)\s+([a-zA-Z0-9\s+]+?)(?:\s*[:,-]\s*|\s+)(.+)$""",
            RegexOption.IGNORE_CASE
        )
        val matchDirect = patternDirect.find(cleanMsgUserText)
        if (matchDirect != null) {
            val r = matchDirect.groupValues[1].trim()
            val m = matchDirect.groupValues[2].trim().ifBlank { "Hello" }
            return """
            ```json
            {
              "tool": "send_message",
              "arguments": {
                "recipient": "$r",
                "message": "$m",
                "app": "$appValue"
              }
            }
            ```
            """.trimIndent()
        }

        // Pattern 4: "send <message> to <recipient>" (when user mentioned whatsapp, sms, or text)
        // e.g. "send hello to Akshaya in WhatsApp", "send happy birthday to Arun"
        if (isWhatsApp || lower.contains("send ") || lower.contains("text")) {
            val patternSendTo = Regex(
                """^(?:send|write)\s+(?:a\s+)?(.+?)\s+to\s+([a-zA-Z0-9\s+]+)$""",
                RegexOption.IGNORE_CASE
            )
            val matchSendTo = patternSendTo.find(cleanMsgUserText)
            if (matchSendTo != null) {
                val m = matchSendTo.groupValues[1].trim().ifBlank { "Hello" }
                val r = matchSendTo.groupValues[2].trim()
                if (r.isNotBlank() && !r.equals("youtube", ignoreCase = true) && !r.equals("google", ignoreCase = true)) {
                    return """
                    ```json
                    {
                      "tool": "send_message",
                      "arguments": {
                        "recipient": "$r",
                        "message": "$m",
                        "app": "$appValue"
                      }
                    }
                    ```
                    """.trimIndent()
                }
            }
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

        // 9. Conversational Knowledge & Entity Answers in Chat
        when {
            lower.contains("youtube") ->
                return "YouTube is a global online video sharing and streaming platform owned by Google. It allows users to watch, upload, share, and comment on videos across music, education, news, and entertainment."

            lower.contains("vijay") ->
                return "Vijay (Joseph Vijay) is a prominent Indian actor and politician in Tamil Nadu. In February 2024, he founded his political party, Tamilaga Vettri Kazhagam (TVK), aiming for the 2026 Tamil Nadu Legislative Assembly elections."

            lower.contains("weather") ->
                return "For live real-time local weather updates, ask me to 'Search weather in my city' or open your weather app."

            lower.contains("joke") ->
                return "Why don't programmers like nature? It has too many bugs!"

            lower.contains("time") ->
                return "You can check the current time on your phone's status bar, or ask me to set an alarm for a specific time."
        }

        return "I am running locally on your device. I can launch apps, make calls, compose messages, check your battery, or control hardware settings. If you want live web results for '$userText', simply say 'Search for $userText'."
    }
}
