package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllTransactionsWithDeleted(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdWithDeleted(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE loanId = :loanId AND isDeleted = 0")
    suspend fun getTransactionsByLoanId(loanId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE repaymentId = :repaymentId AND isDeleted = 0")
    suspend fun getTransactionsByRepaymentId(repaymentId: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE loanId = :loanId AND isDeleted = 0")
    suspend fun softDeleteTransactionsByLoanId(loanId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE repaymentId = :repaymentId AND isDeleted = 0")
    suspend fun softDeleteTransactionsByRepaymentId(repaymentId: Long, updatedAt: Long = System.currentTimeMillis())

    // 帳戶進出金流加總 (排除一般收支的非現金流)
    // 扣除項目: 支出 (EXPENSE, sourceAccountId=account), 轉出 (TRANSFER, sourceAccountId=account)
    // 增加項目: 收入 (INCOME, sourceAccountId=account), 轉入 (TRANSFER, targetAccountId=account)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sourceAccountId = :accountId AND type = 'EXPENSE' AND isDeleted = 0")
    suspend fun getTotalExpenseForAccount(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sourceAccountId = :accountId AND type = 'EXPENSE'")
    suspend fun getTotalExpenseForAccountWithDeleted(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sourceAccountId = :accountId AND type = 'INCOME' AND isDeleted = 0")
    suspend fun getTotalIncomeForAccount(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sourceAccountId = :accountId AND type = 'INCOME'")
    suspend fun getTotalIncomeForAccountWithDeleted(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sourceAccountId = :accountId AND type = 'TRANSFER' AND isDeleted = 0")
    suspend fun getTotalTransferOutForAccount(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE sourceAccountId = :accountId AND type = 'TRANSFER'")
    suspend fun getTotalTransferOutForAccountWithDeleted(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE targetAccountId = :accountId AND type = 'TRANSFER' AND isDeleted = 0")
    suspend fun getTotalTransferInForAccount(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE targetAccountId = :accountId AND type = 'TRANSFER'")
    suspend fun getTotalTransferInForAccountWithDeleted(accountId: Long): Long

    // 依日期區間與條件計算真實收支 (排除借貸與轉帳)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND loanId IS NULL AND repaymentId IS NULL AND isDeleted = 0 AND dateMillis BETWEEN :startMillis AND :endMillis")
    fun getIncomeSumBetweenFlow(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND loanId IS NULL AND repaymentId IS NULL AND isDeleted = 0 AND dateMillis BETWEEN :startMillis AND :endMillis")
    fun getExpenseSumBetweenFlow(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND loanId IS NULL AND repaymentId IS NULL AND isDeleted = 0 AND dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getIncomeSumBetween(startMillis: Long, endMillis: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND loanId IS NULL AND repaymentId IS NULL AND isDeleted = 0 AND dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getExpenseSumBetween(startMillis: Long, endMillis: Long): Long
}
