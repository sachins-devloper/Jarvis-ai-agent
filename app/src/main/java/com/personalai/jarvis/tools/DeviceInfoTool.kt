package com.personalai.jarvis.tools

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * DeviceInfoTool — Retrieves detailed device hardware, OS, memory, storage, battery, and network specifications.
 */
class DeviceInfoTool(private val context: Context) : AgentTool {

    override val name: String = "get_device_info"
    override val description: String = "Retrieves information about this mobile device including model, manufacturer, Android version, battery level & status, RAM usage, storage space, and network connectivity."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "category",
                type = "string",
                description = "Category of device info: 'all', 'specs', 'battery', 'storage', 'ram', 'network'. Defaults to 'all'.",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val category = (arguments["category"] as? String)?.lowercase()?.trim() ?: "all"

        try {
            val sb = StringBuilder()

            when (category) {
                "battery" -> {
                    sb.append(getBatteryInfo())
                }
                "storage" -> {
                    sb.append(getStorageInfo())
                }
                "ram", "memory" -> {
                    sb.append(getRamInfo())
                }
                "network" -> {
                    sb.append(getNetworkInfo())
                }
                "specs", "hardware" -> {
                    sb.appendLine(getHardwareSpecs())
                    sb.append(getOsInfo())
                }
                else -> {
                    sb.appendLine("📱 Device Information:")
                    sb.appendLine("• Model: ${getDeviceName()}")
                    sb.appendLine("• OS: ${getOsInfo()}")
                    sb.appendLine("• Battery: ${getBatteryInfo()}")
                    sb.appendLine("• RAM: ${getRamInfo()}")
                    sb.appendLine("• Storage: ${getStorageInfo()}")
                    sb.appendLine("• Network: ${getNetworkInfo()}")
                    sb.appendLine("• Uptime: ${getUptimeInfo()}")
                }
            }

            ToolResult.success(sb.toString().trim())
        } catch (e: Exception) {
            ToolResult.error("Failed to retrieve device info: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL
        val brand = Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model ($brand)"
        }
    }

    private fun getOsInfo(): String {
        val release = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        return "Android $release (API $sdk)"
    }

    private fun getBatteryInfo(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            val statusStr = if (isCharging) "Charging ⚡" else "Discharging"
            "$level% ($statusStr)"
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getRamInfo(): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)

            val totalGb = memInfo.totalMem.toDouble() / (1024.0.pow(3))
            val availGb = memInfo.availMem.toDouble() / (1024.0.pow(3))
            val usedGb = totalGb - availGb
            val percentUsed = ((usedGb / totalGb) * 100).toInt()

            String.format("%.1f GB used of %.1f GB (%.1f GB free, %d%% used)", usedGb, totalGb, availGb, percentUsed)
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getStorageInfo(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val usedBytes = bytesTotal - bytesAvailable

            val totalGb = bytesTotal.toDouble() / (1024.0.pow(3))
            val usedGb = usedBytes.toDouble() / (1024.0.pow(3))
            val freeGb = bytesAvailable.toDouble() / (1024.0.pow(3))
            val percentUsed = if (bytesTotal > 0) ((usedBytes.toDouble() / bytesTotal) * 100).toInt() else 0

            String.format("%.1f GB used of %.1f GB (%.1f GB free, %d%% used)", usedGb, totalGb, freeGb, percentUsed)
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getNetworkInfo(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNet = cm.activeNetwork ?: return "Offline / Disconnected"
            val caps = cm.getNetworkCapabilities(activeNet) ?: return "Disconnected"

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected via Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected via Mobile Data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected via Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Connected via Bluetooth Tethering"
                else -> "Connected"
            }
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getHardwareSpecs(): String {
        val board = Build.BOARD
        val hardware = Build.HARDWARE
        val arch = System.getProperty("os.arch") ?: "Unknown"
        return "Hardware: $hardware, Board: $board, Architecture: $arch"
    }

    private fun getUptimeInfo(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60
        return "$hours hours, $minutes minutes"
    }
}
