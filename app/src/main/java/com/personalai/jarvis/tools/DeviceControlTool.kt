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

                else -> {
                    ToolResult.error("Unknown action '$action'. Available: flashlight_on, flashlight_off, volume_up, volume_down, battery_status, open_wifi_settings.")
                }
            }
        } catch (e: Exception) {
            ToolResult.error("Device control failed: ${e.localizedMessage ?: e.message}")
        }
    }
}
