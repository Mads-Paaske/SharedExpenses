package com.example.sharedexpenses.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.data.repository.GroupRepository
import com.example.sharedexpenses.data.repository.SyncRepository
import com.example.sharedexpenses.data.repository.UserRepository
import com.example.sharedexpenses.domain.DebtCalculator
import com.example.sharedexpenses.domain.ExpenseItem
import com.example.sharedexpenses.ui.navigation.Routes
import com.example.sharedexpenses.ui.parseDkk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseUiState(
    val title: String = "",
    val amountText: String = "",
    val payerId: String? = null,
    val splitAll: Boolean = true,
    val selectedIds: Set<String> = emptySet(),
    val hasReceipt: Boolean = false,
    val members: List<UserEntity> = emptyList(),
    val showErrors: Boolean = false,
) {
    val amountOre: Long? get() = parseDkk(amountText)
    val participants: List<String> get() = if (splitAll) members.map { it.id } else members.map { it.id }.filter { it in selectedIds }
    val shares: Map<String, Long> get() {
        val amount = amountOre ?: return emptyMap()
        return if (participants.isEmpty()) emptyMap() else DebtCalculator.splitEvenly(amount, participants)
    }
    val payerName: String get() = members.firstOrNull { it.id == payerId }?.name ?: ""
    val canSave: Boolean get() = title.isNotBlank() && amountOre != null && payerId != null && participants.isNotEmpty()
}

/**
 * Shared by the whole add-expense flow (add, choose payer, split, receipt).
 * Scoped to the nested nav graph, so the form survives moving between those screens.
 */
class AddExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[Routes.ARG_GROUP_ID])

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val members = groupRepository.observeGroup(groupId).filterNotNull().first().members
            val me = userRepository.currentUserId.first()
            _uiState.update { it.copy(members = members, payerId = it.payerId ?: me, selectedIds = members.map { m -> m.id }.toSet()) }
        }
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setAmount(value: String) = _uiState.update { it.copy(amountText = value) }
    fun setPayer(id: String) = _uiState.update { it.copy(payerId = id) }
    fun setSplitAll(all: Boolean) = _uiState.update { it.copy(splitAll = all) }
    fun toggleSelected(id: String) = _uiState.update {
        it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
    }
    fun setReceipt(attached: Boolean) = _uiState.update { it.copy(hasReceipt = attached) }

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val me = userRepository.currentUser.filterNotNull().first()
            val id = groupRepository.addExpense(
                groupId = groupId,
                title = state.title.trim(),
                amountOre = state.amountOre!!,
                payerId = state.payerId!!,
                shares = state.shares,
                hasReceipt = state.hasReceipt,
                actor = me,
            )
            onSaved(id)
        }
    }
}

/** Expense details (lo-fi 11) and the save status screen (lo-fi 19, 23, 24, 25). */
class ExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val expenseId: String = checkNotNull(savedStateHandle[Routes.ARG_EXPENSE_ID])

    val expense: StateFlow<ExpenseItem?> = groupRepository.observeExpense(expenseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val online: StateFlow<Boolean> = syncRepository.online
    val simulatedOffline: StateFlow<Boolean> = syncRepository.simulatedOffline

    /** True once this expense has failed at least once - so "Back online" text can be shown (lo-fi 25). */
    val hadFailure: StateFlow<Boolean> = syncRepository.requests
        .map { list -> list.any { it.refId == expenseId && it.attempts > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun simulateNetworkFailure() = syncRepository.simulateNetworkFailure()
    fun restoreConnection() = syncRepository.restoreConnection()
}
