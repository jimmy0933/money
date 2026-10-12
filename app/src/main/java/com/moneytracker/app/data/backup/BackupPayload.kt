package com.moneytracker.app.data.backup

import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.AppSettingsEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import com.moneytracker.app.data.entity.TransactionEntity

data class BackupPayload(
    val schemaVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val contacts: List<ContactEntity>,
    val loans: List<LoanEntity>,
    val loanRepayments: List<LoanRepaymentEntity>,
    val settings: List<AppSettingsEntity>
)
