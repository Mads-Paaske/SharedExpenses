package com.example.sharedexpenses

import android.content.Context
import com.example.sharedexpenses.data.local.AppDatabase
import com.example.sharedexpenses.data.network.AndroidNetworkMonitor
import com.example.sharedexpenses.data.prefs.PreferencesStore
import com.example.sharedexpenses.data.remote.FakeRemoteApi
import com.example.sharedexpenses.data.repository.GroupRepository
import com.example.sharedexpenses.data.repository.SyncRepository
import com.example.sharedexpenses.data.repository.UserRepository
import com.example.sharedexpenses.data.sync.Notifier
import com.example.sharedexpenses.data.sync.SyncEngine
import com.example.sharedexpenses.data.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Creates every object once and hands them out (manual dependency injection, no library).
 * Lives as long as the app process.
 */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob())

    private val database = AppDatabase.create(context)
    private val network = AndroidNetworkMonitor(context)
    private val api = FakeRemoteApi(network)
    private val scheduler = SyncScheduler(context)
    private val prefs = PreferencesStore(context)

    val notifier = Notifier(context, prefs)

    val syncRepository = SyncRepository(database.outboxDao(), scheduler, network)
    val userRepository = UserRepository(database.userDao(), database.activityDao(), prefs, syncRepository)
    val groupRepository = GroupRepository(
        database.groupDao(), database.expenseDao(), database.userDao(), database.activityDao(), syncRepository,
    )
    val syncEngine = SyncEngine(database.outboxDao(), database.expenseDao(), database.activityDao(), api, notifier)

    init {
        notifier.createChannels()
        appScope.launch { userRepository.seedIfEmpty { SeedData.insert(database) } }
        // When the connection comes back, send queued requests right away instead of waiting for the backoff.
        appScope.launch {
            network.online.drop(1).filter { it }.collect { scheduler.syncNow() }
        }
        scheduler.syncNow()
    }
}
