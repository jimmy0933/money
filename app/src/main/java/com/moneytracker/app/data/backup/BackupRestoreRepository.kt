package com.moneytracker.app.data.backup

import androidx.room.withTransaction
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.BackupMetadataEntity

data class CloudBackupInfo(
    val fileId: String,
    val fileName: String,
    val createdTimeMillis: Long,
    val sizeBytes: Long
)

class BackupRestoreRepository(
    private val db: AppDatabase,
    private val driveManager: GoogleDriveManager? = null
) {

    suspend fun createLocalBackupBytes(password: String?): ByteArray {
        val payload = BackupCryptoManager.createBackupPayload(db)
        return BackupCryptoManager.serializeAndEncryptPayload(payload, password)
    }

    suspend fun restoreFromPayload(payload: BackupPayload): Result<Unit> {
        return try {
            // 先進行資料合法性與完整性關聯驗證
            BackupPayloadValidator.validate(payload)

            // 建立還原前的本機資料快照，做為可恢復的快照依據
            val currentLocalSnapshot = BackupCryptoManager.createBackupPayload(db)

            // 使用交易式處理，確保還原失敗不會產生半套資料
            db.withTransaction {
                // 保留目前裝置的 Google 帳號與登入設定
                val currentGoogleEmail = db.settingsAndBackupDao().getSettingValue("google_account_email")

                // 只清理與替換業務資料表，不呼叫 clearAllTables() 清空設定表
                val database = db.openHelper.writableDatabase
                database.execSQL("DELETE FROM transactions")
                database.execSQL("DELETE FROM loan_repayments")
                database.execSQL("DELETE FROM loans")
                database.execSQL("DELETE FROM contacts")
                database.execSQL("DELETE FROM categories")
                database.execSQL("DELETE FROM accounts")

                // 還原帳戶
                payload.accounts.forEach { db.accountDao().insertAccount(it) }

                // 還原分類
                payload.categories.forEach { db.categoryDao().insertCategory(it) }

                // 還原聯絡人
                payload.contacts.forEach { db.contactDao().insertContact(it) }

                // 還原借貸
                payload.loans.forEach { db.loanDao().insertLoan(it) }

                // 還原還款
                payload.loanRepayments.forEach { db.loanRepaymentDao().insertRepayment(it) }

                // 還原交易明細
                payload.transactions.forEach { db.transactionDao().insertTransaction(it) }

                // 還原業務設定 (過濾掉登入身分/憑證相關設定，避免覆蓋本機登入身分)
                payload.settings
                    .filter { it.key != "google_account_email" }
                    .forEach { db.settingsAndBackupDao().setSettingValue(it) }

                // 確保本機 Google 帳號連結完整恢復/保留
                if (!currentGoogleEmail.isNullOrBlank()) {
                    db.settingsAndBackupDao().setSettingValue(
                        AppSettingsEntity("google_account_email", currentGoogleEmail)
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupToGoogleDrive(password: String?): Result<BackupMetadataEntity> {
        val drive = driveManager ?: return Result.failure(IllegalStateException("Google Drive 服務未初始化或未登入"))

        return try {
            val bytes = createLocalBackupBytes(password)
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

            Result.success(metadata)
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

    suspend fun restoreFromCloudBackup(fileId: String, password: String?): Result<Unit> {
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
