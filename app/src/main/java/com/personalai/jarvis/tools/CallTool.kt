package com.personalai.jarvis.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CallTool — Places a phone call given a resolved phone number.
 *
 * Does NOT do contact lookup — use ContactTool first when only a name is known.
 *
 * Priority order:
 *  1. TelecomManager.placeCall()  → direct call, no dialer opens
 *  2. Intent.ACTION_CALL          → direct call, may open in-call UI
 *  3. Intent.ACTION_DIAL          → opens dialer (fallback if CALL_PHONE denied)
 *
 * Tool call JSON:
 *   { "tool": "make_call", "arguments": { "number": "+919876543210", "display_name": "Mom" } }
 */
class CallTool(private val context: Context) : AgentTool {

    override val name        = "make_call"
    override val description = "Places a direct phone call to a phone number. Use lookup_contact first if you only have a name."

    override val definition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "number",
                type = "string",
                description = "Phone number to call (digits, +, - allowed).",
                required = true
            ),
            ToolParameter(
                name = "display_name",
                type = "string",
                description = "Display name for the contact (for confirmation message). Optional.",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val number = (arguments["number"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: number")
        val displayName = (arguments["display_name"] as? String)?.trim() ?: number

        val cleanDigits = number.replace(Regex("[\\s\\-]"), "")
        if (!cleanDigits.matches(Regex("""^[+0-9]+$"""))) {
            return@withContext ToolResult.error("Invalid phone number: '$number'")
        }

        val uri = Uri.parse("tel:$cleanDigits")
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return@withContext try {
            if (hasCallPermission) {
                // Attempt 1: TelecomManager.placeCall (silent, no dialer, best)
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                var placed = false
                if (telecom != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        telecom.placeCall(uri, null)
                        placed = true
                        Log.i(TAG, "Call placed via TelecomManager → $displayName ($cleanDigits)")
                    } catch (e: Exception) {
                        Log.w(TAG, "TelecomManager.placeCall failed", e)
                    }
                }

                // Attempt 2: ACTION_CALL
                if (!placed) {
                    context.startActivity(
                        Intent(Intent.ACTION_CALL, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    Log.i(TAG, "Call placed via ACTION_CALL → $displayName ($cleanDigits)")
                }

                ToolResult.success(
                    output = "📞 Calling $displayName ($number)…",
                    data   = mapOf("number" to cleanDigits, "display_name" to displayName, "direct" to true)
                )
            } else {
                // Fallback: open dialer
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                ToolResult.success(
                    output = "Dialer opened for $displayName. Grant 'Phone' permission for hands-free calling.",
                    data   = mapOf("number" to cleanDigits, "display_name" to displayName, "direct" to false)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Call failed", e)
            ToolResult.error("Failed to call $displayName: ${e.localizedMessage ?: e.message}")
        }
    }

    companion object {
        private const val TAG = "CallTool"
    }
}
