package com.example.sharedexpenses.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.repository.GroupRepository
import com.example.sharedexpenses.data.repository.SyncRepository
import com.example.sharedexpenses.data.repository.UserRepository
import com.example.sharedexpenses.domain.GroupSummary
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.components.AppScreen
import com.example.sharedexpenses.ui.components.InfoCard
import com.example.sharedexpenses.ui.components.PrimaryButton
import com.example.sharedexpenses.ui.components.SecondaryButton
import com.example.sharedexpenses.ui.components.SectionLabel
import com.example.sharedexpenses.ui.components.SyncBanner
import com.example.sharedexpenses.ui.formatDkk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<GroupSummary>> = userRepository.currentUserId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else groupRepository.observeGroupsFor(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val requests: StateFlow<List<OutboxEntity>> = syncRepository.requests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val online: StateFlow<Boolean> = syncRepository.online

    fun retryNow() = syncRepository.retryNow()
}

/** Lo-fi 04 - Home / My groups. */
@Composable
fun HomeScreen(
    onGroupClick: (String) -> Unit,
    onCreateGroup: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val groups by homeViewModel.groups.collectAsStateWithLifecycle()
    val requests by homeViewModel.requests.collectAsStateWithLifecycle()
    val online by homeViewModel.online.collectAsStateWithLifecycle()
    val total = groups.sumOf { it.myBalanceOre }

    AppScreen(
        title = stringResource(R.string.my_groups),
        bottomButton = { PrimaryButton(stringResource(R.string.create_group_button), onCreateGroup) },
    ) {
        InfoCard(title = stringResource(R.string.across_groups)) { Text(balanceText(total)) }
        SyncBanner(online = online, requests = requests, onRetry = homeViewModel::retryNow)
        SectionLabel(stringResource(R.string.your_groups))
        if (groups.isEmpty()) {
            Text(stringResource(R.string.no_groups), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        groups.forEach { group ->
            SecondaryButton(
                text = stringResource(
                    R.string.group_row,
                    group.name,
                    pluralStringResource(R.plurals.members_count, group.memberCount, group.memberCount),
                ),
                onClick = { onGroupClick(group.id) },
            )
            Text(balanceText(group.myBalanceOre), color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** "You are owed 120 DKK" / "You owe 20 DKK" / "You are settled up". */
@Composable
fun balanceText(balanceOre: Long): String = when {
    balanceOre > 0 -> stringResource(R.string.you_are_owed, formatDkk(balanceOre))
    balanceOre < 0 -> stringResource(R.string.you_owe, formatDkk(balanceOre))
    else -> stringResource(R.string.settled_up)
}
