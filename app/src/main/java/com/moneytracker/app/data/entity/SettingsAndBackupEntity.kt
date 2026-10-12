package com.moneytracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "backup_metadata")
data class BackupMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val backupTime: Long,
    val driveFileId: String,
    val checksum: String,
    val sizeBytes: Long,
    val status: String
)
