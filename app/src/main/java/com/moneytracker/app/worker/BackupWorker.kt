package com.moneytracker.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moneytracker.app.data.backup.BackupRestoreRepository
import com.moneytracker.app.data.backup.GoogleDriveManager
import com.moneytracker.app.data.db.AppDatabase
import java.util.concurrent.TimeUnit

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val googleAccount = db.settingsAndBackupDao().getSettingValue("google_account_email")

        if (googleAccount.isNullOrEmpty()) {
            return Result.success()
        }

        val driveManager = GoogleDriveManager(applicationContext, googleAccount)
        val backupRepo = BackupRestoreRepository(db, driveManager)

        if (backupRepo.isRestorePending(googleAccount)) {
            // 尚待使用者選擇「還原既有資料」或「以目前資料開始」，不得自動上傳備份
            return Result.success()
        }

        val isEncrypted = com.moneytracker.app.data.backup.SecureStorageManager.isAutoBackupEncrypted(applicationContext)
        val password = com.moneytracker.app.data.backup.SecureStorageManager.getAutoBackupPassword(applicationContext)

        if (isEncrypted && password.isNullOrBlank()) {
            return Result.failure()
        }

        val result = backupRepo.backupToGoogleDrive(if (isEncrypted) password else null)

        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "google_drive_auto_backup"

        fun schedulePeriodicBackup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (t: Throwable) {
                // 防範單元測試環境下 WorkManager 未初始化之異常
            }
        }

        fun cancelPeriodicBackup(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            } catch (t: Throwable) {
                // 防範單元測試環境下 WorkManager 未初始化之異常
            }
        }
    }

}
