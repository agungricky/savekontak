package com.example.wacontactcollector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customer: CustomerEntity): Long

    @Query("SELECT COUNT(*) FROM customers")
    fun observeTotal(): Flow<Int>

    @Query("SELECT * FROM customers ORDER BY collectedAt DESC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT phone FROM customers")
    suspend fun getAllPhones(): List<String>

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
