package com.moneytracker.app

import com.moneytracker.app.data.backup.CloudBackupInfo
import com.moneytracker.app.data.backup.DriveManager

class FakeDriveManager : DriveManager {
    val files = mutableMapOf<String, Pair<String, ByteArray>>() // fileId -> (fileName, data)
    private var fileCounter = 0

    override suspend fun uploadBackupFile(fileName: String, data: ByteArray): String {
        fileCounter++
        val fileId = "drive_file_id_$fileCounter"
        files[fileId] = Pair(fileName, data)
        return fileId
    }

    override suspend fun listBackupFiles(): List<CloudBackupInfo> {
        return files.map { (id, pair) ->
            CloudBackupInfo(
                fileId = id,
                fileName = pair.first,
                createdTimeMillis = System.currentTimeMillis(),
                sizeBytes = pair.second.size.toLong()
            )
        }.reversed()
    }

    override suspend fun downloadBackupFile(fileId: String): ByteArray {
        return files[fileId]?.second ?: throw IllegalArgumentException("檔案不存在: $fileId")
    }
}
