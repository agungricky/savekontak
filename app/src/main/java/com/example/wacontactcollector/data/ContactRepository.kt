package com.example.wacontactcollector.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "wa_contact_collector.db"
    ).build()
    val preferences = PreferencesStore(appContext)
    val repository = ContactRepository(appContext, database, preferences)
}

class ContactRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val preferences: PreferencesStore,
    private val apiClient: OfficialApiClient = OfficialApiClient()
) {
    val total = database.customerDao().observeTotal()
    val customers = database.customerDao().observeAll()
    val history = database.syncHistoryDao().observeRecent()

    suspend fun sync(): SyncOutcome {
        val started = System.currentTimeMillis()
        val settings = preferences.settings.first()
        if (settings.endpoint.isBlank()) {
            val message = "Endpoint resmi belum dikonfigurasi"
            database.syncHistoryDao().insert(
                SyncHistoryEntity(
                    startedAt = started,
                    finishedAt = System.currentTimeMillis(),
                    newCount = 0,
                    receivedCount = 0,
                    status = "SKIPPED",
                    message = message
                )
            )
            return SyncOutcome(0, 0, message)
        }

        return try {
            require(settings.endpoint.startsWith("https://")) {
                "Endpoint harus menggunakan HTTPS"
            }
            val remote = apiClient.fetch(settings.endpoint, settings.token)
            val contactChecker = DeviceContactsChecker(context)
            var newCount = 0
            remote.forEach { item ->
                val phone = PhoneNormalizer.normalize(item.phone) ?: return@forEach
                val inserted = database.customerDao().insert(
                    CustomerEntity(
                        phone = phone,
                        displayName = item.displayName,
                        source = "official_api",
                        collectedAt = System.currentTimeMillis(),
                        isInDeviceContacts = settings.checkDeviceContacts && contactChecker.exists(phone)
                    )
                )
                if (inserted != -1L) newCount++
            }
            val message = "Sinkronisasi berhasil"
            database.syncHistoryDao().insert(
                SyncHistoryEntity(
                    startedAt = started,
                    finishedAt = System.currentTimeMillis(),
                    newCount = newCount,
                    receivedCount = remote.size,
                    status = "SUCCESS",
                    message = message
                )
            )
            SyncOutcome(newCount, remote.size, message)
        } catch (error: Exception) {
            val message = error.message?.take(160) ?: "Gagal mengakses endpoint"
            database.syncHistoryDao().insert(
                SyncHistoryEntity(
                    startedAt = started,
                    finishedAt = System.currentTimeMillis(),
                    newCount = 0,
                    receivedCount = 0,
                    status = "FAILED",
                    message = message
                )
            )
            SyncOutcome(0, 0, message)
        }
    }

    suspend fun deleteAll() {
        database.customerDao().deleteAll()
        database.syncHistoryDao().deleteAll()
    }

    fun scheduleSync(enabled: Boolean, intervalHours: Int) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(SyncWorker.WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        manager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun launchWhatsApp(business: Boolean): Intent {
        val packageName = if (business) "com.whatsapp.w4b" else "com.whatsapp"
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
            setPackage(packageName)
        }
    }
}
