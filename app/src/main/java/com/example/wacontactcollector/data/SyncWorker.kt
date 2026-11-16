package com.example.wacontactcollector.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wacontactcollector.WaContactApplication

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as WaContactApplication).container.repository
        val result = repository.sync()
        return if (result.message == "Sinkronisasi berhasil" || result.message.contains("belum")) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "official_customer_sync"
    }
}
