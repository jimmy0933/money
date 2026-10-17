package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategoriesWithDeleted(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = :type AND isDeleted = 0 ORDER BY id ASC")
    fun getCategoriesByTypeFlow(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id AND isDeleted = 0")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteCategory(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM categories WHERE isDeleted = 0")
    suspend fun getCategoryCount(): Int
}
