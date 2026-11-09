package com.example.sharedexpenses.ui.activity

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.ActivityEntity
import com.example.sharedexpenses.data.local.ActivityKind
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.repository.SyncRepository
import com.example.sharedexpenses.data.repository.UserRepository
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.components.AppScreen
import com.example.sharedexpenses.ui.components.InfoCard
import com.example.sharedexpenses.ui.components.NavRow
import com.example.sharedexpenses.ui.components.SectionLabel
import com.example.sharedexpenses.ui.components.ValueRow
import com.example.sharedexpenses.ui.components.syncStatusColor
import com.example.sharedexpenses.ui.components.syncStatusText
import com.example.sharedexpenses.ui.formatDkk
import com.example.sharedexpenses.ui.formatTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ActivityViewModel(userRepository: UserRepository, syncRepository: SyncRepository) : ViewModel() {
    val activity: StateFlow<List<ActivityEntity>> = userRepository.activity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val requests: StateFlow<List<OutboxEntity>> = syncRepository.requests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Lo-fi 14 - Activity / Notifications (F12). Also lists the requests and their status (NF04). */
@Composable
fun ActivityScreen(
    onNotificationSettings: () -> Unit,
    onMyGroups: () -> Unit,
    activityViewModel: ActivityViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val activity by activityViewModel.activity.collectAsStateWithLifecycle()
    val requests by activityViewModel.requests.collectAsStateWithLifecycle()
    val notSynced = requests.filter { it.status != SyncStatus.SYNCED }

    AppScreen(title = stringResource(R.string.activity)) {
        if (notSynced.isNotEmpty()) {
            SectionLabel(stringResource(R.string.waiting_to_sync))
            notSynced.forEach { request ->
                Text(
                    stringResource(R.string.request_status, request.label, syncStatusText(request.status)),
                    color = syncStatusColor(request.status),
                )
            }
        }
        SectionLabel(stringResource(R.string.recent_activity))
        if (activity.isEmpty()) Text(stringResource(R.string.no_activity))
        activity.forEach { item ->
            if (item.kind == ActivityKind.SYNC_COMPLETE) {
                InfoCard(title = stringResource(R.string.sync_complete)) {
                    Text(stringResource(R.string.sync_complete_body, item.title, formatTime(item.createdAt)))
                }
            } else {
                ValueRow(
                    label = activityText(item),
                    value = if (item.amountOre > 0) formatDkk(item.amountOre) else "",
                    sub = stringResource(R.string.group_and_time, item.groupName, formatTime(item.createdAt)),
                )
            }
        }
        NavRow(stringResource(R.string.notification_settings), onNotificationSettings)
        NavRow(stringResource(R.string.my_groups), onMyGroups)
        Text(stringResource(R.string.mock_activity_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun activityText(item: ActivityEntity): String = when (item.kind) {
    ActivityKind.EXPENSE_ADDED -> stringResource(R.string.activity_expense_added, item.actorName, item.title)
    ActivityKind.GROUP_CREATED -> stringResource(R.string.activity_group_created, item.actorName, item.title)
    ActivityKind.MEMBERS_UPDATED -> stringResource(R.string.activity_members_updated, item.actorName)
    ActivityKind.REMINDER_SENT -> stringResource(R.string.activity_reminder_sent, item.actorName, item.title)
    ActivityKind.SYNC_COMPLETE -> stringResource(R.string.sync_complete)
}
