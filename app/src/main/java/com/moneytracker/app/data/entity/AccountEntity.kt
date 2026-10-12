package com.moneytracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moneytracker.app.data.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long, // TWD 最小單位 (元)
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
