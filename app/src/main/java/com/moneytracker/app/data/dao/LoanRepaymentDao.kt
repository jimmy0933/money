package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanRepaymentDao {
    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId AND isDeleted = 0 ORDER BY dateMillis ASC, id ASC")
    fun getRepaymentsByLoanIdFlow(loanId: Long): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM loan_repayments WHERE isDeleted = 0")
    fun getAllRepaymentsFlow(): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId AND isDeleted = 0 ORDER BY dateMillis ASC, id ASC")
    suspend fun getRepaymentsByLoanId(loanId: Long): List<LoanRepaymentEntity>

    @Query("SELECT * FROM loan_repayments WHERE isDeleted = 0")
    suspend fun getAllRepayments(): List<LoanRepaymentEntity>

    @Query("SELECT * FROM loan_repayments")
    suspend fun getAllRepaymentsWithDeleted(): List<LoanRepaymentEntity>

    @Query("SELECT * FROM loan_repayments WHERE id = :id AND isDeleted = 0")
    suspend fun getRepaymentById(id: Long): LoanRepaymentEntity?

    @Query("SELECT * FROM loan_repayments WHERE id = :id")
    suspend fun getRepaymentByIdWithDeleted(id: Long): LoanRepaymentEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loan_repayments WHERE loanId = :loanId AND isDeleted = 0")
    suspend fun getTotalRepaidForLoan(loanId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loan_repayments WHERE loanId = :loanId")
    suspend fun getTotalRepaidForLoanWithDeleted(loanId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loan_repayments WHERE loanId = :loanId AND isDeleted = 0")
    fun getTotalRepaidForLoanFlow(loanId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long

    @Update
    suspend fun updateRepayment(repayment: LoanRepaymentEntity)

    @Query("UPDATE loan_repayments SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteRepayment(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE loan_repayments SET isDeleted = 1, updatedAt = :updatedAt WHERE loanId = :loanId AND isDeleted = 0")
    suspend fun softDeleteRepaymentsByLoanId(loanId: Long, updatedAt: Long = System.currentTimeMillis())
}
