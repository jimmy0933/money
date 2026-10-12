package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.BackupMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsAndBackupDao {
    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT value FROM app_settings WHERE key = :key")
    fun getSettingValueFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSettingValue(setting: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupMetadata(metadata: BackupMetadataEntity): Long

    @Query("SELECT * FROM backup_metadata ORDER BY backupTime DESC")
    fun getAllBackupMetadataFlow(): Flow<List<BackupMetadataEntity>>

    @Query("SELECT * FROM backup_metadata ORDER BY backupTime DESC LIMIT 1")
    suspend fun getLatestBackupMetadata(): BackupMetadataEntity?
}
