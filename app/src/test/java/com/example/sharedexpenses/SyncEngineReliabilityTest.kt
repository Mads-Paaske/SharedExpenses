package com.example.sharedexpenses

import com.example.sharedexpenses.data.local.ActivityDao
import com.example.sharedexpenses.data.local.ActivityEntity
import com.example.sharedexpenses.data.local.ActivityKind
import com.example.sharedexpenses.data.local.ExpenseDao
import com.example.sharedexpenses.data.local.ExpenseEntity
import com.example.sharedexpenses.data.local.ExpenseShareEntity
import com.example.sharedexpenses.data.local.OutboxDao
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.RequestType
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.network.NetworkMonitor
import com.example.sharedexpenses.data.remote.FakeRemoteApi
import com.example.sharedexpenses.data.sync.SyncEngine
import com.example.sharedexpenses.data.sync.SyncListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Reliability QA test (NF01, NF04, NF05):
 * a request made while offline is kept, marked FAILED, retried, and reported when it finally succeeds.
 */
class SyncEngineReliabilityTest {

    private val network = FakeNetwork()
    private val outbox = FakeOutboxDao()
    private val expenses = FakeExpenseDao()
    private val activity = FakeActivityDao()
    private val notified = mutableListOf<OutboxEntity>()
    private lateinit var engine: SyncEngine

    @Before
    fun setUp() {
        engine = SyncEngine(
            outbox, expenses, activity, FakeRemoteApi(network, latencyMs = 0),
            object : SyncListener {
                override suspend fun onRequestSynced(request: OutboxEntity) {}
                override suspend fun onPreviouslyFailedRequestSynced(request: OutboxEntity) { notified += request }
            },
        )
        expenses.statuses["e1"] = SyncStatus.PENDING
        outbox.items += OutboxEntity(id = 1, type = RequestType.ADD_EXPENSE, refId = "e1", label = "Groceries", amountOre = 150_00, createdAt = 0)
    }

    @Test
    fun online_requestIsSentFirstTime_noRetryNotification() = runTest {
        assertTrue(engine.syncPending())
        assertEquals(SyncStatus.SYNCED, outbox.items.single().status)
        assertEquals(SyncStatus.SYNCED, expenses.statuses["e1"])
        assertTrue(notified.isEmpty())
    }

    @Test
    fun offline_requestFails_isKeptAndMarkedFailed() = runTest {
        network.setSimulatedOffline(true)

        val done = engine.syncPending()

        assertFalse("worker must ask WorkManager to retry", done)
        val request = outbox.items.single()
        assertEquals(SyncStatus.FAILED, request.status)
        assertEquals(1, request.attempts)
        assertEquals(SyncStatus.FAILED, expenses.statuses["e1"])
    }

    @Test
    fun failedRequest_succeedsOnRetry_andUserIsNotified() = runTest {
        network.setSimulatedOffline(true)
        engine.syncPending()
        engine.syncPending() // still offline: second failed attempt
        assertEquals(2, outbox.items.single().attempts)

        network.setSimulatedOffline(false)
        val done = engine.syncPending()

        assertTrue(done)
        assertEquals(SyncStatus.SYNCED, outbox.items.single().status)
        assertEquals(SyncStatus.SYNCED, expenses.statuses["e1"])
        assertEquals("Groceries", notified.single().label)
        assertEquals(ActivityKind.SYNC_COMPLETE, activity.items.single().kind)
    }

    // ---- fakes ----

    private class FakeNetwork : NetworkMonitor {
        override val simulatedOffline = MutableStateFlow(false)
        override val online: StateFlow<Boolean> get() = MutableStateFlow(!simulatedOffline.value)
        override fun setSimulatedOffline(offline: Boolean) { simulatedOffline.value = offline }
        override fun isOnline() = !simulatedOffline.value
    }

    private class FakeOutboxDao : OutboxDao {
        val items = mutableListOf<OutboxEntity>()
        override suspend fun unsent() = items.filter { it.status != SyncStatus.SYNCED }
        override fun observeAll(): Flow<List<OutboxEntity>> = flowOf(items)
        override fun observe(id: Long): Flow<OutboxEntity?> = flowOf(items.firstOrNull { it.id == id })
        override suspend fun insert(item: OutboxEntity): Long { items += item; return item.id }
        override suspend fun update(item: OutboxEntity) { items.replaceAll { if (it.id == item.id) item else it } }
    }

    private class FakeExpenseDao : ExpenseDao {
        val statuses = mutableMapOf<String, SyncStatus>()
        override suspend fun setStatus(id: String, status: SyncStatus) { statuses[id] = status }
        override fun observeForGroup(groupId: String): Flow<List<ExpenseEntity>> = flowOf(emptyList())
        override fun observeAll(): Flow<List<ExpenseEntity>> = flowOf(emptyList())
        override fun observe(id: String): Flow<ExpenseEntity?> = flowOf(null)
        override fun observeShares(expenseId: String): Flow<List<ExpenseShareEntity>> = flowOf(emptyList())
        override fun observeAllShares(): Flow<List<ExpenseShareEntity>> = flowOf(emptyList())
        override suspend fun insertShares(shares: List<ExpenseShareEntity>) {}
        override suspend fun upsert(expense: ExpenseEntity) {}
    }

    private class FakeActivityDao : ActivityDao {
        val items = mutableListOf<ActivityEntity>()
        override fun observeAll(): Flow<List<ActivityEntity>> = flowOf(items)
        override suspend fun insert(activity: ActivityEntity) { items += activity }
    }
}
