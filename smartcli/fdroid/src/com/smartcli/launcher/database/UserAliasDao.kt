package com.smartcli.launcher.database

import androidx.room.*

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
