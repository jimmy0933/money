package com.moneytracker.app.data.db

import androidx.room.TypeConverter
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromLoanType(value: LoanType): String = value.name

    @TypeConverter
    fun toLoanType(value: String): LoanType = LoanType.valueOf(value)

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus = LoanStatus.valueOf(value)
}
