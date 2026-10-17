package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllAccountsWithDeleted(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND isDeleted = 0")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteAccount(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM accounts WHERE isDeleted = 0")
    suspend fun getAccountCount(): Int
}
