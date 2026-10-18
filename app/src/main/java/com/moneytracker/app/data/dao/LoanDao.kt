package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.model.LoanType
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    fun getAllLoansFlow(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllLoans(): List<LoanEntity>

    @Query("SELECT * FROM loans ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllLoansWithDeleted(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE type = :type AND isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    fun getLoansByTypeFlow(type: LoanType): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE contactId = :contactId AND isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    fun getLoansByContactIdFlow(contactId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE contactId = :contactId AND isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    suspend fun getLoansByContactId(contactId: Long): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id AND isDeleted = 0")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanByIdWithDeleted(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("UPDATE loans SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteLoan(id: Long, updatedAt: Long = System.currentTimeMillis())
}
