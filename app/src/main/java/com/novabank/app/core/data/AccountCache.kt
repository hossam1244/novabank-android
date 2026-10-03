package com.novabank.app.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** Cached account rows — the dashboard paints instantly on cold start. */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iban: String,
    val currency: String,
    val balance: String,
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name")
    suspend fun all(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun clear()
}

@Database(entities = [AccountEntity::class], version = 1, exportSchema = false)
abstract class NovaBankDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
