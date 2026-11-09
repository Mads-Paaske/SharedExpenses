package com.example.sharedexpenses.data.sync

import com.example.sharedexpenses.data.local.ActivityDao
import com.example.sharedexpenses.data.local.ActivityEntity
import com.example.sharedexpenses.data.local.ActivityKind
import com.example.sharedexpenses.data.local.ExpenseDao
import com.example.sharedexpenses.data.local.OutboxDao
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.RequestType
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.remote.FakeRemoteApi
import java.io.IOException

interface SyncListener {
    /** Every request the server accepted - used for group activity notifications (F12, F13). */
    suspend fun onRequestSynced(request: OutboxEntity)

    /** A request that failed earlier finally went through (NF05). */
    suspend fun onPreviouslyFailedRequestSynced(request: OutboxEntity)
}

/**
 * Sends everything in the outbox to the server.
 * No Android classes here, so it can be unit tested with fake DAOs and a fake network.
 */
class SyncEngine(
    private val outboxDao: OutboxDao,
    private val expenseDao: ExpenseDao,
    private val activityDao: ActivityDao,
    private val api: FakeRemoteApi,
    private val listener: SyncListener,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** @return true when the outbox is empty afterwards, false if something must be retried later. */
    suspend fun syncPending(): Boolean {
        var allSent = true
        for (request in outboxDao.unsent()) {
            try {
                api.send(request)
                onSuccess(request)
            } catch (e: IOException) {
                allSent = false
                outboxDao.update(request.copy(status = SyncStatus.FAILED, attempts = request.attempts + 1))
                if (request.type == RequestType.ADD_EXPENSE) expenseDao.setStatus(request.refId, SyncStatus.FAILED)
            }
        }
        return allSent
    }

    private suspend fun onSuccess(request: OutboxEntity) {
        outboxDao.update(request.copy(status = SyncStatus.SYNCED))
        if (request.type == RequestType.ADD_EXPENSE) expenseDao.setStatus(request.refId, SyncStatus.SYNCED)
        listener.onRequestSynced(request)
        if (request.attempts > 0) {
            activityDao.insert(
                ActivityEntity(
                    kind = ActivityKind.SYNC_COMPLETE, groupId = null, groupName = "",
                    actorName = "", title = request.label, amountOre = request.amountOre, createdAt = clock(),
                )
            )
            listener.onPreviouslyFailedRequestSynced(request)
        }
    }
}
