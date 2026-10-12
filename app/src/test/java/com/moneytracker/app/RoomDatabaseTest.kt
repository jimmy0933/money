package com.moneytracker.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDefaultDataSeeding() = runBlocking {
        AppDatabase.seedDefaultData(db)

        val accounts = db.accountDao().getAllAccounts()
        assertTrue(accounts.isNotEmpty())
        assertEquals(4, accounts.size)

        val categories = db.categoryDao().getAllCategories()
        assertTrue(categories.isNotEmpty())
        assertEquals(12, categories.size) // 8 expense + 4 income
    }

    @Test
    fun testIncomeExpenseAndAccountBalance() = runBlocking {
        val accountId = repository.insertAccount(
            AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 1000)
        )

        // 收入 500
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 500,
                dateMillis = System.currentTimeMillis(),
                sourceAccountId = accountId,
                note = "薪水"
            )
        )

        // 支出 200
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 200,
                dateMillis = System.currentTimeMillis(),
                sourceAccountId = accountId,
                note = "午餐"
            )
        )

        val balance = repository.calculateAccountBalance(accountId)
        // 1000 + 500 - 200 = 1300
        assertEquals(1300L, balance)
    }

    @Test
    fun testTransferBetweenAccountsDoesNotAffectIncomeExpense() = runBlocking {
        val acc1 = repository.insertAccount(
            AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 2000)
        )
        val acc2 = repository.insertAccount(
            AccountEntity(name = "銀行", type = AccountType.BANK, initialBalance = 5000)
        )

        // 轉帳 1000 從 現金 到 銀行
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 1000,
                dateMillis = System.currentTimeMillis(),
                sourceAccountId = acc1,
                targetAccountId = acc2,
                note = "存款"
            )
        )

        // 現金餘額: 2000 - 1000 = 1000
        // 銀行餘額: 5000 + 1000 = 6000
        assertEquals(1000L, repository.calculateAccountBalance(acc1))
        assertEquals(6000L, repository.calculateAccountBalance(acc2))

        // 一般收入/支出統計必須不包含轉帳
        val now = System.currentTimeMillis()
        val incomeSum = db.transactionDao().getIncomeSumBetween(now - 10000, now + 10000)
        val expenseSum = db.transactionDao().getExpenseSumBetween(now - 10000, now + 10000)

        assertEquals(0L, incomeSum)
        assertEquals(0L, expenseSum)
    }

    @Test
    fun testLendAndPartRepaymentCalculations() = runBlocking {
        val accountId = repository.insertAccount(
            AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 20000)
        )

        // 我借給小明 10,000 元
        val loanId = repository.createLoanTransaction(
            contactName = "小明",
            type = LoanType.LEND,
            amount = 10000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = accountId,
            note = "小明借錢"
        )

        // 現金帳戶應減少 10,000 -> 剩餘 10,000
        assertEquals(10000L, repository.calculateAccountBalance(accountId))

        var loanDetails = repository.getLoanWithDetails(loanId)
        assertNotNull(loanDetails)
        assertEquals(0L, loanDetails!!.repaidAmount)
        assertEquals(10000L, loanDetails.remainingAmount)
        assertEquals(LoanStatus.UNPAID, loanDetails.loan.status)

        // 小明還款 3,000
        val res1 = repository.addRepayment(
            loanId = loanId,
            amount = 3000,
            dateMillis = System.currentTimeMillis(),
            accountId = accountId,
            note = "部分還款"
        )
        assertTrue(res1.isSuccess)

        // 現金帳戶應增加 3,000 -> 剩餘 13,000
        assertEquals(13000L, repository.calculateAccountBalance(accountId))

        loanDetails = repository.getLoanWithDetails(loanId)
        assertEquals(3000L, loanDetails!!.repaidAmount)
        assertEquals(7000L, loanDetails.remainingAmount)
        assertEquals(LoanStatus.PARTIAL, loanDetails.loan.status)

        // 再還款 7,000
        val res2 = repository.addRepayment(
            loanId = loanId,
            amount = 7000,
            dateMillis = System.currentTimeMillis(),
            accountId = accountId,
            note = "清償"
        )
        assertTrue(res2.isSuccess)

        // 現金帳戶應再增加 7,000 -> 20,000
        assertEquals(20000L, repository.calculateAccountBalance(accountId))

        loanDetails = repository.getLoanWithDetails(loanId)
        assertEquals(10000L, loanDetails!!.repaidAmount)
        assertEquals(0L, loanDetails.remainingAmount)
        assertEquals(LoanStatus.PAID, loanDetails.loan.status)
    }

    @Test
    fun testExcessRepaymentBlocked() = runBlocking {
        val accountId = repository.insertAccount(
            AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 20000)
        )

        val loanId = repository.createLoanTransaction(
            contactName = "小明",
            type = LoanType.LEND,
            amount = 10000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = accountId,
            note = "測試超額還款"
        )

        // 還款 10,001 (超過 10,000)
        val res = repository.addRepayment(
            loanId = loanId,
            amount = 10001,
            dateMillis = System.currentTimeMillis(),
            accountId = accountId,
            note = "超額還款"
        )

        assertTrue(res.isFailure)
    }

    @Test
    fun testDeleteRepaymentRecalculatesStatusAndBalance() = runBlocking {
        val accountId = repository.insertAccount(
            AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 10000)
        )

        val loanId = repository.createLoanTransaction(
            contactName = "老王",
            type = LoanType.BORROW,
            amount = 5000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = accountId,
            note = "向老王借錢"
        )

        // 向老王借 5000 -> 現金增加為 15000
        assertEquals(15000L, repository.calculateAccountBalance(accountId))

        // 我償還 2000 -> 現金減少為 13000
        val repaymentId = repository.addRepayment(
            loanId = loanId,
            amount = 2000,
            dateMillis = System.currentTimeMillis(),
            accountId = accountId,
            note = "還老王"
        ).getOrThrow()

        assertEquals(13000L, repository.calculateAccountBalance(accountId))
        var loanDetails = repository.getLoanWithDetails(loanId)
        assertEquals(3000L, loanDetails!!.remainingAmount)
        assertEquals(LoanStatus.PARTIAL, loanDetails.loan.status)

        // 刪除還款
        repository.deleteRepayment(repaymentId)

        // 刪除還款後，金額應回歸 15000，剩餘借款應回到 5000，狀態回到 UNPAID
        assertEquals(15000L, repository.calculateAccountBalance(accountId))
        loanDetails = repository.getLoanWithDetails(loanId)
        assertEquals(5000L, loanDetails!!.remainingAmount)
        assertEquals(LoanStatus.UNPAID, loanDetails.loan.status)
    }
}
