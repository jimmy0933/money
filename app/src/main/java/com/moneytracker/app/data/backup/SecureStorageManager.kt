package com.moneytracker.app.data.backup

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStorageManager {
    private const val PREFS_FILE_NAME = "secure_backup_prefs"
    private const val KEY_AUTO_BACKUP_PASSWORD = "auto_backup_password"
    private const val KEY_IS_BACKUP_ENCRYPTED = "is_backup_encrypted"

    private fun getSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setAutoBackupPassword(context: Context, password: String?) {
        val prefs = getSharedPrefs(context)
        with(prefs.edit()) {
            if (password.isNullOrBlank()) {
                remove(KEY_AUTO_BACKUP_PASSWORD)
                putBoolean(KEY_IS_BACKUP_ENCRYPTED, false)
            } else {
                putString(KEY_AUTO_BACKUP_PASSWORD, password)
                putBoolean(KEY_IS_BACKUP_ENCRYPTED, true)
            }
            apply()
        }
    }

    fun getAutoBackupPassword(context: Context): String? {
        val prefs = getSharedPrefs(context)
        return prefs.getString(KEY_AUTO_BACKUP_PASSWORD, null)
    }

    fun isAutoBackupEncrypted(context: Context): Boolean {
        val prefs = getSharedPrefs(context)
        return prefs.getBoolean(KEY_IS_BACKUP_ENCRYPTED, false)
    }
}
