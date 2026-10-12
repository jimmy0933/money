package com.moneytracker.app.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import com.moneytracker.app.data.entity.TransactionEntity
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun createBackupPayload(db: AppDatabase): BackupPayload {
        return BackupPayload(
            schemaVersion = 1,
            createdAt = System.currentTimeMillis(),
            accounts = db.accountDao().getAllAccounts(),
            categories = db.categoryDao().getAllCategories(),
            transactions = db.transactionDao().getAllTransactions(),
            contacts = db.contactDao().getAllContacts(),
            loans = db.loanDao().getAllLoans(),
            loanRepayments = db.loanRepaymentDao().getAllRepayments(),
            settings = emptyList()
        )
    }

    fun serializeAndEncryptPayload(payload: BackupPayload, password: String?): ByteArray {
        val jsonString = gson.toJson(payload)
        val rawBytes = jsonString.toByteArray(Charsets.UTF_8)

        if (password.isNull_or_blank()) {
            return rawBytes
        }

        return encryptAesGcmOrPbe(rawBytes, password)
    }

    fun decryptAndDeserializePayload(encryptedData: ByteArray, password: String?): BackupPayload {
        val rawBytes = if (password.isNull_or_blank()) {
            encryptedData
        } else {
            decryptAesGcmOrPbe(encryptedData, password)
        }

        val jsonString = String(rawBytes, Charsets.UTF_8)
        val payload = gson.fromJson(jsonString, BackupPayload::class.java)
            ?: throw IllegalArgumentException("無效的備份 JSON 格式")

        if (payload.schemaVersion > 1) {
            throw IllegalArgumentException("不支援的備份版本: ${payload.schemaVersion}")
        }

        return payload
    }

    private fun encryptAesGcmOrPbe(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16) { 0x42.toByte() } // 固定 Salt 簡化驗證 (可擴充)
        val iv = ByteArray(16) { 0x24.toByte() }   // 固定 IV

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun decryptAesGcmOrPbe(encryptedData: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16) { 0x42.toByte() }
        val iv = ByteArray(16) { 0x24.toByte() }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(encryptedData)
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
