package com.smartcli.core.cli

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.app.ActivityManager
import android.os.Build
import androidx.annotation.RequiresApi

class CommandProcessor {
    
    fun processCommand(command: String, context: Context): String {
        val parts = command.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return ""
        
        return when (parts[0].lowercase()) {
            "help" -> getHelpText()
            "lock" -> handleLock(context)
            "exit" -> handleExit(context)
            "map" -> handleMapCommand(parts.drop(1), context)
            "sms" -> handleSmsCommand(parts.drop(1), context)
            "call" -> handleCallCommand(parts.drop(1), context)
            "contact" -> handleContactCommand(parts.drop(1), context)
            "mail" -> handleMailCommand(parts.drop(1), context)
            "wifi" -> handleWifiCommand(parts.drop(1), context)
            "bluetooth" -> handleBluetoothCommand(parts.drop(1), context)
            "app" -> handleAppLaunch(parts.drop(1), context)
            "ps" -> handleProcessList(context)
            "kill" -> handleKillProcess(parts.drop(1), context)
            "settings" -> handleSettings(context)
            "reboot" -> handleReboot(context)
            "shutdown" -> handleShutdown(context)
            else -> "Command not found: $command. Type 'help' for available commands."
        }
    }

    private fun getHelpText(): String {
        return """
        ┌─────────────────────────────────────────────────────┐
        │                   SMARTCLI HELP                     │
        ├─────────────────────────────────────────────────────┤
        │ help       - Show this help message                │
        │ lock       - Lock device immediately               │
        │ exit       - Logout and lock device                │
        │ map <dest> - Open maps navigation                  │
        │ sms <num> <msg> - Send SMS message                 │
        │ call <number> - Make phone call                    │
        │ contact list - List all contacts                   │
        │ wifi [on|off|list] - WiFi control                  │
        │ bluetooth [on|off|list] - Bluetooth control        │
        │ app <name> - Launch application                    │
        │ ps         - Show running processes                │
        │ kill <pid> - Kill process by ID                    │
        │ settings   - Open device settings                  │
        │ reboot     - Reboot device (root)                  │
        │ shutdown   - Shutdown device (root)                │
        └─────────────────────────────────────────────────────┘
        """.trimIndent()
    }

