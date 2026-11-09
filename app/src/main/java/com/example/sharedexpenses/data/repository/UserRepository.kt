package com.example.sharedexpenses.data.repository

import com.example.sharedexpenses.data.local.ActivityDao
import com.example.sharedexpenses.data.local.ActivityEntity
import com.example.sharedexpenses.data.local.RequestType
import com.example.sharedexpenses.data.local.UserDao
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.data.prefs.NotificationLevel
import com.example.sharedexpenses.data.prefs.PreferencesStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

/** F01-F04: mock authentication (NF06) and profile. */
class UserRepository(
    private val userDao: UserDao,
    private val activityDao: ActivityDao,
    private val prefs: PreferencesStore,
    private val sync: SyncRepository,
) {
    val currentUserId: Flow<String?> = prefs.currentUserId
    val notificationLevel: Flow<NotificationLevel> = prefs.notificationLevel

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = prefs.currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else userDao.observe(id)
    }

    val activity: Flow<List<ActivityEntity>> = activityDao.observeAll()

    /** Demo auth: any seeded/registered email with a non-empty password. */
    suspend fun logIn(email: String, password: String): Boolean {
        val user = userDao.findByEmail(email.trim()) ?: return false
        if (password.isEmpty() || password != user.password) return false
        prefs.setCurrentUser(user.id)
        return true
    }

    /** @return false if the email is already used. */
    suspend fun register(name: String, email: String, phone: String, password: String): Boolean {
        if (userDao.findByEmail(email.trim()) != null) return false
        val user = UserEntity(UUID.randomUUID().toString(), name.trim(), email.trim(), phone.trim(), password)
        userDao.upsert(user)
        prefs.setCurrentUser(user.id)
        sync.enqueue(RequestType.UPDATE_PROFILE, user.id, user.name)
        return true
    }

    suspend fun updateProfile(name: String, email: String, phone: String) {
        val id = prefs.currentUserIdNow() ?: return
        val user = userDao.get(id) ?: return
        userDao.upsert(user.copy(name = name.trim(), email = email.trim(), phone = phone.trim()))
        sync.enqueue(RequestType.UPDATE_PROFILE, id, name.trim())
    }

    suspend fun setNotificationLevel(level: NotificationLevel) = prefs.setNotificationLevel(level)

    suspend fun logOut() = prefs.setCurrentUser(null)

    /** NF06: dummy data, inserted the first time the app starts. */
    suspend fun seedIfEmpty(seed: suspend () -> Unit) {
        if (userDao.count() == 0) seed()
    }
}
