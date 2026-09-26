package com.personalai.jarvis.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class CachedNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "JarvisNotification"

        @Volatile
        var instance: JarvisNotificationListenerService? = null
            private set

        private val recentNotifications = mutableListOf<CachedNotification>()

        fun isRunning(): Boolean = instance != null

        fun getRecentNotifications(): List<CachedNotification> {
            synchronized(recentNotifications) {
                return recentNotifications.toList()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Jarvis Notification Listener connected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotBlank() || text.isNotBlank()) {
            val item = CachedNotification(
                packageName = sbn.packageName,
                title = title,
                text = text,
                timestamp = sbn.postTime
            )

            synchronized(recentNotifications) {
                recentNotifications.add(0, item)
                if (recentNotifications.size > 30) {
                    recentNotifications.removeAt(recentNotifications.lastIndex)
                }
            }
            Log.d(TAG, "Notification cached from ${sbn.packageName}: $title - $text")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.i(TAG, "Jarvis Notification Listener disconnected.")
    }
}
