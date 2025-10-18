package com.smartcli.launcher.database

import androidx.room.*

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCommands(limit: Int): List<CommandHistory>

    @Insert
    suspend fun insertCommand(command: CommandHistory)

    @Query("DELETE FROM command_history WHERE timestamp < :timestamp")
    suspend fun deleteOldCommands(timestamp: Long)
}
