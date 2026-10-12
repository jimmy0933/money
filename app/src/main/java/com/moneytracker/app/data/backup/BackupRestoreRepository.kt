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
            // 使用交易式處理，確保還原失敗不會產生半套資料
            db.withTransaction {
                // 清空既有資料
                db.clearAllTables()

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

                // 還原設定
                payload.settings.forEach { db.settingsAndBackupDao().setSettingValue(it) }
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
