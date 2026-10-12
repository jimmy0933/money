package com.moneytracker.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneytracker.app.data.dao.AccountDao
import com.moneytracker.app.data.dao.CategoryDao
import com.moneytracker.app.data.dao.ContactDao
import com.moneytracker.app.data.dao.LoanDao
import com.moneytracker.app.data.dao.LoanRepaymentDao
import com.moneytracker.app.data.dao.SettingsAndBackupDao
import com.moneytracker.app.data.dao.TransactionDao
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.BackupMetadataEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        ContactEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class,
        AppSettingsEntity::class,
        BackupMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun contactDao(): ContactDao
    abstract fun loanDao(): LoanDao
    abstract fun loanRepaymentDao(): LoanRepaymentDao
    abstract fun settingsAndBackupDao(): SettingsAndBackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 種入預設帳戶與預設分類
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            seedDefaultData(database)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDefaultData(db: AppDatabase) {
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
        }
    }
}
