package com.example.sharedexpenses.data.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sharedexpenses.MainActivity
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.RequestType
import com.example.sharedexpenses.data.prefs.NotificationLevel
import com.example.sharedexpenses.data.prefs.PreferencesStore
import kotlinx.coroutines.flow.first
import com.example.sharedexpenses.ui.formatDkk

/** Posts Android system notifications (F12, F13, NF05). */
class Notifier(private val context: Context, private val prefs: PreferencesStore) : SyncListener {

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_SYNC, context.getString(R.string.channel_sync), NotificationManager.IMPORTANCE_DEFAULT)
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_GROUP, context.getString(R.string.channel_group), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    /** NF05: always shown, notification settings only apply to group activity (lo-fi screen 17). */
    override suspend fun onPreviouslyFailedRequestSynced(request: OutboxEntity) {
        val text = if (request.amountOre > 0) {
            context.getString(R.string.notif_synced_body_amount, request.label, formatDkk(request.amountOre))
        } else {
            context.getString(R.string.notif_synced_body, request.label)
        }
        post(CHANNEL_SYNC, request.id.toInt(), context.getString(R.string.notif_synced_title), text)
    }

    /**
     * F12 + F13 (mock): the server has told the other group members. We confirm it with a notification,
     * filtered by the user's notification setting (F03).
     */
    override suspend fun onRequestSynced(request: OutboxEntity) {
        val level = prefs.notificationLevel.first()
        when (request.type) {
            RequestType.ADD_EXPENSE -> groupActivity(
                level, mentionsYou = false,
                title = context.getString(R.string.notif_expense_title),
                text = context.getString(R.string.notif_expense_body, request.label, formatDkk(request.amountOre)),
            )
            RequestType.SEND_REMINDER -> groupActivity(
                level, mentionsYou = true,
                title = context.getString(R.string.notif_reminder_title),
                text = context.getString(R.string.notif_reminder_body, request.label, formatDkk(request.amountOre)),
            )
            else -> Unit
        }
    }

    /** [mentionsYou] = the activity is directly about the current user (e.g. their reminder). */
    private fun groupActivity(level: NotificationLevel, mentionsYou: Boolean, title: String, text: String) {
        val allowed = when (level) {
            NotificationLevel.ALL -> true
            NotificationLevel.MENTIONS -> mentionsYou
            NotificationLevel.OFF -> false
        }
        if (allowed) post(CHANNEL_GROUP, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), title, text)
    }

    @SuppressLint("MissingPermission") // checked at the top of the function
    private fun post(channel: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_SYNC = "sync"
        const val CHANNEL_GROUP = "group_activity"
    }
}
