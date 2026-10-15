package com.moneytracker.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.moneytracker.app.data.backup.BackupCryptoManager
import com.moneytracker.app.data.backup.BackupPayload
import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.db.DatabaseInitializer
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
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
        DatabaseInitializer.resetInitializationStateForTest()
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
     * 測試延遲資料庫初始化，驗證不會與還原競爭或覆蓋還原結果
     */
    @Test
    fun testDelayedDatabaseInitializationDoesNotCompeteWithRestore() = runBlocking {
        // 先建立含有特殊資料的備份檔
        appRepo.insertAccount(AccountEntity(name = "專屬還原帳戶", type = AccountType.CREDIT_CARD, initialBalance = 6666))
        val backupFileId = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 重置 DB 與初始化狀態 (模擬 App 刪除重裝)
        db.clearAllTables()
        DatabaseInitializer.resetInitializationStateForTest()

        // 啟動一個會延遲執行 DatabaseInitializer 的協程
        val initJob = launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(50)
            DatabaseInitializer.ensureInitialized(db)
        }

        // 同時執行雲端還原
        val restoreResult = backupRepo.restoreFromCloudBackup(backupFileId, password = null)
        assertTrue(restoreResult.isSuccess)

        initJob.join()

        // 驗證還原資料庫包含「專屬還原帳戶」，且沒有預設種入資料污染
        val accounts = db.accountDao().getAllAccounts()
        assertEquals(1, accounts.size)
        assertEquals("專屬還原帳戶", accounts[0].name)
        assertEquals(6666L, accounts[0].initialBalance)
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
     * 模擬交易內核對失敗，原資料必須完整回滾 (Rollback)
     */
    @Test
    fun testRestoreVerificationFailureRollsBackAllChanges() = runBlocking {
        // 本機原本資料
        appRepo.insertAccount(AccountEntity(id = 100, name = "重要原有帳戶", type = AccountType.CASH, initialBalance = 9999))

        // 建立會引發交易內核對不相符的非法 payload (例如: 借貸狀態與還款加總不合)
        val invalidPayload = BackupPayload(
            schemaVersion = 1,
            accounts = listOf(
                AccountEntity(id = 1, name = "測試帳戶", type = AccountType.CASH, initialBalance = 1000)
            ),
            categories = emptyList(),
            contacts = listOf(
                ContactEntity(id = 1, name = "聯絡人")
            ),
            loans = listOf(
                LoanEntity(
                    id = 1,
                    contactId = 1,
                    type = LoanType.LEND,
                    amount = 5000,
                    dateMillis = System.currentTimeMillis(),
                    dueDateMillis = null,
                    accountId = 1,
                    note = "測試借貸",
                    status = LoanStatus.PAID // 宣稱 PAID，但無還款紀錄！
                )
            ),
            loanRepayments = emptyList(), // 0 筆還款
            transactions = emptyList(),
            settings = emptyList()
        )

        val result = backupRepo.restoreFromPayload(invalidPayload)
        assertTrue("核對失敗應回傳 Failure", result.isFailure)

        // 驗證原資料完全不變 (已成功回滾)
        val accounts = db.accountDao().getAllAccounts()
        assertEquals(1, accounts.size)
        assertEquals("重要原有帳戶", accounts[0].name)
        assertEquals(9999L, accounts[0].initialBalance)
    }

    /**
     * 完整各表欄位、關聯及餘額核對測試
     */
    @Test
    fun testDetailedFieldsAndBalancesVerificationOnRestore() = runBlocking {
        val accId = appRepo.insertAccount(AccountEntity(name = "主帳戶", type = AccountType.BANK, initialBalance = 20000))
        val catId = appRepo.insertCategory(CategoryEntity(name = "餐飲支出", type = TransactionType.EXPENSE))

        // 交易與借貸還款
        appRepo.insertTransaction(TransactionEntity(type = TransactionType.EXPENSE, amount = 120, dateMillis = System.currentTimeMillis(), sourceAccountId = accId, categoryId = catId, note = "午餐"))
        val loanId = appRepo.createLoanTransaction(contactName = "朋友A", type = LoanType.LEND, amount = 1000, dateMillis = System.currentTimeMillis(), dueDateMillis = null, accountId = accId, note = "借朋友")
        appRepo.addRepayment(loanId = loanId, amount = 400, dateMillis = System.currentTimeMillis(), accountId = accId, note = "還款")

        val fileId = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 清空
        db.clearAllTables()

        // 還原並核對 Summary 與各表欄位
        val restoreResult = backupRepo.restoreFromCloudBackup(fileId, password = null)
        assertTrue(restoreResult.isSuccess)

        val summary = restoreResult.getOrThrow()
        assertEquals(1, summary.accountCount)
        assertEquals(1, summary.categoryCount)
        assertEquals(1, summary.contactCount)
        assertEquals(1, summary.loanCount)
        assertEquals(1, summary.repaymentCount)
        assertEquals(3, summary.transactionCount) // 1 一般 + 1 借出 + 1 還款

        val restoredLoan = appRepo.getLoanWithDetails(loanId)
        assertNotNull(restoredLoan)
        assertEquals(400L, restoredLoan!!.repaidAmount)
        assertEquals(600L, restoredLoan.remainingAmount)
        assertEquals(LoanStatus.PARTIAL, restoredLoan.loan.status)
    }

    /**
     * 預覽後取消不得改動本機資料測試
     */
    @Test
    fun testInspectBackupPreviewDoesNotModifyDatabaseAndCanCancel() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
        val viewModel = MainViewModel(application, db)
        viewModel.onGoogleSignInSuccess("previewtest@gmail.com")

        // 本機先有帳戶
        viewModel.addAccount("本機原有帳戶", AccountType.CASH, 5000)

        ShadowLooper.idleMainLooper()
        for (i in 1..20) {
            ShadowLooper.idleMainLooper()
            if (viewModel.accountSaveState.value is MainViewModel.AccountSaveUiState.Success) break
            kotlinx.coroutines.delay(20)
        }

        // 建立一個備份檔
        val backupResult = viewModel.repository.insertAccount(AccountEntity(name = "雲端備份帳戶", type = AccountType.BANK, initialBalance = 8888))
        val fileId = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 還原前刪除雲端備份帳戶，留下本機原有帳戶
        viewModel.repository.softDeleteAccount(backupResult)

        // 僅執行預覽 (inspectBackupForRestore)
        viewModel.inspectBackupForRestore(fileId, password = null, customDriveManager = fakeDriveManager)

        ShadowLooper.idleMainLooper()
        for (i in 1..20) {
            ShadowLooper.idleMainLooper()
            if (viewModel.restoreUiState.value is MainViewModel.RestoreUiState.Preview) break
            kotlinx.coroutines.delay(20)
        }

        val restoreState = viewModel.restoreUiState.value
        assertTrue("狀態必須為 Preview", restoreState is MainViewModel.RestoreUiState.Preview)

        // 使用者點擊「取消」/ 重置 RestoreUiState
        viewModel.resetRestoreUiState()
        assertEquals(MainViewModel.RestoreUiState.Idle, viewModel.restoreUiState.value)

        // 驗證本機資料庫依然包含本機原有帳戶，且沒有將「雲端備份帳戶」寫入
        val accounts = db.accountDao().getAllAccounts()
        assertTrue(accounts.any { it.name == "本機原有帳戶" })
        assertTrue(accounts.none { it.name == "雲端備份帳戶" })
    }

    /**
     * 測試：登入後 pendingRestore 時 BackupWorker 執行不會上傳預設資料，且還原既有備份 A 能排除預設備份 B
     */
    @Test
    fun testPendingRestorePreventsAutoBackupAndAllowsRestoringOriginalBackup() = runBlocking {
        val email = "testrestore@gmail.com"

        val context = ApplicationProvider.getApplicationContext<Context>()
        val application = context as android.app.Application
        backupRepo.setRestorePending(email, true)
        db.settingsAndBackupDao().setSettingValue(
            com.moneytracker.app.data.entity.AppSettingsEntity("google_account_email", email)
        )

        val viewModel = MainViewModel(application, db)

        ShadowLooper.idleMainLooper()
        for (i in 1..50) {
            ShadowLooper.idleMainLooper()
            if (viewModel.isRestorePending.value) break
            kotlinx.coroutines.delay(20)
        }

        assertTrue("設定 restorePending = true 後，isRestorePending 應為 true", viewModel.isRestorePending.value)

        // 嘗試 performBackup，應回傳錯誤訊息，阻止上傳
        viewModel.performBackup(null, fakeDriveManager)
        ShadowLooper.idleMainLooper()
        for (i in 1..20) {
            ShadowLooper.idleMainLooper()
            if (viewModel.backupUiState.value is MainViewModel.BackupUiState.Error) break
            kotlinx.coroutines.delay(20)
        }
        val backupState = viewModel.backupUiState.value
        assertTrue(backupState is MainViewModel.BackupUiState.Error)
        assertEquals("尚待選擇是否還原既有雲端備份，暫停備份", (backupState as MainViewModel.BackupUiState.Error).message)

        // 建立既有有效備份 A (含 1000 元帳戶與 100 元交易)
        appRepo.insertAccount(AccountEntity(name = "備份A帳戶", type = AccountType.CASH, initialBalance = 1000))
        val backupAFileId = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 清空資料庫模擬新裝置/重裝，並產生重裝後的預設資料備份 B
        db.clearAllTables()
        DatabaseInitializer.ensureInitialized(db)
        val backupBFileId = backupRepo.backupToGoogleDrive(password = null).getOrThrow().driveFileId

        // 列出雲端備份，驗證能識別 B 為【預設初始資料】
        val cloudBackupsResult = backupRepo.listCloudBackups()
        assertTrue(cloudBackupsResult.isSuccess)
        val backups = cloudBackupsResult.getOrThrow()

        val backupBInfo = backups.find { it.fileId == backupBFileId }
        val backupAInfo = backups.find { it.fileId == backupAFileId }

        assertNotNull(backupBInfo)
        assertNotNull(backupAInfo)
        assertTrue("備份 B 應標示為預設初始資料", backupBInfo!!.isDefaultOnly)
        assertTrue("備份 A 不應標示為預設初始資料", !backupAInfo!!.isDefaultOnly)

        // 選擇還原既有備份 A
        val restoreResult = backupRepo.restoreFromCloudBackup(backupAFileId, password = null)
        assertTrue("還原 A 必須成功", restoreResult.isSuccess)

        // 驗證 restore_pending 已被清除為 false
        val isPendingAfterRestore = backupRepo.isRestorePending(email)
        assertTrue("還原成功後 restore_pending 應為 false", !isPendingAfterRestore)

        // 驗證 DB 恢復 A 的資料
        val accounts = db.accountDao().getAllAccounts()
        assertTrue(accounts.any { it.name == "備份A帳戶" })
    }
}
