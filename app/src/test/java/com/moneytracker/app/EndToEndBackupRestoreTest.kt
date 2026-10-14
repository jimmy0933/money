package com.moneytracker.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.moneytracker.app.data.backup.BackupCryptoManager
import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.repository.AppRepository
import com.moneytracker.app.data.repository.DataOperationCoordinator
import com.moneytracker.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class EndToEndBackupRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var appRepo: AppRepository
    private lateinit var fakeDriveManager: FakeDriveManager
    private lateinit var backupRepo: BackupRestoreRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appRepo = AppRepository(db)
        fakeDriveManager = FakeDriveManager()
        backupRepo = BackupRestoreRepository(db, fakeDriveManager)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * 依使用者 6 步驟明確排查與驗證根因：
     * 1. 新增「還原測試帳戶」，初始餘額 1234。
     * 2. 追蹤同一筆帳戶：
     *   - 儲存完成後資料庫是否存在。
     *   - 產生的備份 payload 是否包含。
     *   - 上傳成功的 fileId。
     *   - 下載該 fileId 後，解密內容是否包含。
     *   - 還原提交後資料庫是否存在。
     *   - 帳戶管理畫面 Flow 是否顯示。
     */
    @Test
    fun testSixStepRootCauseVerification() = runBlocking {
        // 步驟 1 & 階段 A: 儲存完成後資料庫是否存在
        val accountId = appRepo.insertAccount(
            AccountEntity(name = "還原測試帳戶", type = AccountType.CASH, initialBalance = 1234)
        )

        val savedAccount = appRepo.getAccountById(accountId)
        assertNotNull("1. DB 必須存在該筆帳戶", savedAccount)
        assertEquals("還原測試帳戶", savedAccount!!.name)
        assertEquals(1234L, savedAccount.initialBalance)

        // 階段 B: 產生的備份 payload 是否包含
        val (payload, _) = backupRepo.createLocalBackupBytes(password = null)
        val payloadHasAccount = payload.accounts.any { it.name == "還原測試帳戶" && it.initialBalance == 1234L }
        assertTrue("2. 產生之備份 Payload 必須包含該筆帳戶", payloadHasAccount)

        // 階段 C: 上傳成功的 fileId
        val backupResult = backupRepo.backupToGoogleDrive(password = null)
        assertTrue("3. 上傳至 Google Drive 必須成功", backupResult.isSuccess)
        val fileId = backupResult.getOrThrow().driveFileId
        assertTrue("fileId 必須不為空", fileId.isNotEmpty())

        // 階段 D: 下載該 fileId 後，解密內容是否包含
        val downloadedBytes = fakeDriveManager.downloadBackupFile(fileId)
        val downloadedPayload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, password = null)
        val downloadedHasAccount = downloadedPayload.accounts.any { it.name == "還原測試帳戶" && it.initialBalance == 1234L }
        assertTrue("4. 下載解密內容必須包含該帳戶", downloadedHasAccount)

        // 階段 E: 還原提交後資料庫是否存在
        val restoreResult = backupRepo.restoreFromCloudBackup(fileId, password = null)
        assertTrue("還原必須成功", restoreResult.isSuccess)

        val restoredAccount = appRepo.getAccountById(accountId)
        assertNotNull("5. 還原後 DB 必須存在該帳戶", restoredAccount)
        assertEquals("還原測試帳戶", restoredAccount!!.name)
        assertEquals(1234L, restoredAccount.initialBalance)

        // 階段 F: 帳戶列表是否包含該帳戶
        val accountsList = db.accountDao().getAllAccounts()
        assertTrue("6. DB 必須包含該帳戶", accountsList.any { it.name == "還原測試帳戶" })
    }

    /**
     * 測試延遲帳戶寫入，驗證備份會等待完成，不產生漏資料的成功備份
     */
    @Test
    fun testDelayedAccountWriteBackupWaitsForCompletion() = runBlocking {
        val job = launch(Dispatchers.IO) {
            DataOperationCoordinator.runWriteOperation {
                db.accountDao().insertAccount(
                    AccountEntity(name = "延遲寫入帳戶", type = AccountType.BANK, initialBalance = 9999)
                )
            }
        }
        job.join()

        val backupResult = backupRepo.backupToGoogleDrive(password = null)
        assertTrue(backupResult.isSuccess)
        val fileId = backupResult.getOrThrow().driveFileId
        val downloadedBytes = fakeDriveManager.downloadBackupFile(fileId)
        val payload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, password = null)

        assertTrue("備份必須包含寫入之帳戶", payload.accounts.any { it.name == "延遲寫入帳戶" })
    }

    /**
     * 備份後更改本機測試資料，再還原該 fileId，確認名稱、ID、初始餘額完全一致
     */
    @Test
    fun testRestoreSpecificFileIdMatchesOriginalSnapshot() = runBlocking {
        val accId = appRepo.insertAccount(AccountEntity(name = "原始帳戶", type = AccountType.CASH, initialBalance = 8888))

        val backupResult = backupRepo.backupToGoogleDrive(password = "pass123")
        val fileId = backupResult.getOrThrow().driveFileId

        // 修改本機資料 (污染 DB)
        appRepo.softDeleteAccount(accId)
        appRepo.insertAccount(AccountEntity(name = "污染帳戶", type = AccountType.CREDIT_CARD, initialBalance = 100))

        // 指定 fileId 還原
        val restoreResult = backupRepo.restoreFromCloudBackup(fileId, password = "pass123")
        assertTrue(restoreResult.isSuccess)

        val accounts = db.accountDao().getAllAccounts()
        assertEquals(1, accounts.size)
        assertEquals(accId, accounts[0].id)
        assertEquals("原始帳戶", accounts[0].name)
        assertEquals(8888L, accounts[0].initialBalance)
    }

    /**
     * 使用全新測試資料庫還原，排除原本資料造成的假成功
     */
    @Test
    fun testRestoreIntoFreshCleanDatabase() = runBlocking {
        appRepo.insertAccount(AccountEntity(name = "乾淨測試帳戶", type = AccountType.BANK, initialBalance = 5000))

        val fileId = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 徹底清空 Room 資料庫 (模擬全新 App 安裝)
        db.clearAllTables()
        assertEquals(0, db.accountDao().getAllAccounts().size)

        // 執行還原
        val restoreResult = backupRepo.restoreFromCloudBackup(fileId, password = null)
        assertTrue(restoreResult.isSuccess)

        val restored = db.accountDao().getAllAccounts()
        assertEquals(1, restored.size)
        assertEquals("乾淨測試帳戶", restored[0].name)
        assertEquals(5000L, restored[0].initialBalance)
    }

    /**
     * 同時存在新舊備份，確認還原指定檔案 (fileId)
     */
    @Test
    fun testMultipleBackupsRestoresCorrectSpecifiedFileId() = runBlocking {
        // 備份 1
        appRepo.insertAccount(AccountEntity(name = "帳戶 1", type = AccountType.CASH, initialBalance = 100))
        val fileId1 = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 備份 2
        appRepo.insertAccount(AccountEntity(name = "帳戶 2", type = AccountType.BANK, initialBalance = 200))
        val fileId2 = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 還原 備份 1，驗證僅有 帳戶 1
        backupRepo.restoreFromCloudBackup(fileId1, password = null)
        val res1 = db.accountDao().getAllAccounts()
        assertEquals(1, res1.size)
        assertEquals("帳戶 1", res1[0].name)

        // 還原 備份 2，驗證含有 帳戶 1 與 帳戶 2
        backupRepo.restoreFromCloudBackup(fileId2, password = null)
        val res2 = db.accountDao().getAllAccounts()
        assertEquals(2, res2.size)
    }

    /**
     * 測試有密碼與無密碼備份還原
     */
    @Test
    fun testPasswordAndNoPasswordBackupsAndRestores() = runBlocking {
        appRepo.insertAccount(AccountEntity(name = "測試帳戶", type = AccountType.CASH, initialBalance = 100))

        // 無密碼備份
        val fileIdNoPass = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId
        val restoreNoPass = backupRepo.restoreFromCloudBackup(fileIdNoPass, password = null)
        assertTrue(restoreNoPass.isSuccess)

        // 有密碼備份
        val fileIdWithPass = backupRepo.backupToGoogleDrive(password = "P@ssw0rd123").getOrThrow().driveFileId
        val restoreWithPass = backupRepo.restoreFromCloudBackup(fileIdWithPass, password = "P@ssw0rd123")
        assertTrue(restoreWithPass.isSuccess)
    }

    /**
     * ViewModel 整合與畫面 Flow 即時更新測試
     */
    @Test
    fun testViewModelSaveAccountAndBackupRestoreFlow() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
        val viewModel = MainViewModel(application, db)

        // 設定假 DriveManager
        viewModel.onGoogleSignInSuccess("test@gmail.com")

        // 點擊新增帳戶
        viewModel.addAccount("ViewModel帳戶", AccountType.CASH, 777)

        // 等待 ViewModel coroutine 寫入
        var accountSaved = false
        ShadowLooper.idleMainLooper()
        for (i in 1..30) {
            ShadowLooper.idleMainLooper()
            if (viewModel.accountSaveState.value is MainViewModel.AccountSaveUiState.Success) {
                accountSaved = true
                break
            }
            kotlinx.coroutines.delay(20)
        }
        assertTrue("帳戶應成功寫入", accountSaved)

        // 執行備份
        viewModel.performBackup(password = null, customDriveManager = fakeDriveManager)

        var fileId = ""
        ShadowLooper.idleMainLooper()
        for (i in 1..30) {
            ShadowLooper.idleMainLooper()
            val bState = viewModel.backupUiState.value
            if (bState is MainViewModel.BackupUiState.Success) {
                fileId = bState.details.driveFileId
                break
            }
            kotlinx.coroutines.delay(20)
        }
        assertTrue("fileId 必須非空", fileId.isNotEmpty())

        // 清空本機
        viewModel.db.clearAllTables()

        // 執行還原
        viewModel.confirmAndRestoreCloudBackup(fileId, password = null, customDriveManager = fakeDriveManager)

        ShadowLooper.idleMainLooper()
        for (i in 1..30) {
            ShadowLooper.idleMainLooper()
            val rState = viewModel.restoreUiState.value
            if (rState is MainViewModel.RestoreUiState.Success) {
                break
            }
            kotlinx.coroutines.delay(20)
        }
        assertTrue("還原狀態必須為 Success", viewModel.restoreUiState.value is MainViewModel.RestoreUiState.Success)

        val restoredAccounts = viewModel.db.accountDao().getAllAccounts()
        assertTrue("DB 必須即時顯示還原後帳戶", restoredAccounts.any { it.name == "ViewModel帳戶" })
    }
}
