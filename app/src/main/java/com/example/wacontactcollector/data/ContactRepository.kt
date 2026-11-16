package com.example.wacontactcollector.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "wa_contact_collector.db"
    ).build()
    val repository = ContactRepository(appContext, database)
}

class ContactRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val total = database.customerDao().observeTotal()
    val customers = database.customerDao().observeAll()
    val history = database.captureHistoryDao().observeRecent()

    suspend fun recordApprovedCandidate(rawPhone: String): CaptureResult {
        val started = System.currentTimeMillis()
        val phone = PhoneNormalizer.normalize(rawPhone)
        if (phone == null) {
            recordHistory(started, 0, 0, "INVALID", "Nomor tidak valid")
            return CaptureResult(null, saved = false, duplicate = false, "Nomor tidak valid")
        }

        val inDeviceContacts = DeviceContactsChecker(context).exists(phone)
        val inserted = database.customerDao().insert(
            CustomerEntity(
                phone = phone,
                displayName = null,
                source = "whatsapp_accessibility_approved",
                collectedAt = System.currentTimeMillis(),
                isInDeviceContacts = inDeviceContacts
            )
        )
        return if (inserted == -1L) {
            recordHistory(started, 0, 1, "DUPLICATE", "Nomor sudah tersimpan")
            CaptureResult(phone, saved = false, duplicate = true, "Nomor sudah tersimpan")
        } else {
            recordHistory(started, 1, 1, "SAVED", "Nomor disimpan oleh pengguna")
            CaptureResult(phone, saved = true, duplicate = false, "Nomor berhasil disimpan")
        }
    }

    private suspend fun recordHistory(
        started: Long,
        newCount: Int,
        receivedCount: Int,
        status: String,
        message: String
    ) {
        database.captureHistoryDao().insert(
            CaptureHistoryEntity(
                startedAt = started,
                finishedAt = System.currentTimeMillis(),
                newCount = newCount,
                receivedCount = receivedCount,
                status = status,
                message = message
            )
        )
    }

    suspend fun deleteAll() {
        database.customerDao().deleteAll()
        database.captureHistoryDao().deleteAll()
    }

    fun launchWhatsApp(business: Boolean): Intent {
        val packageName = if (business) "com.whatsapp.w4b" else "com.whatsapp"
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
            setPackage(packageName)
        }
    }
}
