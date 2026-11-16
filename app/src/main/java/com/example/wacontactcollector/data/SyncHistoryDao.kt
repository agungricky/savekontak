package com.example.wacontactcollector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncHistoryDao {
    @Insert
    suspend fun insert(item: SyncHistoryEntity)

    @Query("SELECT * FROM sync_history ORDER BY finishedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<SyncHistoryEntity>>

    @Query("DELETE FROM sync_history")
    suspend fun deleteAll()
}
