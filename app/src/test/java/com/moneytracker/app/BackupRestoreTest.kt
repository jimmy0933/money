package com.moneytracker.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.moneytracker.app.data.backup.BackupCryptoManager
import com.moneytracker.app.data.backup.BackupPayload
import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.data.repository.AppRepository
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
class BackupRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var appRepo: AppRepository
    private lateinit var backupRepo: BackupRestoreRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appRepo = AppRepository(db)
        backupRepo = BackupRestoreRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSerializationAndEncryptionWithoutPassword() = runBlocking {
        // 新增 50 筆交易與 5 筆借貸情境測試
        val accId = appRepo.insertAccount(AccountEntity(name = "測試帳戶", type = AccountType.CASH, initialBalance = 50000))
        val catId = appRepo.insertCategory(CategoryEntity(name = "測試分類", type = TransactionType.EXPENSE))

        for (i in 1..50) {
            appRepo.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = (i * 10).toLong(),
                    dateMillis = System.currentTimeMillis(),
                    sourceAccountId = accId,
                    categoryId = catId,
                    note = "筆記 $i"
                )
            )
        }

        for (i in 1..5) {
            val loanId = appRepo.createLoanTransaction(
                contactName = "朋友 $i",
                type = LoanType.LEND,
                amount = (i * 1000).toLong(),
                dateMillis = System.currentTimeMillis(),
                dueDateMillis = null,
                accountId = accId,
                note = "借款 $i"
            )
            appRepo.addRepayment(
                loanId = loanId,
                amount = 200,
                dateMillis = System.currentTimeMillis(),
                accountId = accId,
                note = "部分還款 $i"
            )
        }

        val payload = BackupCryptoManager.createBackupPayload(db)
        assertEquals(50 + 5 + 5, payload.transactions.size) // 50 筆 + 5 筆借貸交易 + 5 筆還款交易
        assertEquals(5, payload.loans.size)
        assertEquals(5, payload.loanRepayments.size)

        val bytes = BackupCryptoManager.serializeAndEncryptPayload(payload, password = null)
        val checksum = BackupCryptoManager.calculateSha256(bytes)
        assertTrue(checksum.isNotEmpty())

        val deserializedPayload = BackupCryptoManager.decryptAndDeserializePayload(bytes, password = null)
        assertEquals(payload.accounts.size, deserializedPayload.accounts.size)
        assertEquals(payload.transactions.size, deserializedPayload.transactions.size)
        assertEquals(payload.loans.size, deserializedPayload.loans.size)
    }

    @Test
    fun testEncryptionAndDecryptionWithPassword() = runBlocking {
        val accId = appRepo.insertAccount(AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 10000))
        val payload = BackupCryptoManager.createBackupPayload(db)

        val password = "MySecretPassword123"
        val encryptedBytes = BackupCryptoManager.serializeAndEncryptPayload(payload, password)

        // 使用正確密碼解密
        val decrypted = BackupCryptoManager.decryptAndDeserializePayload(encryptedBytes, password)
        assertEquals(1, decrypted.accounts.size)
        assertEquals("現金", decrypted.accounts[0].name)
    }

    @Test(expected = Exception::class)
    fun testDecryptionWithWrongPasswordFails() = runBlocking {
        val accId = appRepo.insertAccount(AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 10000))
        val payload = BackupCryptoManager.createBackupPayload(db)

        val password = "MySecretPassword123"
        val encryptedBytes = BackupCryptoManager.serializeAndEncryptPayload(payload, password)

        // 使用錯誤密碼解密，預期拋出異常
        BackupCryptoManager.decryptAndDeserializePayload(encryptedBytes, "WrongPassword")
        Unit
    }

    @Test
    fun testRestoreFromBackupPayloadRetainsAllData() = runBlocking {
        // 原本 DB 資料
        val accId = appRepo.insertAccount(AccountEntity(name = "原本帳戶", type = AccountType.CASH, initialBalance = 10000))
        val loanId = appRepo.createLoanTransaction(
            contactName = "小明",
            type = LoanType.LEND,
            amount = 5000,
            dateMillis = System.currentTimeMillis(),
            dueDateMillis = null,
            accountId = accId,
            note = "測試備份"
        )
        appRepo.addRepayment(loanId, 2000, System.currentTimeMillis(), accId, "還 2000")

        // 建立備份 Payload
        val payload = BackupCryptoManager.createBackupPayload(db)

        // 清空並污染現有 DB 資料 (模擬 App 移除重裝)
        db.clearAllTables()
        appRepo.insertAccount(AccountEntity(name = "全新乾淨帳戶", type = AccountType.BANK, initialBalance = 0))

        // 執行還原
        val result = backupRepo.restoreFromPayload(payload)
        assertTrue(result.isSuccess)

        // 驗證還原結果
        val accounts = db.accountDao().getAllAccounts()
        assertEquals(1, accounts.size)
        assertEquals("原本帳戶", accounts[0].name)

        val restoredLoan = appRepo.getLoanWithDetails(loanId)
        assertNotNull(restoredLoan)
        assertEquals(2000L, restoredLoan!!.repaidAmount)
        assertEquals(3000L, restoredLoan.remainingAmount)
    }
}
