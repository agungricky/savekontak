package com.example.wacontactcollector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val phone: String,
    val displayName: String?,
    val source: String,
    val collectedAt: Long,
    val isInDeviceContacts: Boolean
)

@Entity(tableName = "sync_history")
data class CaptureHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long,
    val newCount: Int,
    val receivedCount: Int,
    val status: String,
    val message: String?
)

data class CaptureResult(
    val normalizedPhone: String?,
    val saved: Boolean,
    val duplicate: Boolean,
    val message: String
)

data class PendingPhone(
    val normalizedPhone: String,
    val detectedAt: Long = System.currentTimeMillis()
)

data class ScanStatus(
    val packageName: String? = null,
    val eventCount: Long = 0,
    val lastScanAt: Long? = null
)

data class RemoteCustomer(
    val phone: String,
    val displayName: String? = null
)

data class SyncOutcome(
    val newCount: Int,
    val receivedCount: Int,
    val message: String
)
