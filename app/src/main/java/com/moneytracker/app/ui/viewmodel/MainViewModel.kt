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
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = repository.getExpenseSumBetweenFlow(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), 0L)

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = repository.getAllAccountsWithBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), emptyList())

    val totalAccountBalance: StateFlow<Long> = accountsWithBalance.combine(accountsWithBalance) { list, _ ->
        list.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), 0L)

    val outstandingLendTotal: StateFlow<Long> = repository.getOutstandingLendTotalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), 0L)

    val outstandingBorrowTotal: StateFlow<Long> = repository.getOutstandingBorrowTotalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), 0L)

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), emptyList())

    val loansWithDetails: StateFlow<List<LoanWithDetails>> = repository.getAllLoansWithDetailsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), emptyList())

    val contactSummaries: StateFlow<List<ContactSummary>> = repository.getContactSummariesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubsubscribed(5000), emptyList())

    // Settings / Backup State
    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<CloudBackupInfo>>(emptyList())
    val cloudBackups: StateFlow<List<CloudBackupInfo>> = _cloudBackups.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        loadSettings()
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _googleAccountEmail.value = db.settingsAndBackupDao().getSettingValue("google_account_email")
            val lastTimeStr = db.settingsAndBackupDao().getSettingValue("last_backup_time")
            _lastBackupTime.value = lastTimeStr?.toLongOrNull()
        }
    }

    fun setGoogleAccount(email: String?) {
        viewModelScope.launch {
            if (email != null) {
                db.settingsAndBackupDao().setSettingValue(AppSettingsEntity("google_account_email", email))
            } else {
                db.settingsAndBackupDao().setSettingValue(AppSettingsEntity("google_account_email", ""))
            }
            _googleAccountEmail.value = email
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

    fun performBackup(password: String?) {
        viewModelScope.launch {
            val email = _googleAccountEmail.value
            if (email.isNull_or_empty()) {
                _uiMessage.value = "請先連動 Google 帳號"
                return@launch
            }
            val driveManager = GoogleDriveManager(getApplication(), email)
            val backupRepo = BackupRestoreRepository(db, driveManager)

            val result = backupRepo.backupToGoogleDrive(password)
            if (result.isSuccess) {
                val metadata = result.getOrNull()
                _lastBackupTime.value = metadata?.backupTime
                _uiMessage.value = "Google Drive 備份成功！"
            } else {
                _uiMessage.value = "備份失敗: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun fetchCloudBackups() {
        viewModelScope.launch {
            val email = _googleAccountEmail.value
            if (email.isNull_or_empty()) return@launch

            val driveManager = GoogleDriveManager(getApplication(), email)
            val backupRepo = BackupRestoreRepository(db, driveManager)

            val result = backupRepo.listCloudBackups()
            if (result.isSuccess) {
                _cloudBackups.value = result.getOrDefault(emptyList())
            } else {
                _uiMessage.value = "取得雲端備份失敗: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreCloudBackup(fileId: String, password: String?) {
        viewModelScope.launch {
            val email = _googleAccountEmail.value
            if (email.isNull_or_empty()) return@launch

            val driveManager = GoogleDriveManager(getApplication(), email)
            val backupRepo = BackupRestoreRepository(db, driveManager)

            val result = backupRepo.restoreFromCloudBackup(fileId, password)
            if (result.isSuccess) {
                _uiMessage.value = "資料還原成功！"
            } else {
                _uiMessage.value = "還原失敗: ${result.exceptionOrNull()?.message}"
            }
        }
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

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
