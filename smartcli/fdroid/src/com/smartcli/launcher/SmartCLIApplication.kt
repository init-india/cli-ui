package com.smartcli.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.smartcli.launcher.database.CommandDatabase

class SmartCLIApplication : Application() {

    companion object {
        lateinit var instance: SmartCLIApplication
        lateinit var commandDatabase: CommandDatabase
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        commandDatabase = Room.databaseBuilder(
            applicationContext,
            CommandDatabase::class.java,
            "command_database"
        ).build()

        // Create notification channel
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "smartcli_commands",
                "SmartCLI Commands",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CLI command notifications"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
