// CommandDatabase.kt - ONLY the database class
package com.smartcli.launcher.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [CommandHistory::class, UserAlias::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CommandDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun userAliasDao(): UserAliasDao

    companion object {
        @Volatile
        private var INSTANCE: CommandDatabase? = null

        fun getInstance(context: Context): CommandDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CommandDatabase::class.java,
                    "command_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
