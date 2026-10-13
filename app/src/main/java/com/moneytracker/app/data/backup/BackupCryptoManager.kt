package com.moneytracker.app.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import androidx.room.withTransaction
import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import com.moneytracker.app.data.entity.TransactionEntity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val random = SecureRandom()

    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12 // 96-bit IV for AES-GCM
    private const val GCM_TAG_LENGTH = 128

    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun createBackupPayload(db: AppDatabase): BackupPayload {
        return db.withTransaction {
            BackupPayload(
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
    }

    fun serializeAndEncryptPayload(payload: BackupPayload, password: String?): ByteArray {
        val jsonString = gson.toJson(payload)
        val rawBytes = jsonString.toByteArray(Charsets.UTF_8)

        if (password.isNullOrBlank()) {
            return rawBytes
        }

        return encryptAesGcm(rawBytes, password)
    }

    fun decryptAndDeserializePayload(encryptedData: ByteArray, password: String?): BackupPayload {
        val rawBytes = if (password.isNullOrBlank()) {
            encryptedData
        } else {
            decryptAesGcm(encryptedData, password)
        }

        val jsonString = String(rawBytes, Charsets.UTF_8)
        val payload = try {
            gson.fromJson(jsonString, BackupPayload::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("無效的備份 JSON 格式: ${e.message}")
        } ?: throw IllegalArgumentException("無效的備份 JSON 格式")

        BackupPayloadValidator.validate(payload)

        return payload
    }

    private fun encryptAesGcm(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(data)

        // Header: [SALT 16B] + [IV 12B] + [CIPHERTEXT]
        val result = ByteArray(SALT_LENGTH + IV_LENGTH + ciphertext.size)
        System.arraycopy(salt, 0, result, 0, SALT_LENGTH)
        System.arraycopy(iv, 0, result, SALT_LENGTH, IV_LENGTH)
        System.arraycopy(ciphertext, 0, result, SALT_LENGTH + IV_LENGTH, ciphertext.size)

        return result
    }

    private fun decryptAesGcm(encryptedData: ByteArray, password: String): ByteArray {
        if (encryptedData.size < SALT_LENGTH + IV_LENGTH) {
            throw IllegalArgumentException("無效的加密資料長度")
        }

        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(encryptedData, 0, salt, 0, SALT_LENGTH)

        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(encryptedData, SALT_LENGTH, iv, 0, IV_LENGTH)

        val ciphertextOffset = SALT_LENGTH + IV_LENGTH
        val ciphertextLength = encryptedData.size - ciphertextOffset
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(encryptedData, ciphertextOffset, ciphertext, 0, ciphertextLength)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

}
