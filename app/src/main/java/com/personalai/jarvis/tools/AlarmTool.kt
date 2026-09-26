package com.personalai.jarvis.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SetAlarmTool(private val context: Context) : AgentTool {
    override val name: String = "set_alarm"
    override val description: String = "Sets an alarm on the Android device for a specific hour and minute."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "hour",
                type = "number",
                description = "Hour of the day in 24-hour format (0-23).",
                required = true
            ),
            ToolParameter(
                name = "minute",
                type = "number",
                description = "Minute of the hour (0-59).",
                required = false
            ),
            ToolParameter(
                name = "message",
                type = "string",
                description = "Alarm label or description.",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val rawHour = arguments["hour"]
        val hour = when (rawHour) {
            is Number -> rawHour.toInt()
            is String -> rawHour.toIntOrNull()
            else -> null
        } ?: return@withContext ToolResult.error("Missing or invalid 'hour' parameter (must be 0-23).")

        val rawMinute = arguments["minute"]
        val minute = when (rawMinute) {
            is Number -> rawMinute.toInt()
            is String -> rawMinute.toIntOrNull()
            else -> 0
        } ?: 0

        val label = (arguments["message"] as? String) ?: "Jarvis Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", hour, minute)
            ToolResult.success(
                output = "Alarm successfully scheduled for $formattedTime with label '$label'.",
                data = mapOf("hour" to hour, "minute" to minute, "label" to label)
            )
        } catch (e: Exception) {
            ToolResult.error("Failed to set alarm: ${e.localizedMessage ?: e.message}")
        }
    }
}
