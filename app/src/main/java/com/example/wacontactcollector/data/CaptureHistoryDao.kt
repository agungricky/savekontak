package com.example.wacontactcollector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureHistoryDao {
    @Insert
    suspend fun insert(item: CaptureHistoryEntity)

    @Query("SELECT * FROM sync_history ORDER BY finishedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<CaptureHistoryEntity>>

    @Query("DELETE FROM sync_history")
    suspend fun deleteAll()
}
