package com.personalai.jarvis.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * CallLogTool — Retrieves call history and missed calls from the user's phone.
 */
class CallLogTool(private val context: Context) : AgentTool {

    override val name: String = "get_call_log"
    override val description: String = "Retrieves recent call history and missed calls from the phone. Can filter for missed calls, incoming calls, outgoing calls, or all calls."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "type",
                type = "string",
                description = "Type of calls to retrieve: 'missed', 'incoming', 'outgoing', or 'all'. Defaults to 'missed'.",
                required = false
            ),
            ToolParameter(
                name = "limit",
                type = "integer",
                description = "Maximum number of call records to retrieve (default is 5).",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolResult.error("Permission READ_CALL_LOG is not granted. Please grant the Call Logs permission in Android Settings to check call history.")
        }

        val typeArg = (arguments["type"] as? String)?.lowercase()?.trim() ?: "missed"
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 5

        try {
            val selection = when (typeArg) {
                "missed" -> "${CallLog.Calls.TYPE} = ?"
                "incoming" -> "${CallLog.Calls.TYPE} = ?"
                "outgoing" -> "${CallLog.Calls.TYPE} = ?"
                else -> null
            }

            val selectionArgs = when (typeArg) {
                "missed" -> arrayOf(CallLog.Calls.MISSED_TYPE.toString())
                "incoming" -> arrayOf(CallLog.Calls.INCOMING_TYPE.toString())
                "outgoing" -> arrayOf(CallLog.Calls.OUTGOING_TYPE.toString())
                else -> null
            }

            val projection = arrayOf(
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            val sortOrder = "${CallLog.Calls.DATE} DESC"

            val calls = mutableListOf<CallRecord>()

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    val number = if (numIdx != -1) cursor.getString(numIdx) ?: "Unknown" else "Unknown"
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else CallLog.Calls.MISSED_TYPE
                    val dateMillis = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val durationSec = if (durIdx != -1) cursor.getLong(durIdx) else 0L

                    calls.add(CallRecord(name, number, type, dateMillis, durationSec))
                    count++
                }
            }

            if (calls.isEmpty()) {
                val msg = when (typeArg) {
                    "missed" -> "You have no missed calls."
                    "incoming" -> "No recent incoming calls found."
                    "outgoing" -> "No recent outgoing calls found."
                    else -> "No recent calls found in call history."
                }
                return@withContext ToolResult.success(msg)
            }

            val title = when (typeArg) {
                "missed" -> if (calls.size == 1) "You have 1 missed call:" else "You have ${calls.size} missed calls:"
                "incoming" -> "Recent incoming calls (${calls.size}):"
                "outgoing" -> "Recent outgoing calls (${calls.size}):"
                else -> "Recent calls (${calls.size}):"
            }

            val sb = java.lang.StringBuilder()
            sb.appendLine(title)

            calls.forEachIndexed { index, record ->
                val caller = if (!record.name.isNullOrBlank()) "${record.name} (${record.number})" else record.number
                val timeStr = formatCallTime(record.dateMillis)
                val typeTag = if (typeArg == "all") " [${getCallTypeString(record.type)}]" else ""
                sb.appendLine("${index + 1}. $caller$typeTag - $timeStr")
            }

            ToolResult.success(sb.toString().trimEnd())
        } catch (e: Exception) {
            ToolResult.error("Failed to read call log: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun getCallTypeString(type: Int): String {
        return when (type) {
            CallLog.Calls.MISSED_TYPE -> "Missed"
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.REJECTED_TYPE -> "Rejected"
            CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
            else -> "Call"
        }
    }

    private fun formatCallTime(dateMillis: Long): String {
        if (dateMillis <= 0) return "Unknown time"

        val callDate = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val now = Calendar.getInstance()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(dateMillis))

        return when {
            isSameDay(callDate, now) -> "Today at $formattedTime"
            isYesterday(callDate, now) -> "Yesterday at $formattedTime"
            callDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                val monthDayFormat = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
                monthDayFormat.format(Date(dateMillis))
            }
            else -> {
                val fullFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                fullFormat.format(Date(dateMillis))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(cal1, yesterday)
    }

    private data class CallRecord(
        val name: String?,
        val number: String,
        val type: Int,
        val dateMillis: Long,
        val durationSec: Long
    )
}
