package com.moneytracker.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
