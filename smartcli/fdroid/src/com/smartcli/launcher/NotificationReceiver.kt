package com.smartcli.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("SmartCLI", "Boot completed, starting SmartCLI...")
                // Start SmartCLI on boot
                val launchIntent = Intent(context, FdroidMainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
            }
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Some devices use this for boot completed
                val launchIntent = Intent(context, FdroidMainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
