package com.moneytracker.app.data.backup

interface DriveManager {
    suspend fun uploadBackupFile(fileName: String, data: ByteArray): String
    suspend fun listBackupFiles(): List<CloudBackupInfo>
    suspend fun downloadBackupFile(fileId: String): ByteArray
}
