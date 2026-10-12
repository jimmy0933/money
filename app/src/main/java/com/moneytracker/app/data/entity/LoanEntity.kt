package com.moneytracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("contactId"),
        Index("accountId")
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val type: LoanType,
    val amount: Long, // 原始借款金額
    val dateMillis: Long,
    val dueDateMillis: Long? = null, // 預計還款日期 (可不填)
    val accountId: Long,
    val note: String = "",
    val status: LoanStatus = LoanStatus.UNPAID,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
