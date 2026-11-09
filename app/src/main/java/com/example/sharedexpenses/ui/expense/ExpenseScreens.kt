package com.example.sharedexpenses.ui.expense

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.components.AppScreen
import com.example.sharedexpenses.ui.components.InfoCard
import com.example.sharedexpenses.ui.components.PrimaryButton
import com.example.sharedexpenses.ui.components.SecondaryButton
import com.example.sharedexpenses.ui.components.ValueRow
import com.example.sharedexpenses.ui.components.syncStatusText
import com.example.sharedexpenses.ui.formatDkk
import com.example.sharedexpenses.ui.formatTime

/** Lo-fi 09 / 26 / 27 - Add expense (F07). The ViewModel is passed in from the nested nav graph. */
@Composable
fun AddExpenseScreen(
    addExpenseViewModel: AddExpenseViewModel,
    onBack: () -> Unit,
    onChoosePayer: () -> Unit,
    onSplit: () -> Unit,
    onAttachReceipt: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val state by addExpenseViewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = stringResource(R.string.add_expense),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.save_expense), { addExpenseViewModel.save(onSaved) }) },
    ) {
        OutlinedTextField(
            state.title, addExpenseViewModel::setTitle,
            label = { Text(stringResource(R.string.field_expense_name)) },
            isError = state.showErrors && state.title.isBlank(),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            state.amountText, addExpenseViewModel::setAmount,
            label = { Text(stringResource(R.string.field_amount)) },
            isError = state.showErrors && state.amountOre == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(stringResource(R.string.paid_by, state.payerName), onChoosePayer)
        SecondaryButton(
            if (state.splitAll) {
                stringResource(R.string.split_all, state.members.size)
            } else {
                stringResource(R.string.split_selected, state.participants.size)
            },
            onSplit,
        )
        SecondaryButton(
            if (state.hasReceipt) stringResource(R.string.receipt_attached) else stringResource(R.string.attach_receipt_optional),
            onAttachReceipt,
        )
        val amount = state.amountOre
        if (amount != null && state.participants.isNotEmpty()) {
            Text(stringResource(R.string.equal_share, formatDkk(amount / state.participants.size)))
        }
        if (state.showErrors && !state.canSave) {
            Text(stringResource(R.string.error_expense_form), color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Lo-fi 20 - Choose payer. */
@Composable
fun ChoosePayerScreen(addExpenseViewModel: AddExpenseViewModel, onBack: () -> Unit) {
    val state by addExpenseViewModel.uiState.collectAsStateWithLifecycle()
    AppScreen(
        title = stringResource(R.string.choose_payer),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.done), onBack) },
    ) {
        Text(stringResource(R.string.who_paid, state.title.ifBlank { stringResource(R.string.this_expense) }))
        state.members.forEach { member ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.payerId == member.id, onClick = { addExpenseViewModel.setPayer(member.id) })
                Text(member.name)
            }
        }
    }
}

