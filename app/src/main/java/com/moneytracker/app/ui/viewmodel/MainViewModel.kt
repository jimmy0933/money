package com.moneytracker.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.backup.CloudBackupInfo
import com.moneytracker.app.data.backup.GoogleDriveManager
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.data.repository.AccountWithBalance
import com.moneytracker.app.data.repository.AppRepository
import com.moneytracker.app.data.repository.ContactSummary
import com.moneytracker.app.data.repository.LoanWithDetails
import com.moneytracker.app.worker.BackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val db = AppDatabase.getDatabase(application)
    val repository = AppRepository(db)

    // Current Month range
    private val calendar = Calendar.getInstance()
    private val startOfMonth: Long
    private val endOfMonth: Long

    init {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        endOfMonth = calendar.timeInMillis
    }

    // Home Flow States
    val monthlyIncome: StateFlow<Long> = repository.getIncomeSumBetweenFlow(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = repository.getExpenseSumBetweenFlow(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = repository.getAllAccountsWithBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAccountBalance: StateFlow<Long> = accountsWithBalance.combine(accountsWithBalance) { list, _ ->
        list.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val outstandingLendTotal: StateFlow<Long> = repository.getOutstandingLendTotalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val outstandingBorrowTotal: StateFlow<Long> = repository.getOutstandingBorrowTotalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loansWithDetails: StateFlow<List<LoanWithDetails>> = repository.getAllLoansWithDetailsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactSummaries: StateFlow<List<ContactSummary>> = repository.getContactSummariesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    enum class GoogleAuthState {
        LOGGED_OUT,
        LOGGING_IN,
        LOGGED_IN,
        FAILED
    }

    data class RestoreSummary(
        val accountCount: Int,
        val categoryCount: Int,
        val transactionCount: Int,
        val contactCount: Int,
        val loanCount: Int,
        val repaymentCount: Int
    )

    sealed class CloudBackupListState {
        object Idle : CloudBackupListState()
        object Loading : CloudBackupListState()
        data class Success(val backups: List<CloudBackupInfo>) : CloudBackupListState()
        object Empty : CloudBackupListState()
        data class Error(val message: String) : CloudBackupListState()
    }

    sealed class RestoreUiState {
        object Idle : RestoreUiState()
        object Downloading : RestoreUiState()
        object DecryptingAndValidating : RestoreUiState()
        object Restoring : RestoreUiState()
        data class Success(val summary: RestoreSummary) : RestoreUiState()
        data class Error(val message: String) : RestoreUiState()
    }

    sealed class BackupUiState {
        object Idle : BackupUiState()
        object BackingUp : BackupUiState()
        object Success : BackupUiState()
        data class Error(val message: String) : BackupUiState()
    }

    // Settings / Backup State
    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()

    private val _googleAuthState = MutableStateFlow(GoogleAuthState.LOGGED_OUT)
    val googleAuthState: StateFlow<GoogleAuthState> = _googleAuthState.asStateFlow()

    private val _googleAuthErrorMessage = MutableStateFlow<String?>(null)
    val googleAuthErrorMessage: StateFlow<String?> = _googleAuthErrorMessage.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<CloudBackupInfo>>(emptyList())
    val cloudBackups: StateFlow<List<CloudBackupInfo>> = _cloudBackups.asStateFlow()

    private val _cloudBackupListState = MutableStateFlow<CloudBackupListState>(CloudBackupListState.Idle)
    val cloudBackupListState: StateFlow<CloudBackupListState> = _cloudBackupListState.asStateFlow()

    private val _restoreUiState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val restoreUiState: StateFlow<RestoreUiState> = _restoreUiState.asStateFlow()

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        loadSettings()
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun setUiMessage(message: String) {
        _uiMessage.value = message
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val email = db.settingsAndBackupDao().getSettingValue("google_account_email")
            val cleanEmail = if (email.isNullOrBlank()) null else email
            _googleAccountEmail.value = cleanEmail

            if (cleanEmail != null) {
                _googleAuthState.value = GoogleAuthState.LOGGED_IN
                BackupWorker.schedulePeriodicBackup(getApplication())
            } else {
                _googleAuthState.value = GoogleAuthState.LOGGED_OUT
            }

            val lastTimeStr = db.settingsAndBackupDao().getSettingValue("last_backup_time")
            _lastBackupTime.value = lastTimeStr?.toLongOrNull()
        }
    }

    fun startGoogleSignIn() {
        _googleAuthState.value = GoogleAuthState.LOGGING_IN
        _googleAuthErrorMessage.value = null
    }

    fun onGoogleSignInSuccess(email: String) {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            db.settingsAndBackupDao().setSettingValue(AppSettingsEntity("google_account_email", cleanEmail))
            BackupWorker.schedulePeriodicBackup(getApplication())
            _googleAccountEmail.value = cleanEmail
            _googleAuthState.value = GoogleAuthState.LOGGED_IN
            _googleAuthErrorMessage.value = null
            _uiMessage.value = "已成功連結 Google 帳號：$cleanEmail"
        }
    }

    fun onGoogleSignInFailure(statusCode: Int?, rawErrorMsg: String?) {
        _googleAuthState.value = GoogleAuthState.FAILED
        val userMsg = when (statusCode) {
            10 -> "Google 登入失敗 (錯誤碼: 10 DEVELOPER_ERROR)：請確認 Google Cloud Console 上此 App 包名 (com.moneytracker.app) 與 SHA-1 簽章指紋配置正確。"
            12500 -> "Google 登入失敗 (錯誤碼: 12500)：請確認裝置已安裝且更新 Google Play 服務，並已啟用 OAuth。"
            12501, 16 -> "登入已取消"
            else -> "Google 登入失敗 (${statusCode?.let { "錯誤碼: $it" } ?: "未知錯誤"})：${rawErrorMsg ?: "存取授權失敗"}"
        }
        _googleAuthErrorMessage.value = userMsg
        _uiMessage.value = userMsg
    }

    fun signOutGoogleAccount() {
        viewModelScope.launch {
            db.settingsAndBackupDao().setSettingValue(AppSettingsEntity("google_account_email", ""))
            BackupWorker.cancelPeriodicBackup(getApplication())
            _googleAccountEmail.value = null
            _cloudBackups.value = emptyList()
            _cloudBackupListState.value = CloudBackupListState.Idle
            _lastBackupTime.value = null
            _googleAuthState.value = GoogleAuthState.LOGGED_OUT
            _googleAuthErrorMessage.value = null
            _uiMessage.value = "已登出 Google 帳號，並清空雲端備份歷史對照"
        }
    }

    fun setGoogleAccount(email: String?) {
        if (email.isNullOrBlank()) {
            signOutGoogleAccount()
        } else {
            onGoogleSignInSuccess(email)
        }
    }

    // Actions
    fun addTransaction(
        type: TransactionType,
        amount: Long,
        dateMillis: Long,
        sourceAccountId: Long,
        targetAccountId: Long?,
        categoryId: Long?,
        note: String
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    dateMillis = dateMillis,
                    sourceAccountId = sourceAccountId,
                    targetAccountId = targetAccountId,
                    categoryId = categoryId,
                    note = note
                )
            )
            _uiMessage.value = "新增交易成功"
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.softDeleteTransaction(id)
            _uiMessage.value = "已刪除交易"
        }
    }

    fun addLoan(
        contactName: String,
        type: LoanType,
        amount: Long,
        dateMillis: Long,
        dueDateMillis: Long?,
        accountId: Long,
        note: String
    ) {
        viewModelScope.launch {
            repository.createLoanTransaction(
                contactName = contactName,
                type = type,
                amount = amount,
                dateMillis = dateMillis,
                dueDateMillis = dueDateMillis,
                accountId = accountId,
                note = note
            )
            _uiMessage.value = "新增借貸紀錄成功"
        }
    }

    fun addLoanRepayment(
        loanId: Long,
        amount: Long,
        dateMillis: Long,
        accountId: Long,
        note: String
    ) {
        viewModelScope.launch {
            val result = repository.addRepayment(loanId, amount, dateMillis, accountId, note)
            if (result.isSuccess) {
                _uiMessage.value = "新增還款成功"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "還款失敗"
            }
        }
    }

    fun deleteLoan(loanId: Long) {
        viewModelScope.launch {
            repository.deleteLoan(loanId)
            _uiMessage.value = "已刪除借貸紀錄"
        }
    }

    fun deleteLoanRepayment(repaymentId: Long) {
        viewModelScope.launch {
            repository.deleteRepayment(repaymentId)
            _uiMessage.value = "已刪除還款紀錄"
        }
    }

    fun performBackup(password: String?) {
        if (_isOperationInProgress.value) return
        viewModelScope.launch {
            val email = _googleAccountEmail.value
            if (email.isNullOrEmpty()) {
                _uiMessage.value = "請先連動 Google 帳號"
                _backupUiState.value = BackupUiState.Error("請先連動 Google 帳號")
                return@launch
            }

            _isOperationInProgress.value = true
            _backupUiState.value = BackupUiState.BackingUp

            try {
                val driveManager = GoogleDriveManager(getApplication(), email)
                val backupRepo = BackupRestoreRepository(db, driveManager)

                val result = backupRepo.backupToGoogleDrive(password)
                if (result.isSuccess) {
                    val metadata = result.getOrNull()
                    _lastBackupTime.value = metadata?.backupTime
                    _backupUiState.value = BackupUiState.Success
                    _uiMessage.value = "Google Drive 備份成功！"
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "備份失敗"
                    _backupUiState.value = BackupUiState.Error(errorMsg)
                    _uiMessage.value = "備份失敗: $errorMsg"
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "備份過程發生錯誤"
                _backupUiState.value = BackupUiState.Error(errorMsg)
                _uiMessage.value = "備份失敗: $errorMsg"
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun fetchCloudBackups() {
        viewModelScope.launch {
            val email = _googleAccountEmail.value
            if (email.isNullOrEmpty()) {
                _cloudBackupListState.value = CloudBackupListState.Error("請先連結 Google 帳號")
                return@launch
            }

            _cloudBackupListState.value = CloudBackupListState.Loading
            _cloudBackups.value = emptyList()

            try {
                val driveManager = GoogleDriveManager(getApplication(), email)
                val backupRepo = BackupRestoreRepository(db, driveManager)

                val result = backupRepo.listCloudBackups()
                if (result.isSuccess) {
                    val backups = result.getOrDefault(emptyList())
                    _cloudBackups.value = backups
                    if (backups.isEmpty()) {
                        _cloudBackupListState.value = CloudBackupListState.Empty
                    } else {
                        _cloudBackupListState.value = CloudBackupListState.Success(backups)
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "無法取得雲端備份列表"
                    _cloudBackupListState.value = CloudBackupListState.Error(errorMsg)
                    _uiMessage.value = "取得雲端備份失敗: $errorMsg"
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "發生未知錯誤"
                _cloudBackupListState.value = CloudBackupListState.Error(errorMsg)
                _uiMessage.value = "取得雲端備份失敗: $errorMsg"
            }
        }
    }

    fun restoreCloudBackup(fileId: String, password: String?) {
        if (_isOperationInProgress.value) return
        viewModelScope.launch {
            val email = _googleAccountEmail.value
            if (email.isNullOrEmpty()) {
                _restoreUiState.value = RestoreUiState.Error("請先連動 Google 帳號")
                return@launch
            }

            _isOperationInProgress.value = true
            try {
                val driveManager = GoogleDriveManager(getApplication(), email)
                val backupRepo = BackupRestoreRepository(db, driveManager)

                // 階段 1: 下載
                _restoreUiState.value = RestoreUiState.Downloading
                val downloadedBytes = try {
                    driveManager.downloadBackupFile(fileId)
                } catch (e: Exception) {
                    val errorMsg = "下載備份檔案失敗: ${e.message ?: "網路或存取權限異常"}"
                    _restoreUiState.value = RestoreUiState.Error(errorMsg)
                    _uiMessage.value = errorMsg
                    return@launch
                }

                // 階段 2: 解密與格式/關聯完整性驗證
                _restoreUiState.value = RestoreUiState.DecryptingAndValidating
                val payload = try {
                    com.moneytracker.app.data.backup.BackupCryptoManager.decryptAndDeserializePayload(
                        downloadedBytes,
                        password
                    )
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "解密或驗證資料失敗"
                    _restoreUiState.value = RestoreUiState.Error(errorMsg)
                    _uiMessage.value = "還原失敗: $errorMsg"
                    return@launch
                }

                // 階段 3: 資料庫寫入
                _restoreUiState.value = RestoreUiState.Restoring
                val restoreResult = backupRepo.restoreFromPayload(payload)

                // 階段 4: 成果處理
                if (restoreResult.isSuccess) {
                    loadSettings()
                    val summary = RestoreSummary(
                        accountCount = payload.accounts.size,
                        categoryCount = payload.categories.size,
                        transactionCount = payload.transactions.size,
                        contactCount = payload.contacts.size,
                        loanCount = payload.loans.size,
                        repaymentCount = payload.loanRepayments.size
                    )
                    _restoreUiState.value = RestoreUiState.Success(summary)
                    _uiMessage.value = "資料還原成功！"
                } else {
                    val errorMsg = restoreResult.exceptionOrNull()?.message ?: "寫入資料庫失敗"
                    _restoreUiState.value = RestoreUiState.Error(errorMsg)
                    _uiMessage.value = "還原失敗: $errorMsg"
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "還原過程發生未知錯誤"
                _restoreUiState.value = RestoreUiState.Error(errorMsg)
                _uiMessage.value = "還原失敗: $errorMsg"
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun resetRestoreUiState() {
        _restoreUiState.value = RestoreUiState.Idle
    }

    fun resetBackupUiState() {
        _backupUiState.value = BackupUiState.Idle
    }

    fun addAccount(name: String, type: com.moneytracker.app.data.model.AccountType, initialBalance: Long) {
        viewModelScope.launch {
            repository.insertAccount(AccountEntity(name = name, type = type, initialBalance = initialBalance))
            _uiMessage.value = "新增帳戶成功"
        }
    }

    fun addCategory(name: String, type: TransactionType) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, type = type))
            _uiMessage.value = "新增分類成功"
        }
    }

}
