package com.smartcli.launcher.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.util.Date


@Database(
    entities = [CommandHistory::class, UserAlias::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CommandDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun userAliasDao(): UserAliasDao
}

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "command") val command: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "success") val success: Boolean
)

@Entity(tableName = "user_aliases")
data class UserAlias(
    @PrimaryKey @ColumnInfo(name = "alias") val alias: String,
    @ColumnInfo(name = "command") val command: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCommands(limit: Int): List<CommandHistory>

    @Insert
    suspend fun insertCommand(command: CommandHistory)

    @Query("DELETE FROM command_history WHERE timestamp < :timestamp")
    suspend fun deleteOldCommands(timestamp: Long)
}

@Dao
interface UserAliasDao {
    @Query("SELECT * FROM user_aliases WHERE alias = :alias")
    suspend fun getAlias(alias: String): UserAlias?

    @Insert
    suspend fun insertAlias(alias: UserAlias)

    @Query("DELETE FROM user_aliases WHERE alias = :alias")
    suspend fun deleteAlias(alias: String)

    @Query("SELECT * FROM user_aliases")
    suspend fun getAllAliases(): List<UserAlias>
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
