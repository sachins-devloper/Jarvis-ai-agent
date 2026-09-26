package com.personalai.jarvis.tools

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceControlTool(private val context: Context) : AgentTool {
    override val name: String = "device_control"
    override val description: String = "Controls device hardware and settings: flashlight, volume, battery level, or opening system settings."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "Action to perform: 'flashlight_on', 'flashlight_off', 'volume_up', 'volume_down', 'battery_status', 'open_wifi_settings', 'open_bluetooth_settings'.",
                required = true
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.Main) {
        val action = (arguments["action"] as? String)?.lowercase()?.trim()
            ?: return@withContext ToolResult.error("Missing required parameter: action")

        try {
            when (action) {
                "flashlight_on", "torch_on" -> {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                        ?: return@withContext ToolResult.error("No camera found for flashlight.")
                    cameraManager.setTorchMode(cameraId, true)
                    ToolResult.success("Flashlight turned on.")
                }

                "flashlight_off", "torch_off" -> {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                        ?: return@withContext ToolResult.error("No camera found for flashlight.")
                    cameraManager.setTorchMode(cameraId, false)
                    ToolResult.success("Flashlight turned off.")
                }

                "volume_up" -> {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ToolResult.success("Volume increased.")
                }

                "volume_down" -> {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    ToolResult.success("Volume decreased.")
                }

                "battery_status" -> {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val isCharging = bm.isCharging
                    val state = if (isCharging) "charging" else "not charging"
                    ToolResult.success("Battery is at $level% and currently $state.")
                }

                "open_wifi_settings" -> {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult.success("Opened Wi-Fi settings.")
                }

                "open_bluetooth_settings" -> {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult.success("Opened Bluetooth settings.")
                }

                "storage_status", "check_storage", "get_storage" -> {
                    val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                    val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
                    val bytesTotal = stat.blockCountLong * stat.blockSizeLong
                    val usedBytes = bytesTotal - bytesAvailable
                    val usedGb = String.format("%.2f", usedBytes.toDouble() / (1024 * 1024 * 1024))
                    val totalGb = String.format("%.2f", bytesTotal.toDouble() / (1024 * 1024 * 1024))
                    val freeGb = String.format("%.2f", bytesAvailable.toDouble() / (1024 * 1024 * 1024))
                    val percentUsed = if (bytesTotal > 0) ((usedBytes.toDouble() / bytesTotal) * 100).toInt() else 0
                    ToolResult.success("Phone Storage: $usedGb GB used of $totalGb GB ($freeGb GB free, $percentUsed% used).")
                }

                "list_installed_apps", "get_apps" -> {
                    val pm = context.packageManager
                    val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA).filter {
                        pm.getLaunchIntentForPackage(it.packageName) != null
                    }.map { pm.getApplicationLabel(it).toString() }.distinct().sorted()
                    val preview = apps.take(15).joinToString(", ")
                    val extra = if (apps.size > 15) " and ${apps.size - 15} more" else ""
                    ToolResult.success("Found ${apps.size} installed apps: $preview$extra.")
                }

                "take_screenshot", "screenshot" -> {
                    val service = com.personalai.jarvis.services.JarvisAccessibilityService.instance
                    if (service != null) {
                        val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                        if (success) {
                            ToolResult.success("Screenshot captured.")
                        } else {
                            ToolResult.error("Could not capture screenshot.")
                        }
                    } else {
                        ToolResult.error("Accessibility Service is not enabled. Please enable Jarvis in Settings > Accessibility.")
                    }
                }

                "device_info", "device_specs", "about", "phone_info", "specs" -> {
                    DeviceInfoTool(context).execute(emptyMap())
                }

                else -> {
                    ToolResult.error("Unknown action '$action'. Available: flashlight_on, flashlight_off, volume_up, volume_down, battery_status, storage_status, device_info, list_installed_apps, take_screenshot, open_wifi_settings.")
                }
            }
        } catch (e: Exception) {
            ToolResult.error("Device control failed: ${e.localizedMessage ?: e.message}")
        }
    }
}
