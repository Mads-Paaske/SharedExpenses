package com.example.sharedexpenses.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** F03: "On - all group activity", "Mentions only", "Off". */
enum class NotificationLevel { ALL, MENTIONS, OFF }

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Small key-value storage: who is logged in (survives app restarts) and notification settings. */
class PreferencesStore(private val context: Context) {
    private val userIdKey = stringPreferencesKey("current_user_id")
    private val notificationKey = stringPreferencesKey("notification_level")

    val currentUserId: Flow<String?> = context.dataStore.data.map { it[userIdKey] }

    val notificationLevel: Flow<NotificationLevel> = context.dataStore.data.map { prefs ->
        prefs[notificationKey]?.let { NotificationLevel.valueOf(it) } ?: NotificationLevel.ALL
    }

    suspend fun currentUserIdNow(): String? = currentUserId.first()

    suspend fun setCurrentUser(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(userIdKey) else prefs[userIdKey] = id
        }
    }

    suspend fun setNotificationLevel(level: NotificationLevel) {
        context.dataStore.edit { it[notificationKey] = level.name }
    }
}
