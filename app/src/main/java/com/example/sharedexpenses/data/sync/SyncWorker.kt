package com.example.sharedexpenses.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sharedexpenses.SharedExpensesApplication
import java.util.concurrent.TimeUnit

/**
 * NF01: background retry. WorkManager runs this even if the user leaves the app.
 * If a request fails, Result.retry() makes WorkManager try again later with exponential backoff
 * (10 s, 20 s, 40 s, ...).
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as SharedExpensesApplication).container
        return if (container.syncEngine.syncPending()) Result.success() else Result.retry()
    }
}

class SyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    /** Start sending now. REPLACE restarts a worker that is waiting in backoff, so new requests go out right away. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "outbox_sync"
    }
}
