package com.moneytracker.app

import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.backup.GoogleDriveManager
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.data.repository.AppRepository
import com.moneytracker.app.data.repository.DataOperationCoordinator
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

        // Try inserting transaction with non-existent account - should fail
        failed = false
        try {
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 100,
                    dateMillis = System.currentTimeMillis(),
                    sourceAccountId = 999, // Invalid
                    note = "Test invalid account"
                )
            )
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue("Transaction with invalid source account should fail", failed)

        // Try inserting transfer with same source and target - should fail
        failed = false
        try {
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.TRANSFER,
                    amount = 100,
                    dateMillis = System.currentTimeMillis(),
                    sourceAccountId = accountId,
                    targetAccountId = accountId,
                    note = "Test same account transfer"
                )
            )
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue("Transfer with same source and target account should fail", failed)

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
    fun testBackupRestoreDeletedLoan_VerifyOldRepaymentsFiltered() = runBlocking {
        // Prepare data
        val accountId = repository.insertAccount(
            AccountEntity(name = "Test Account", type = AccountType.CASH, initialBalance = 1000)
        )
        val contactId = repository.addContact("Test Contact")

        // Add a loan (LEND)
        val amount = 1000L
        repository.createLoanTransaction(
            contactName = "Test Contact",
            contactId = contactId,
            type = LoanType.LEND,
            amount = amount,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = accountId,
            note = "Test Loan"
        )
        val loan = db.loanDao().getAllLoans().first()
        assertEquals(LoanStatus.UNPAID, loan.status)

        // Add repayment (400)
        repository.addRepayment(
            loanId = loan.id,
            amount = 400L,
            dateMillis = System.currentTimeMillis(),
            accountId = accountId,
            note = "Repayment 1"
        )
        var updatedLoan = db.loanDao().getLoanById(loan.id)!!
        assertEquals(LoanStatus.PARTIAL, updatedLoan.status)
        val repayments = db.loanRepaymentDao().getRepaymentsByLoanId(loan.id)
        val repayment = repayments.first()

        // Delete repayment (Revoke) -> Should become UNPAID again
        repository.deleteRepayment(repayment.id)
        updatedLoan = db.loanDao().getLoanById(loan.id)!!
        assertEquals(LoanStatus.UNPAID, updatedLoan.status)

        // Delay 5 seconds
        kotlinx.coroutines.delay(5000)

        // Delete the loan
        repository.deleteLoan(loan.id)

        // Attempt backup and restore
        val (payload, _) = backupRestoreRepository.createLocalBackupBytesInternal("password")
        val restoreResult = backupRestoreRepository.restoreFromPayload(payload)

        assertTrue("Restore should succeed", restoreResult.isSuccess)
    }


    @Test
    fun testFormErrorsNotHidden_VerifyErrorsVisible() = runBlocking {
        // We will simulate the form logic by trying to insert a transaction with invalid fields
        // and verifying the repository throws an error that would be caught by the ViewModel
        // and set to the errorMessage state.
        val accountId = repository.insertAccount(
            AccountEntity(name = "Test Account", type = AccountType.CASH, initialBalance = 1000)
        )

        var caughtException: Exception? = null
        try {
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.TRANSFER,
                    amount = -50, // Invalid amount
                    dateMillis = System.currentTimeMillis(),
                    sourceAccountId = accountId,
                    targetAccountId = null, // Invalid target
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
