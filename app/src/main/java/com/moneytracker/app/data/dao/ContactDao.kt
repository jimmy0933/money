package com.moneytracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.data.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsWithDeleted(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id AND isDeleted = 0")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactByIdWithDeleted(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getContactByName(name: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteContact(id: Long, updatedAt: Long = System.currentTimeMillis())
}
