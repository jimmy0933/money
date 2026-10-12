package com.moneytracker.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
        viewModel = MainViewModel(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAddTransactionUpdatesState() = runTest {
        val accounts = viewModel.accounts.first()
        val defaultAccount = accounts.first()

        viewModel.addTransaction(
            type = TransactionType.EXPENSE,
            amount = 150,
            dateMillis = System.currentTimeMillis(),
            sourceAccountId = defaultAccount.id,
            targetAccountId = null,
            categoryId = null,
            note = "測試 ViewModel 記帳"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val txs = viewModel.transactions.first()
        assertTrue(txs.any { it.note == "測試 ViewModel 記帳" && it.amount == 150L })
    }

    @Test
    fun testMultipleLoansForSameContactAndBothTypes() = runTest {
        val accounts = viewModel.accounts.first()
        val defaultAccount = accounts.first()

        // 借出 10,000 給小明
        viewModel.addLoan(
            contactName = "小明",
            type = LoanType.LEND,
            amount = 10000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = defaultAccount.id,
            note = "第一次借出"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 再借出 2,000 給小明
        viewModel.addLoan(
            contactName = "小明",
            type = LoanType.LEND,
            amount = 2000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = defaultAccount.id,
            note = "第二次借出"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 向小明借入 3,000
        viewModel.addLoan(
            contactName = "小明",
            type = LoanType.BORROW,
            amount = 3000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = defaultAccount.id,
            note = "向小明借款"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val summaries = viewModel.contactSummaries.first()
        val xiaoming = summaries.find { it.contact.name == "小明" }

        assertNotNull(xiaoming)
        // 借出總額 12,000，未還 12,000
        assertEquals(12000L, xiaoming!!.totalLendAmount)
        assertEquals(12000L, xiaoming.outstandingLend)

        // 借入總額 3,000，未還 3,000
        assertEquals(3000L, xiaoming.totalBorrowAmount)
        assertEquals(3000L, xiaoming.outstandingBorrow)
    }

    @Test
    fun testOfflineBackupFailureDoesNotCorruptLocalData() = runTest {
        val accounts = viewModel.accounts.first()
        val defaultAccount = accounts.first()

        // 新增資料
        viewModel.addTransaction(
            type = TransactionType.INCOME,
            amount = 5000,
            dateMillis = System.currentTimeMillis(),
            sourceAccountId = defaultAccount.id,
            targetAccountId = null,
            categoryId = null,
            note = "離線資料測試"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 執行無網路/無帳號備份 (預期報告錯誤訊息)
        viewModel.performBackup(password = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val msg = viewModel.uiMessage.value
        assertNotNull(msg)

        // 驗證本機資料依然完好無損
        val txs = viewModel.transactions.first()
        assertTrue(txs.any { it.note == "離線資料測試" })
    }
}
