package com.moneytracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneytracker.app.data.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("sourceAccountId"),
        Index("targetAccountId"),
        Index("categoryId"),
        Index("loanId"),
        Index("repaymentId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long, // TWD 最小金額
    val dateMillis: Long,
    val categoryId: Long? = null,
    val sourceAccountId: Long, // 支出/借出/轉出的帳戶
    val targetAccountId: Long? = null, // 轉帳目標帳戶
    val note: String = "",
    val loanId: Long? = null, // 關聯的借貸 ID
    val repaymentId: Long? = null, // 關聯的還款 ID
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
