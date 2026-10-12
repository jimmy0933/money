package com.moneytracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moneytracker.app.data.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType, // EXPENSE or INCOME
    val iconName: String = "category",
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
