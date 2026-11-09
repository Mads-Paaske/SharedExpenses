package com.example.sharedexpenses.data.repository

import com.example.sharedexpenses.data.local.OutboxDao
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.RequestType
import com.example.sharedexpenses.data.network.NetworkMonitor
import com.example.sharedexpenses.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Queues requests for the server and exposes their state to the UI (NF04). */
class SyncRepository(
    private val outboxDao: OutboxDao,
    private val scheduler: SyncScheduler,
    private val network: NetworkMonitor,
) {
    val requests: Flow<List<OutboxEntity>> = outboxDao.observeAll()
    val online: StateFlow<Boolean> = network.online
    val simulatedOffline: StateFlow<Boolean> = network.simulatedOffline

    fun observeRequest(id: Long): Flow<OutboxEntity?> = outboxDao.observe(id)

    suspend fun enqueue(type: RequestType, refId: String, label: String, amountOre: Long = 0): Long {
        val id = outboxDao.insert(
            OutboxEntity(type = type, refId = refId, label = label, amountOre = amountOre, createdAt = System.currentTimeMillis())
        )
        scheduler.syncNow()
        return id
    }

    /** Demo controls from the lo-fi (screens 19, 23, 24). */
    fun simulateNetworkFailure() = network.setSimulatedOffline(true)

    fun restoreConnection() {
        network.setSimulatedOffline(false)
        scheduler.syncNow()
    }

    fun retryNow() = scheduler.syncNow()
}
