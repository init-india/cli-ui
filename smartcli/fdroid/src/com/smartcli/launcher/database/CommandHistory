package com.smartcli.launcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val timestamp: Long,
    val success: Boolean = true
)
