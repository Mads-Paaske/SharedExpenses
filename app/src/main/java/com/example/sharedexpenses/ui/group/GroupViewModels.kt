package com.example.sharedexpenses.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.data.repository.GroupRepository
import com.example.sharedexpenses.data.repository.SyncRepository
import com.example.sharedexpenses.data.repository.UserRepository
import com.example.sharedexpenses.domain.GroupDetail
import com.example.sharedexpenses.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared by the group overview, members, edit group, expenses and balances screens. */
class GroupViewModel(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    val groupId: String = checkNotNull(savedStateHandle[Routes.ARG_GROUP_ID])

    val group: StateFlow<GroupDetail?> = groupRepository.observeGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentUserId: StateFlow<String?> = userRepository.currentUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allUsers: StateFlow<List<UserEntity>> = groupRepository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val requests: StateFlow<List<OutboxEntity>> = syncRepository.requests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val online: StateFlow<Boolean> = syncRepository.online

    fun retryNow() = syncRepository.retryNow()

    fun updateGroup(name: String, description: String, onDone: () -> Unit) {
        viewModelScope.launch {
            groupRepository.updateGroup(groupId, name.trim(), description.trim())
            onDone()
        }
    }

    fun saveMembers(memberIds: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            val me = userRepository.currentUser.filterNotNull().first()
            groupRepository.updateMembers(groupId, (memberIds + me.id).distinct(), me)
            onDone()
        }
    }
}

/** Lo-fi 05 - create group (F05 + F06). */
class CreateGroupViewModel(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    val allUsers: StateFlow<List<UserEntity>> = groupRepository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentUserId: StateFlow<String?> = userRepository.currentUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, description: String, memberIds: List<String>, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val me = userRepository.currentUser.filterNotNull().first()
            onCreated(groupRepository.createGroup(name.trim(), description.trim(), memberIds, me))
        }
    }
}

/** Lo-fi 13 + 22 - send a payment reminder (F13). */
class ReminderViewModel(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[Routes.ARG_GROUP_ID])
    private val toUserId: String = checkNotNull(savedStateHandle[Routes.ARG_USER_ID])

    data class UiState(
        val groupName: String = "",
        val to: UserEntity? = null,
        val amountOre: Long = 0,
        val me: UserEntity? = null,
    )

    val uiState: StateFlow<UiState> = combine(
        groupRepository.observeGroup(groupId), userRepository.currentUser,
    ) { group, me ->
        val owed = group?.settlements?.firstOrNull { it.fromId == toUserId && it.toId == me?.id }?.amountOre ?: 0
        UiState(group?.group?.name ?: "", group?.members?.firstOrNull { it.id == toUserId }, owed, me)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private val _sentStatus = MutableStateFlow<SyncStatus?>(null)
    /** null = not sent yet; afterwards the live status of the request (NF04). */
    val sentStatus: StateFlow<SyncStatus?> = _sentStatus.asStateFlow()

    fun send() {
        val state = uiState.value
        val to = state.to ?: return
        val me = state.me ?: return
        viewModelScope.launch {
            val requestId = groupRepository.sendReminder(groupId, to, state.amountOre, me)
            _sentStatus.value = SyncStatus.PENDING
            syncRepository.observeRequest(requestId).collect { request ->
                if (request != null) _sentStatus.value = request.status
            }
        }
    }
}
