package com.moneytracker.app.data.backup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveManager(
    private val context: Context,
    private val accountName: String
) : DriveManager {
    private val driveService: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccountName = accountName
        }

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("MoneyTrackerApp")
        .build()
    }

    override suspend fun uploadBackupFile(fileName: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        val fileMetadata = File().apply {
            name = fileName
            parents = listOf("appDataFolder")
        }

        val mediaContent = ByteArrayContent("application/json", data)
        val file = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()

        file.id
    }

    override suspend fun listBackupFiles(): List<CloudBackupInfo> = withContext(Dispatchers.IO) {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name, createdTime, size)")
            .setOrderBy("createdTime desc")
            .execute()

        result.files?.map { file ->
            CloudBackupInfo(
                fileId = file.id,
                fileName = file.name ?: "backup.json",
                createdTimeMillis = file.createdTime?.value ?: System.currentTimeMillis(),
                sizeBytes = file.getSize() ?: 0L
            )
        } ?: emptyList()
    }

    override suspend fun downloadBackupFile(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val outputStream = java.io.ByteArrayOutputStream()
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.toByteArray()
    }
}
