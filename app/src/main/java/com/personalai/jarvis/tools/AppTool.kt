package com.personalai.jarvis.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAppTool(private val context: Context) : AgentTool {
    override val name: String = "open_app"
    override val description: String = "Launches an installed Android application by name or package identifier."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "app_name",
                type = "string",
                description = "Common name of the app (e.g. 'YouTube', 'WhatsApp', 'Chrome', 'Camera', 'Settings') or full package name.",
                required = true
            )
        )
    )

    private val commonPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "whatsapp" to "com.whatsapp",
        "settings" to "com.android.settings",
        "camera" to "com.google.android.GoogleCamera",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "mail" to "com.google.android.gm",
        "play store" to "com.android.vending",
        "store" to "com.android.vending",
        "spotify" to "com.spotify.music",
        "clock" to "com.google.android.deskclock",
        "alarm" to "com.google.android.deskclock",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "gallery" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos"
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val appQuery = (arguments["app_name"] as? String)?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: app_name")

        val pm: PackageManager = context.packageManager
        val queryLower = appQuery.lowercase()

        // 1. Check known mapped packages
        val mappedPackage = commonPackageMap[queryLower]

        // 2. Resolve target package by querying installed applications
        var targetPackage: String? = mappedPackage

        if (targetPackage == null || pm.getLaunchIntentForPackage(targetPackage) == null) {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            // Exact label match
            val exactMatch = installedApps.firstOrNull { appInfo ->
                pm.getApplicationLabel(appInfo).toString().equals(appQuery, ignoreCase = true)
            }
            // Substring label match
            val fuzzyMatch = exactMatch ?: installedApps.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                label.contains(appQuery, ignoreCase = true)
            }

            if (fuzzyMatch != null) {
                targetPackage = fuzzyMatch.packageName
            }
        }

        // If user provided a direct package name
        if (targetPackage == null && appQuery.contains(".")) {
            targetPackage = appQuery
        }

        if (targetPackage == null) {
            return@withContext ToolResult.error("Could not find an installed application matching '$appQuery'.")
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            ?: return@withContext ToolResult.error("Application '$targetPackage' is installed but cannot be launched directly.")

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        val appLabel = try {
            val info = pm.getApplicationInfo(targetPackage, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            appQuery
        }

        ToolResult.success(
            output = "Successfully opened $appLabel ($targetPackage).",
            data = mapOf("package" to targetPackage, "label" to appLabel)
        )
    }
}
