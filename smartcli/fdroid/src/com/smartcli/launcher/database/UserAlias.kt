package com.smartcli.launcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_aliases")
data class UserAlias(
    @PrimaryKey val alias: String,
    val command: String,
    val createdAt: Long = System.currentTimeMillis()
)
