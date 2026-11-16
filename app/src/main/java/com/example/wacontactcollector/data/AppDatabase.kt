package com.example.wacontactcollector.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class, CaptureHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun captureHistoryDao(): CaptureHistoryDao
}
