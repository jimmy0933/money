package com.moneytracker.app.data.backup

import androidx.room.withTransaction
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.BackupMetadataEntity
import com.moneytracker.app.data.repository.DataOperationCoordinator

data class CloudBackupInfo(
    val fileId: String,
    val fileName: String,
    val createdTimeMillis: Long,
    val sizeBytes: Long
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

    private suspend fun createLocalBackupBytesInternal(password: String?): Pair<BackupPayload, ByteArray> {
        val payload = BackupCryptoManager.createBackupPayload(db)
        val bytes = BackupCryptoManager.serializeAndEncryptPayload(payload, password)
        return Pair(payload, bytes)
    }

    suspend fun createLocalBackupBytes(password: String?): Pair<BackupPayload, ByteArray> {
        return DataOperationCoordinator.runBackupOperation {
            createLocalBackupBytesInternal(password)
        }
    }

    suspend fun restoreFromPayload(payload: BackupPayload): Result<VerifiedRestoreSummary> {
        return DataOperationCoordinator.runRestoreOperation {
            try {
                // 1. 資料合法性與完整性關聯驗證
                BackupPayloadValidator.validate(payload)

                // 2. 交易式寫入 Room DB
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

                    if (!currentGoogleEmail.isNullOrBlank()) {
                        db.settingsAndBackupDao().setSettingValue(
                            AppSettingsEntity("google_account_email", currentGoogleEmail)
                        )
                    }
                }

                // 3. 還原提交後，讀回 Room 各表核對 ID、欄位與筆數，再顯示成功，不能只顯示 payload 的筆數
                val dbAccounts = db.accountDao().getAllAccounts()
                val dbCategories = db.categoryDao().getAllCategories()
                val dbContacts = db.contactDao().getAllContacts()
                val dbLoans = db.loanDao().getAllLoans()
                val dbRepayments = db.loanRepaymentDao().getAllRepayments()
                val dbTransactions = db.transactionDao().getAllTransactions()

                if (dbAccounts.size != payload.accounts.size ||
                    dbAccounts.map { it.id }.toSet() != payload.accounts.map { it.id }.toSet() ||
                    dbAccounts.any { dbAcc -> payload.accounts.none { it.id == dbAcc.id && it.name == dbAcc.name && it.initialBalance == dbAcc.initialBalance } }) {
                    throw IllegalStateException("還原後帳戶表數據與備份內容不符 (預期: ${payload.accounts.size} 筆, 實際: ${dbAccounts.size} 筆)")
                }

                if (dbCategories.size != payload.categories.size ||
                    dbCategories.map { it.id }.toSet() != payload.categories.map { it.id }.toSet()) {
                    throw IllegalStateException("還原後分類表數據與備份內容不符 (預期: ${payload.categories.size} 筆, 實際: ${dbCategories.size} 筆)")
                }

                if (dbContacts.size != payload.contacts.size ||
                    dbContacts.map { it.id }.toSet() != payload.contacts.map { it.id }.toSet()) {
                    throw IllegalStateException("還原後聯絡人表數據與備份內容不符 (預期: ${payload.contacts.size} 筆, 實際: ${dbContacts.size} 筆)")
                }

                if (dbLoans.size != payload.loans.size ||
                    dbLoans.map { it.id }.toSet() != payload.loans.map { it.id }.toSet()) {
                    throw IllegalStateException("還原後借貸表數據與備份內容不符 (預期: ${payload.loans.size} 筆, 實際: ${dbLoans.size} 筆)")
                }

                if (dbRepayments.size != payload.loanRepayments.size ||
                    dbRepayments.map { it.id }.toSet() != payload.loanRepayments.map { it.id }.toSet()) {
                    throw IllegalStateException("還原後還款表數據與備份內容不符 (預期: ${payload.loanRepayments.size} 筆, 實際: ${dbRepayments.size} 筆)")
                }

                if (dbTransactions.size != payload.transactions.size ||
                    dbTransactions.map { it.id }.toSet() != payload.transactions.map { it.id }.toSet()) {
                    throw IllegalStateException("還原後交易表數據與備份內容不符 (預期: ${payload.transactions.size} 筆, 實際: ${dbTransactions.size} 筆)")
                }

                Result.success(
                    VerifiedRestoreSummary(
                        accountCount = dbAccounts.size,
                        categoryCount = dbCategories.size,
                        transactionCount = dbTransactions.size,
                        contactCount = dbContacts.size,
                        loanCount = dbLoans.size,
                        repaymentCount = dbRepayments.size
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun backupToGoogleDrive(password: String?): Result<BackupSuccessDetails> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))

        return try {
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
            val list = drive.listBackupFiles()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectCloudBackup(fileId: String, password: String?): Result<BackupPayload> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))
        return try {
            val downloadedBytes = drive.downloadBackupFile(fileId)
            val payload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, password)
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloudBackup(fileId: String, password: String?): Result<VerifiedRestoreSummary> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))
        return try {
            val downloadedBytes = drive.downloadBackupFile(fileId)
            val payload = BackupCryptoManager.decryptAndDeserializePayload(downloadedBytes, password)
            restoreFromPayload(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
