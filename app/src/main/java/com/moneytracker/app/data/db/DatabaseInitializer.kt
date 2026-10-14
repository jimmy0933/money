package com.moneytracker.app.data.db

import androidx.room.withTransaction
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.TransactionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DatabaseInitializer {
    private val mutex = Mutex()
    @Volatile
    private var isInitializedMemory = false

    suspend fun ensureInitialized(db: AppDatabase) {
        if (isInitializedMemory) return
        mutex.withLock {
            if (isInitializedMemory) return
            db.withTransaction {
                val initDone = db.settingsAndBackupDao().getSettingValue("db_initialized")
                if (initDone != "true") {
                    if (db.accountDao().getAccountCount() == 0) {
                        db.accountDao().insertAccount(
                            AccountEntity(name = "現金", type = AccountType.CASH, initialBalance = 0)
                        )
                        db.accountDao().insertAccount(
                            AccountEntity(name = "銀行帳戶", type = AccountType.BANK, initialBalance = 0)
                        )
                        db.accountDao().insertAccount(
                            AccountEntity(name = "電子支付", type = AccountType.E_PAYMENT, initialBalance = 0)
                        )
                        db.accountDao().insertAccount(
                            AccountEntity(name = "信用卡", type = AccountType.CREDIT_CARD, initialBalance = 0)
                        )
                    }

                    if (db.categoryDao().getCategoryCount() == 0) {
                        val defaultExpenses = listOf("餐飲", "交通", "購物", "娛樂", "生活", "醫療", "家庭", "其他")
                        val defaultIncomes = listOf("薪資", "獎金", "投資", "其他收入")

                        defaultExpenses.forEach { name ->
                            db.categoryDao().insertCategory(
                                CategoryEntity(name = name, type = TransactionType.EXPENSE, isDefault = true)
                            )
                        }

                        defaultIncomes.forEach { name ->
                            db.categoryDao().insertCategory(
                                CategoryEntity(name = name, type = TransactionType.INCOME, isDefault = true)
                            )
                        }
                    }

                    db.settingsAndBackupDao().setSettingValue(
                        AppSettingsEntity("db_initialized", "true")
                    )
                }
            }
            isInitializedMemory = true
        }
    }

    fun markInitialized() {
        isInitializedMemory = true
    }

    fun resetInitializationStateForTest() {
        isInitializedMemory = false
    }
}