/** Lo-fi 10 - Select participants / split expense (F09). */
@Composable
fun SplitScreen(addExpenseViewModel: AddExpenseViewModel, onBack: () -> Unit) {
    val state by addExpenseViewModel.uiState.collectAsStateWithLifecycle()
    AppScreen(
        title = stringResource(R.string.split_expense),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.apply_split), onBack, enabled = state.participants.isNotEmpty()) },
    ) {
        state.amountOre?.let { Text(stringResource(R.string.title_amount, state.title, formatDkk(it))) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = state.splitAll, onClick = { addExpenseViewModel.setSplitAll(true) })
            Text(pluralStringResource(R.plurals.all_members_people, state.members.size, state.members.size))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !state.splitAll, onClick = { addExpenseViewModel.setSplitAll(false) })
            Text(stringResource(R.string.selected_members))
        }
        if (!state.splitAll) {
            InfoCard {
                state.members.forEach { member ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(member.id in state.selectedIds, { addExpenseViewModel.toggleSelected(member.id) })
                        Text(member.name, modifier = Modifier.weight(1f))
                        Text(formatDkk(state.shares[member.id] ?: 0))
                    }
                }
            }
        }
        if (state.participants.isEmpty()) {
            Text(stringResource(R.string.select_one_participant), color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Lo-fi 21 - Attach receipt (F08, optional - mock image only). */
@Composable
fun AttachReceiptScreen(addExpenseViewModel: AddExpenseViewModel, onBack: () -> Unit) {
    val state by addExpenseViewModel.uiState.collectAsStateWithLifecycle()
    AppScreen(
        title = stringResource(R.string.attach_receipt),
        onBack = onBack,
        bottomButton = {
            PrimaryButton(stringResource(R.string.use_mock_receipt), { addExpenseViewModel.setReceipt(true); onBack() })
        },
    ) {
        InfoCard(title = stringResource(R.string.receipt_preview)) {
            Text(if (state.hasReceipt) stringResource(R.string.mock_receipt_image) else stringResource(R.string.no_receipt))
            Text(stringResource(R.string.no_camera_note))
        }
        if (state.hasReceipt) SecondaryButton(stringResource(R.string.remove_receipt), { addExpenseViewModel.setReceipt(false) })
    }
}

/** Lo-fi 11 - Expense details. */
@Composable
fun ExpenseDetailsScreen(
    onBack: () -> Unit,
    expenseViewModel: ExpenseViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val expense by expenseViewModel.expense.collectAsStateWithLifecycle()
    val item = expense ?: return

    AppScreen(
        title = stringResource(R.string.expense_details),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.back_to_history), onBack) },
    ) {
        Text(stringResource(R.string.title_amount, item.title, formatDkk(item.amountOre)), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.paid_by_on, item.payer?.name ?: "?", formatTime(item.createdAt)))
        InfoCard(title = stringResource(R.string.split_title)) {
            item.shares.forEach { (userId, share) ->
                ValueRow(item.shareNames[userId] ?: "?", formatDkk(share))
            }
        }
        InfoCard(title = stringResource(R.string.receipt)) {
            Text(if (item.hasReceipt) stringResource(R.string.mock_receipt_image) else stringResource(R.string.no_receipt))
        }
        Text(
            if (item.syncStatus == SyncStatus.SYNCED) stringResource(R.string.saved_members_notified) else syncStatusText(item.syncStatus),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Lo-fi 19, 23, 24, 25 - one screen that follows the saved expense live (NF04):
 * pending -> failed (retry queued, keeps working offline) -> saved ("back online").
 */
@Composable
fun ExpenseStatusScreen(
    onContinue: (groupId: String) -> Unit,
    expenseViewModel: ExpenseViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val expense by expenseViewModel.expense.collectAsStateWithLifecycle()
    val simulatedOffline by expenseViewModel.simulatedOffline.collectAsStateWithLifecycle()
    val online by expenseViewModel.online.collectAsStateWithLifecycle()
    val hadFailure by expenseViewModel.hadFailure.collectAsStateWithLifecycle()
    val item = expense ?: return
    val amount = formatDkk(item.amountOre)

    when (item.syncStatus) {
        SyncStatus.PENDING -> AppScreen(
            title = if (online) stringResource(R.string.expense_pending) else stringResource(R.string.offline_retry_queued),
            bottomButton = { PrimaryButton(stringResource(R.string.continue_to_group), { onContinue(item.groupId) }) },
        ) {
            // Really offline: WorkManager waits for a connection before it tries (NetworkType.CONNECTED).
            if (!online) {
                InfoCard(title = stringResource(R.string.offline_showing_saved)) {
                    Text(stringResource(R.string.offline_saved_body))
                    Text(stringResource(R.string.queued_until_online))
                }
            }
            InfoCard(title = stringResource(R.string.saving_expense)) {
                Text(stringResource(R.string.title_amount, item.title, amount))
                Text(stringResource(R.string.pending_keep_using))
            }
            SecondaryButton(stringResource(R.string.demo_simulate_failure), expenseViewModel::simulateNetworkFailure, enabled = !simulatedOffline)
        }

        SyncStatus.FAILED -> AppScreen(
            title = if (online) stringResource(R.string.background_retry) else stringResource(R.string.offline_retry_queued),
            bottomButton = { PrimaryButton(stringResource(R.string.browse_saved_history), { onContinue(item.groupId) }) },
        ) {
            if (!online) {
                InfoCard(title = stringResource(R.string.offline_showing_saved)) {
                    Text(stringResource(R.string.offline_saved_body))
                }
            }
            InfoCard(title = stringResource(R.string.could_not_sync), isError = true) {
                Text(stringResource(R.string.title_amount, item.title, amount))
                Text(stringResource(R.string.could_not_sync_body))
            }
            if (simulatedOffline) {
                SecondaryButton(stringResource(R.string.demo_connection_restored), expenseViewModel::restoreConnection)
            } else {
                Text(stringResource(R.string.reconnecting_body, item.title))
            }
        }

        SyncStatus.SYNCED -> AppScreen(
            title = stringResource(R.string.expense_saved),
            bottomButton = { PrimaryButton(stringResource(R.string.view_expenses), { onContinue(item.groupId) }) },
        ) {
            InfoCard(title = if (hadFailure) stringResource(R.string.back_online_saved) else stringResource(R.string.expense_saved)) {
                Text(
                    if (hadFailure) stringResource(R.string.previously_failed_synced, item.title, amount)
                    else stringResource(R.string.title_amount, item.title, amount)
                )
                Text(stringResource(R.string.balances_updated))
            }
        }
    }
}
