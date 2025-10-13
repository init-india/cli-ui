package com.smartcli.core.cli

import android.content.Context
import android.os.BatteryManager
import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.os.StatFs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TUIFormatter {
    
    companion object {
        fun formatSystemStatus(context: Context): String {
            val batteryStatus = getBatteryStatus(context)
            val memoryStatus = getMemoryStatus(context)
            val storageStatus = getStorageStatus(context)
            val timeStatus = getTimeStatus()
            
            return "$batteryStatus $memoryStatus $storageStatus $timeStatus"
        }
        
        fun formatNotification(count: Int, type: String): String {
            return when (type) {
                "sms" -> "✉️$count"
                "call" -> "📞$count" 
                "whatsapp" -> "📱$count"
                "email" -> "📧$count"
                else -> "🔔$count"
            }
        }
        
        private fun getBatteryStatus(context: Context): String {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val batteryIcon = when {
                batteryLevel >= 80 -> "🟢"
                batteryLevel >= 50 -> "🟡"
                batteryLevel >= 20 -> "🟠"
                else -> "🔴"
            }
            return "$batteryIcon$batteryLevel%"
        }
        
        private fun getMemoryStatus(context: Context): String {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val usedMem = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024 * 1024)
            val totalMem = memoryInfo.totalMem / (1024 * 1024 * 1024)
            
            return "📱${usedMem}/${totalMem}GB"
        }
        
        private fun getStorageStatus(context: Context): String {
            val stat = StatFs(File("/data").absolutePath)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalGB = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
            val availableGB = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
            val usedGB = totalGB - availableGB
            
            return "💾${usedGB}/${totalGB}GB"
        }
        
        private fun getTimeStatus(): String {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy;HH:mm", Locale.getDefault())
            return "🕒${dateFormat.format(Date())}"
        }
    }
}
