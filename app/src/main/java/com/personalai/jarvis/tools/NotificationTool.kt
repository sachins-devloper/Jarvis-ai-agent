package com.personalai.jarvis.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import com.personalai.jarvis.services.JarvisNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationTool(private val context: Context) : AgentTool {
    override val name: String = "notification_manager"
    override val description: String = "Posts a system notification or reads recently received phone notifications."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "'post' to send a notification, or 'read_recent' to inspect recent notifications.",
                required = true
            ),
            ToolParameter(
                name = "title",
                type = "string",
                description = "Notification title (required for 'post').",
                required = false
            ),
            ToolParameter(
                name = "message",
                type = "string",
                description = "Notification content body (required for 'post').",
                required = false
            )
        )
    )

    companion object {
        private const val CHANNEL_ID = "jarvis_agent_channel"
        private const val CHANNEL_NAME = "Jarvis Agent"
    }

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val action = (arguments["action"] as? String)?.lowercase()?.trim() ?: "post"

        if (action == "read_recent") {
            if (!JarvisNotificationListenerService.isRunning()) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return@withContext ToolResult.error("Notification Listener service is not active. Please grant notification access in Android settings.")
            }

            val recent = JarvisNotificationListenerService.getRecentNotifications()
            if (recent.isEmpty()) {
                return@withContext ToolResult.success("No recent notifications captured yet.")
            }

            val summary = recent.take(5).joinToString("\n") {
                "- [${it.packageName}] ${it.title}: ${it.text}"
            }
            return@withContext ToolResult.success("Recent notifications:\n$summary")
        }

        // Post notification
        val title = (arguments["title"] as? String) ?: "Jarvis"
        val message = (arguments["message"] as? String) ?: "Task completed."

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Jarvis Agent Notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        ToolResult.success("Notification posted: '$title - $message'.")
    }
}