    private fun handleLock(context: Context): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        return "Device locked"
    }

    private fun handleExit(context: Context): String {
        handleLock(context)
        return "Logged out and device locked"
    }

    private fun handleMapCommand(args: List<String>, context: Context): String {
        if (args.isEmpty()) return "Usage: map <destination>"
        
        val destination = args.joinToString(" ")
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        return try {
            context.startActivity(intent)
            "🗺️  Opening maps for: $destination"
        } catch (e: Exception) {
            "❌ No maps app found. Install Google Maps or similar."
        }
    }

    private fun handleCallCommand(args: List<String>, context: Context): String {
        if (args.isEmpty()) return "Usage: call <phone_number>"
        
        val number = args.joinToString("")
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        return try {
            context.startActivity(intent)
            "📞 Calling: $number"
        } catch (e: SecurityException) {
            "❌ CALL_PHONE permission required"
        } catch (e: Exception) {
            "❌ Error making call: ${e.message}"
        }
    }

    private fun handleSmsCommand(args: List<String>, context: Context): String {
        if (args.size < 2) return "Usage: sms <number> <message>"
        
        val number = args[0]
        val message = args.drop(1).joinToString(" ")
        
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:${Uri.encode(number)}")
        intent.putExtra("sms_body", message)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        return try {
            context.startActivity(intent)
            "✉️  SMS ready for: $number"
        } catch (e: Exception) {
            "❌ No SMS app available"
        }
    }

    private fun handleAppLaunch(args: List<String>, context: Context): String {
        if (args.isEmpty()) return "Usage: app <package_name>"
        
        val packageName = args[0]
        val pm = context.packageManager
        
        return try {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "🚀 Launched: $packageName"
            } else {
                "❌ App not found: $packageName"
            }
        } catch (e: Exception) {
            "❌ Error launching app: ${e.message}"
        }
    }

    private fun handleProcessList(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return "No running processes"
        
        val processes = runningProcesses.take(20).joinToString("\n") { process ->
            val appName = try {
                val packageInfo = context.packageManager.getPackageInfo(process.processName, 0)
                packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
            } catch (e: Exception) {
                process.processName
            }
            "📱 ${process.pid} | $appName"
        }
        
        return "Running Processes:\n$processes\n\nUse 'kill <pid>' to terminate"
    }

    private fun handleKillProcess(args: List<String>, context: Context): String {
        if (args.isEmpty()) return "Usage: kill <process_id>"
        
        return try {
            val pid = args[0].toInt()
            android.os.Process.killProcess(pid)
            "✅ Process $pid terminated"
        } catch (e: SecurityException) {
            "❌ Permission denied: Cannot kill system process"
        } catch (e: Exception) {
            "❌ Error killing process: ${e.message}"
        }
    }

    private fun handleWifiCommand(args: List<String>, context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        return when (args.firstOrNull()?.lowercase()) {
            "on" -> {
                wifiManager.isWifiEnabled = true
                "📶 WiFi enabled"
            }
            "off" -> {
                wifiManager.isWifiEnabled = false
                "📶 WiFi disabled"
            }
            "list" -> {
                val scanResults = wifiManager.scanResults ?: return "No networks found"
                val networks = scanResults.take(10).joinToString("\n") { result ->
                    val strength = when {
                        result.level >= -50 -> "🔴 Excellent"
                        result.level >= -60 -> "🟠 Good"
                        result.level >= -70 -> "🟡 Fair"
                        else -> "🟢 Weak"
                    }
                    "📡 ${result.SSID} | $strength (${result.level}dBm)"
                }
                "Available Networks:\n$networks"
            }
            else -> "Usage: wifi [on|off|list]"
        }
    }

    private fun handleBluetoothCommand(args: List<String>, context: Context): String {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() 
            ?: return "❌ Bluetooth not available on this device"
        
        return when (args.firstOrNull()?.lowercase()) {
            "on" -> {
                bluetoothAdapter.enable()
                "🔵 Bluetooth enabling..."
            }
            "off" -> {
                bluetoothAdapter.disable()
                "🔵 Bluetooth disabled"
            }
            "list" -> {
                val pairedDevices = bluetoothAdapter.bondedDevices
                if (pairedDevices.isEmpty()) return "No paired devices"
                
                val devices = pairedDevices.joinToString("\n") { device ->
                    val type = when (device.type) {
                        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                        BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                        BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                        else -> "Unknown"
                    }
                    "🎧 ${device.name} | $type | ${device.address}"
                }
                "Paired Devices:\n$devices"
            }
            else -> "Usage: bluetooth [on|off|list]"
        }
    }

    private fun handleContactCommand(args: List<String>, context: Context): String {
        return when (args.firstOrNull()?.lowercase()) {
            "list" -> getContactsList(context)
            else -> "Usage: contact list"
        }
    }

    private fun getContactsList(context: Context): String {
        val contacts = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            android.provider.ContactsContract.Contacts.CONTENT_URI,
            null, null, null, android.provider.ContactsContract.Contacts.DISPLAY_NAME
        )
        
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                if (!name.isNullOrEmpty()) {
                    contacts.add("👤 $name")
                }
            }
        }
        
        return if (contacts.isNotEmpty()) {
            contacts.take(15).joinToString("\n") + 
            if (contacts.size > 15) "\n... and ${contacts.size - 15} more" else ""
        } else {
            "No contacts found"
        }
    }

    private fun handleMailCommand(args: List<String>, context: Context): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        return try {
            context.startActivity(intent)
            "📧 Opening email client..."
        } catch (e: Exception) {
            "❌ No email app found"
        }
    }

    private fun handleSettings(context: Context): String {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        return try {
            context.startActivity(intent)
            "⚙️  Opening settings..."
        } catch (e: Exception) {
            "❌ Cannot open settings"
        }
    }

    private fun handleReboot(context: Context): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c reboot")
            "🔄 Device rebooting..."
        } catch (e: Exception) {
            "❌ Root access required for reboot"
        }
    }

    private fun handleShutdown(context: Context): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c reboot -p")
            "⭕ Device shutting down..."
        } catch (e: Exception) {
            "❌ Root access required for shutdown"
        }
    }
}
