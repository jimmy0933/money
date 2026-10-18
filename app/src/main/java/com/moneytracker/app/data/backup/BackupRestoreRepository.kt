package com.moneytracker.app.data.backup

import androidx.room.withTransaction
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.db.DatabaseInitializer
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.BackupMetadataEntity
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.repository.DataOperationCoordinator

data class CloudBackupInfo(
    val fileId: String,
    val fileName: String,
    val createdTimeMillis: Long,
    val sizeBytes: Long,
    val isDefaultOnly: Boolean = false,
    val summaryText: String = ""
)

data class BackupSuccessDetails(
    val backupTime: Long,
    val driveFileId: String,
    val checksum: String,
    val sizeBytes: Long,
    val accountCount: Int,
    val categoryCount: Int,
    val transactionCount: Int,
    val contactCount: Int,
    val loanCount: Int,
    val repaymentCount: Int
)

data class VerifiedRestoreSummary(
    val accountCount: Int,
    val categoryCount: Int,
    val transactionCount: Int,
    val contactCount: Int,
    val loanCount: Int,
    val repaymentCount: Int
)

class BackupRestoreRepository(
    private val db: AppDatabase,
    private val driveManager: DriveManager? = null
) {

    suspend fun isRestorePending(accountEmail: String?): Boolean {
        if (accountEmail.isNullOrBlank()) return false
        val key = "restore_pending_${accountEmail.trim().lowercase()}"
        return db.settingsAndBackupDao().getSettingValue(key) == "true"
    }

    suspend fun setRestorePending(accountEmail: String, isPending: Boolean) {
        if (accountEmail.isBlank()) return
        val key = "restore_pending_${accountEmail.trim().lowercase()}"
        db.settingsAndBackupDao().setSettingValue(
            AppSettingsEntity(key, if (isPending) "true" else "false")
        )
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun createLocalBackupBytesInternal(password: String?): Pair<BackupPayload, ByteArray> {
        val payload = BackupCryptoManager.createBackupPayload(db)
        val bytes = BackupCryptoManager.serializeAndEncryptPayload(payload, password)
        return Pair(payload, bytes)
    }

    suspend fun createLocalBackupBytes(password: String?): Pair<BackupPayload, ByteArray> {
        DatabaseInitializer.ensureInitialized(db)
        return DataOperationCoordinator.runBackupOperation {
            createLocalBackupBytesInternal(password)
        }
    }

    suspend fun restoreFromPayload(payload: BackupPayload): Result<VerifiedRestoreSummary> {
        DatabaseInitializer.ensureInitialized(db)
        return DataOperationCoordinator.runRestoreOperation {
            try {
                // 1. 資料合法性與完整性關聯驗證
                BackupPayloadValidator.validate(payload)

                // 2. 交易式寫入 Room DB 並在交易內完成完整性與業務欄位/餘額/關聯核對
                var verifiedSummary: VerifiedRestoreSummary? = null

                db.withTransaction {
                    val currentGoogleEmail = db.settingsAndBackupDao().getSettingValue("google_account_email")

                    val database = db.openHelper.writableDatabase
                    database.execSQL("DELETE FROM transactions")
                    database.execSQL("DELETE FROM loan_repayments")
                    database.execSQL("DELETE FROM loans")
                    database.execSQL("DELETE FROM contacts")
                    database.execSQL("DELETE FROM categories")
                    database.execSQL("DELETE FROM accounts")

                    payload.accounts.forEach { db.accountDao().insertAccount(it) }
                    payload.categories.forEach { db.categoryDao().insertCategory(it) }
                    payload.contacts.forEach { db.contactDao().insertContact(it) }
                    payload.loans.forEach { db.loanDao().insertLoan(it) }
                    payload.loanRepayments.forEach { db.loanRepaymentDao().insertRepayment(it) }
                    payload.transactions.forEach { db.transactionDao().insertTransaction(it) }

                    payload.settings
                        .filter { it.key != "google_account_email" }
                        .forEach { db.settingsAndBackupDao().setSettingValue(it) }

                    db.settingsAndBackupDao().setSettingValue(
                        AppSettingsEntity("db_initialized", "true")
                    )

                    if (!currentGoogleEmail.isNullOrBlank()) {
                        val cleanEmail = currentGoogleEmail.trim()
                        db.settingsAndBackupDao().setSettingValue(
                            AppSettingsEntity("google_account_email", cleanEmail)
                        )
                        // 還原成功後，清除該帳號的 restore_pending 狀態
                        db.settingsAndBackupDao().setSettingValue(
                            AppSettingsEntity("restore_pending_${cleanEmail.lowercase()}", "false")
                        )
                    }

                    // --- 交易內完整性與業務欄位核對 ---

                    // A. 帳戶表核對 (筆數、ID、名稱、類型、初始餘額、刪除標記、帳戶餘額可計算)
                    val dbAccounts = db.accountDao().getAllAccountsWithDeleted()
                    if (dbAccounts.size != payload.accounts.size) {
                        throw IllegalStateException("還原後帳戶筆數與備份不符 (預期: ${payload.accounts.size}, 實際: ${dbAccounts.size})")
                    }
                    for (expected in payload.accounts) {
                        val actual = db.accountDao().getAccountByIdWithDeleted(expected.id)
                            ?: throw IllegalStateException("還原後找不到帳戶 ID ${expected.id}")
                        if (actual.name != expected.name || actual.type != expected.type ||
                            actual.initialBalance != expected.initialBalance || actual.isDeleted != expected.isDeleted) {
                            throw IllegalStateException("還原後帳戶欄位與備份不符 (ID: ${expected.id})")
                        }
                        val income = if (expected.isDeleted) db.transactionDao().getTotalIncomeForAccountWithDeleted(expected.id) else db.transactionDao().getTotalIncomeForAccount(expected.id)
                        val expense = if (expected.isDeleted) db.transactionDao().getTotalExpenseForAccountWithDeleted(expected.id) else db.transactionDao().getTotalExpenseForAccount(expected.id)
                        val transferOut = if (expected.isDeleted) db.transactionDao().getTotalTransferOutForAccountWithDeleted(expected.id) else db.transactionDao().getTotalTransferOutForAccount(expected.id)
                        val transferIn = if (expected.isDeleted) db.transactionDao().getTotalTransferInForAccountWithDeleted(expected.id) else db.transactionDao().getTotalTransferInForAccount(expected.id)
                        val currentBal = expected.initialBalance + income - expense - transferOut + transferIn
                        if (currentBal.toString().isEmpty()) {
                            throw IllegalStateException("帳戶餘額計算異常 (ID: ${expected.id})")
                        }
                    }

                    // B. 收支分類表核對 (筆數、ID、名稱、類型、圖示、預設/刪除標記)
                    val dbCategories = db.categoryDao().getAllCategoriesWithDeleted()
                    if (dbCategories.size != payload.categories.size) {
                        throw IllegalStateException("還原後分類筆數與備份不符 (預期: ${payload.categories.size}, 實際: ${dbCategories.size})")
                    }
                    for (expected in payload.categories) {
                        val actual = db.categoryDao().getCategoryByIdWithDeleted(expected.id)
                            ?: throw IllegalStateException("還原後找不到分類 ID ${expected.id}")
                        if (actual.name != expected.name || actual.type != expected.type ||
                            actual.iconName != expected.iconName || actual.isDefault != expected.isDefault ||
                            actual.isDeleted != expected.isDeleted) {
                            throw IllegalStateException("還原後分類欄位與備份不符 (ID: ${expected.id})")
                        }
                    }

                    // C. 聯絡人表核對 (筆數、ID、姓名、電話、備註、刪除標記)
                    val dbContacts = db.contactDao().getAllContactsWithDeleted()
                    if (dbContacts.size != payload.contacts.size) {
                        throw IllegalStateException("還原後聯絡人筆數與備份不符 (預期: ${payload.contacts.size}, 實際: ${dbContacts.size})")
                    }
                    for (expected in payload.contacts) {
                        val actual = db.contactDao().getContactByIdWithDeleted(expected.id)
                            ?: throw IllegalStateException("還原後找不到聯絡人 ID ${expected.id}")
                        if (actual.name != expected.name || actual.phone != expected.phone ||
                            actual.note != expected.note || actual.isDeleted != expected.isDeleted) {
                            throw IllegalStateException("還原後聯絡人欄位與備份不符 (ID: ${expected.id})")
                        }
                    }

                    // D. 借貸紀錄表核對 (筆數、ID、關聯聯絡人、類型、金額、日期、到期日、關聯帳戶、狀態、備註、刪除標記)
                    val dbLoans = db.loanDao().getAllLoansWithDeleted()
                    if (dbLoans.size != payload.loans.size) {
                        throw IllegalStateException("還原後借貸紀錄筆數與備份不符 (預期: ${payload.loans.size}, 實際: ${dbLoans.size})")
                    }
                    for (expected in payload.loans) {
                        val actual = db.loanDao().getLoanByIdWithDeleted(expected.id)
                            ?: throw IllegalStateException("還原後找不到借貸 ID ${expected.id}")
                        if (actual.contactId != expected.contactId || actual.type != expected.type ||
                            actual.amount != expected.amount || actual.dateMillis != expected.dateMillis ||
                            actual.dueDateMillis != expected.dueDateMillis || actual.accountId != expected.accountId ||
                            actual.note != expected.note || actual.status != expected.status ||
                            actual.isDeleted != expected.isDeleted) {
                            throw IllegalStateException("還原後借貸紀錄欄位與備份不符 (ID: ${expected.id})")
                        }
                        if (db.contactDao().getContactByIdWithDeleted(expected.contactId) == null) {
                            throw IllegalStateException("還原後借貸之聯絡人 ID ${expected.contactId} 不存在")
                        }
                        if (db.accountDao().getAccountByIdWithDeleted(expected.accountId) == null) {
                            throw IllegalStateException("還原後借貸之帳戶 ID ${expected.accountId} 不存在")
                        }

                        // 驗證借貸狀態與還款加總一致性
                        // 正確區分已撤銷還款與隨借貸刪除的歷史資料。不可直接把所有已刪除還款視為有效，也不可取消所有完整性驗證。
                        val totalRepaid = if (!expected.isDeleted) {
                            db.loanRepaymentDao().getTotalRepaidForLoan(expected.id)
                        } else {
                            -1L
                        }

                        if (totalRepaid != -1L) {
                            val calculatedStatus = when {
                                totalRepaid >= expected.amount -> LoanStatus.PAID
                                totalRepaid > 0 -> LoanStatus.PARTIAL
                                else -> LoanStatus.UNPAID
                            }

                            if (expected.status != calculatedStatus) {
                                throw IllegalStateException("還原後借貸狀態與還款加總不相符 (ID: ${expected.id}, 預期: ${expected.status}, 計算: ${calculatedStatus.name}, 總金額: $totalRepaid)")
                            }
                        }
                    }

                    // E. 還款紀錄表核對 (筆數、ID、關聯借貸、金額、日期、關聯帳戶、備註、刪除標記)
                    val dbRepayments = db.loanRepaymentDao().getAllRepaymentsWithDeleted()
                    if (dbRepayments.size != payload.loanRepayments.size) {
                        throw IllegalStateException("還原後還款紀錄筆數與備份不符 (預期: ${payload.loanRepayments.size}, 實際: ${dbRepayments.size})")
                    }
                    for (expected in payload.loanRepayments) {
                        val actual = db.loanRepaymentDao().getRepaymentByIdWithDeleted(expected.id)
                            ?: throw IllegalStateException("還原後找不到還款 ID ${expected.id}")
                        if (actual.loanId != expected.loanId || actual.amount != expected.amount ||
                            actual.dateMillis != expected.dateMillis || actual.accountId != expected.accountId ||
                            actual.note != expected.note || actual.isDeleted != expected.isDeleted) {
                            throw IllegalStateException("還原後還款紀錄欄位與備份不符 (ID: ${expected.id})")
                        }
                        if (db.loanDao().getLoanByIdWithDeleted(expected.loanId) == null) {
                            throw IllegalStateException("還原後還款之借貸 ID ${expected.loanId} 不存在")
                        }
                        if (db.accountDao().getAccountByIdWithDeleted(expected.accountId) == null) {
                            throw IllegalStateException("還原後還款之帳戶 ID ${expected.accountId} 不存在")
                        }
                    }

                    // F. 交易紀錄表核對 (筆數、ID、類型、金額、日期、關聯分類、來源帳戶、目標帳戶、關聯借貸、關聯還款、刪除標記)
                    val dbTransactions = db.transactionDao().getAllTransactionsWithDeleted()
                    if (dbTransactions.size != payload.transactions.size) {
                        throw IllegalStateException("還原後交易紀錄筆數與備份不符 (預期: ${payload.transactions.size}, 實際: ${dbTransactions.size})")
                    }
                    for (expected in payload.transactions) {
                        val actual = db.transactionDao().getTransactionByIdWithDeleted(expected.id)
                            ?: throw IllegalStateException("還原後找不到交易 ID ${expected.id}")
                        if (actual.type != expected.type || actual.amount != expected.amount ||
                            actual.dateMillis != expected.dateMillis || actual.categoryId != expected.categoryId ||
                            actual.sourceAccountId != expected.sourceAccountId || actual.targetAccountId != expected.targetAccountId ||
                            actual.note != expected.note || actual.loanId != expected.loanId ||
                            actual.repaymentId != expected.repaymentId || actual.isDeleted != expected.isDeleted) {
                            throw IllegalStateException("還原後交易紀錄欄位與備份不符 (ID: ${expected.id})")
                        }
                        if (db.accountDao().getAccountByIdWithDeleted(expected.sourceAccountId) == null) {
                            throw IllegalStateException("還原後交易之來源帳戶 ID ${expected.sourceAccountId} 不存在")
                        }
                        if (expected.targetAccountId != null && db.accountDao().getAccountByIdWithDeleted(expected.targetAccountId) == null) {
                            throw IllegalStateException("還原後交易之目標帳戶 ID ${expected.targetAccountId} 不存在")
                        }
                        if (expected.categoryId != null && db.categoryDao().getCategoryByIdWithDeleted(expected.categoryId) == null) {
                            throw IllegalStateException("還原後交易之分類 ID ${expected.categoryId} 不存在")
                        }
                        if (expected.loanId != null && db.loanDao().getLoanByIdWithDeleted(expected.loanId) == null) {
                            throw IllegalStateException("還原後交易之借貸 ID ${expected.loanId} 不存在")
                        }
                        if (expected.repaymentId != null && db.loanRepaymentDao().getRepaymentByIdWithDeleted(expected.repaymentId) == null) {
                            throw IllegalStateException("還原後交易之還款 ID ${expected.repaymentId} 不存在")
                        }
                    }

                    verifiedSummary = VerifiedRestoreSummary(
                        accountCount = dbAccounts.size,
                        categoryCount = dbCategories.size,
                        transactionCount = dbTransactions.size,
                        contactCount = dbContacts.size,
                        loanCount = dbLoans.size,
                        repaymentCount = dbRepayments.size
                    )
                }

                DatabaseInitializer.markInitialized()
                Result.success(verifiedSummary!!)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun backupToGoogleDrive(password: String?): Result<BackupSuccessDetails> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))

        return try {
            DatabaseInitializer.ensureInitialized(db)
            DataOperationCoordinator.runBackupOperation {
                val (payload, bytes) = createLocalBackupBytesInternal(password)
                val checksum = BackupCryptoManager.calculateSha256(bytes)
                val fileName = "moneytracker_backup_${System.currentTimeMillis()}.json"

                val driveFileId = drive.uploadBackupFile(fileName, bytes)

                val metadata = BackupMetadataEntity(
                    backupTime = System.currentTimeMillis(),
                    driveFileId = driveFileId,
                    checksum = checksum,
                    sizeBytes = bytes.size.toLong(),
                    status = "SUCCESS"
                )

                db.settingsAndBackupDao().insertBackupMetadata(metadata)
                db.settingsAndBackupDao().setSettingValue(
                    AppSettingsEntity("last_backup_time", metadata.backupTime.toString())
                )

                val details = BackupSuccessDetails(
                    backupTime = metadata.backupTime,
                    driveFileId = driveFileId,
                    checksum = checksum,
                    sizeBytes = bytes.size.toLong(),
                    accountCount = payload.accounts.size,
                    categoryCount = payload.categories.size,
                    transactionCount = payload.transactions.size,
                    contactCount = payload.contacts.size,
                    loanCount = payload.loans.size,
                    repaymentCount = payload.loanRepayments.size
                )

                Result.success(details)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listCloudBackups(): Result<List<CloudBackupInfo>> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))
        return try {
            val rawList = drive.listBackupFiles()
            val enrichedList = rawList.map { info ->
                try {
                    val downloadedBytes = drive.downloadBackupFile(info.fileId)
                    val payload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, null)
                    val isDefault = payload.transactions.isEmpty() &&
                            payload.loans.isEmpty() &&
                            payload.loanRepayments.isEmpty() &&
                            payload.accounts.all { it.initialBalance == 0L }

                    val summary = "交易 ${payload.transactions.size} 筆 | 帳戶 ${payload.accounts.size} 筆 | 借貸 ${payload.loans.size} 筆"
                    info.copy(isDefaultOnly = isDefault, summaryText = summary)
                } catch (e: Exception) {
                    // 若加密需要密碼或無法直接讀取內容，則保留基本資訊
                    info.copy(isDefaultOnly = false, summaryText = "加密備份檔或格式未知")
                }
            }
            Result.success(enrichedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectCloudBackup(fileId: String, password: String?): Result<BackupPayload> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))
        return try {
            android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Starting backup inspection for fileId=$fileId")
            DatabaseInitializer.ensureInitialized(db)
            val downloadedBytes = drive.downloadBackupFile(fileId)
            android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Downloaded backup file byte count=${downloadedBytes.size}")
            val payload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, password)
            android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Decrypted payload: accounts=${payload.accounts.size}, transactions=${payload.transactions.size}, loans=${payload.loans.size}")
            Result.success(payload)
        } catch (e: Exception) {
            android.util.Log.e("BackupRestoreRepo", "[Diagnosis] Inspection failed for fileId=$fileId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloudBackup(fileId: String, password: String?): Result<VerifiedRestoreSummary> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))
        return try {
            android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Starting cloud restore for fileId=$fileId")
            DatabaseInitializer.ensureInitialized(db)
            val downloadedBytes = drive.downloadBackupFile(fileId)
            android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Downloaded bytes successfully for restore, size=${downloadedBytes.size}")
            val payload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, password)
            android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Decrypted payload successfully, executing restoreToDb transaction...")
            val restoreResult = restoreFromPayload(payload)
            if (restoreResult.isSuccess) {
                val summary = restoreResult.getOrThrow()
                android.util.Log.d("BackupRestoreRepo", "[Diagnosis] Transaction committed & verified from DB: $summary")
            } else {
                android.util.Log.e("BackupRestoreRepo", "[Diagnosis] Restore transaction failed: ${restoreResult.exceptionOrNull()?.message}")
            }
            restoreResult
        } catch (e: Exception) {
            android.util.Log.e("BackupRestoreRepo", "[Diagnosis] Cloud restore exception for fileId=$fileId: ${e.message}")
            Result.failure(e)
        }
    }
}
