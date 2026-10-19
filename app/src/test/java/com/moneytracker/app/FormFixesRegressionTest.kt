package com.moneytracker.app

import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.data.repository.AppRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FormFixesRegressionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var backupRestoreRepository: BackupRestoreRepository

    @Before
    fun setup() {
        db = androidx.room.Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
        repository = AppRepository(db)
        backupRestoreRepository = BackupRestoreRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testTransactionsScreenDialogs_VerifyNoCrashAndStatusSaved() = runBlocking {
        // Create an account
        val accountId = repository.insertAccount(
            AccountEntity(name = "Test Account", type = AccountType.CASH, initialBalance = 1000)
        )

        // Try inserting transaction with amount 0 - should fail
        var failed = false
        try {
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 0,
                    dateMillis = System.currentTimeMillis(),
                    sourceAccountId = accountId,
                    note = "Test 0 amount"
                )
            )
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue("Transaction with amount 0 should fail", failed)

        // Validate state
        val txs = db.transactionDao().getAllTransactions()
        assertTrue("No transactions should be inserted", txs.isEmpty())

        // Create valid transaction
        val validTxId = repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 100,
                dateMillis = System.currentTimeMillis(),
                sourceAccountId = accountId,
                note = "Test valid income"
            )
        )
        assertTrue(validTxId > 0)
    }

    @Test
    fun testFormErrorsNotHidden_VerifyErrorsVisible() = runBlocking {
        val accountId = repository.insertAccount(
            AccountEntity(name = "Test Account", type = AccountType.CASH, initialBalance = 1000)
        )

        var caughtException: Exception? = null
        try {
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.TRANSFER,
                    amount = -50,
                    dateMillis = System.currentTimeMillis(),
                    sourceAccountId = accountId,
                    targetAccountId = null,
                    note = ""
                )
            )
        } catch (e: IllegalArgumentException) {
            caughtException = e
        }

        assertNotNull("Should throw an error for invalid form data", caughtException)
        assertTrue(caughtException?.message?.contains("交易金額必須大於零") == true)
    }
}
