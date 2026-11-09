package com.example.sharedexpenses.ui.group

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.components.AppScreen
import com.example.sharedexpenses.ui.components.InfoCard
import com.example.sharedexpenses.ui.components.NavRow
import com.example.sharedexpenses.ui.components.PrimaryButton
import com.example.sharedexpenses.ui.components.SecondaryButton
import com.example.sharedexpenses.ui.components.SectionLabel
import com.example.sharedexpenses.ui.components.SyncBanner
import com.example.sharedexpenses.ui.components.ValueRow
import com.example.sharedexpenses.ui.components.syncStatusColor
import com.example.sharedexpenses.ui.components.syncStatusText
import com.example.sharedexpenses.ui.formatDkk
import com.example.sharedexpenses.ui.home.balanceText

/** Lo-fi 06 - group overview. */
@Composable
fun GroupOverviewScreen(
    onBack: () -> Unit,
    onExpenses: () -> Unit,
    onBalances: () -> Unit,
    onMembers: () -> Unit,
    onEditGroup: () -> Unit,
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    groupViewModel: GroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val detail by groupViewModel.group.collectAsStateWithLifecycle()
    val me by groupViewModel.currentUserId.collectAsStateWithLifecycle()
    val requests by groupViewModel.requests.collectAsStateWithLifecycle()
    val online by groupViewModel.online.collectAsStateWithLifecycle()
    val group = detail ?: return
    val myBalance = group.netBalances[me] ?: 0L

    AppScreen(
        title = group.group.name,
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.add_expense_button), onAddExpense) },
    ) {
        if (group.group.description.isNotBlank()) Text(group.group.description)
        Text(group.members.joinToString { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InfoCard(title = stringResource(R.string.your_balance)) { Text(balanceText(myBalance)) }
        SyncBanner(online = online, requests = requests, onRetry = groupViewModel::retryNow)
        NavRow(stringResource(R.string.expenses), onExpenses)
        NavRow(stringResource(R.string.balances_row, balanceText(myBalance)), onBalances)
        NavRow(pluralStringResource(R.plurals.members_count, group.members.size, group.members.size), onMembers)
        NavRow(stringResource(R.string.group_settings), onEditGroup)
        SectionLabel(stringResource(R.string.recent_expenses))
        group.expenses.take(5).forEach { expense ->
            TextButton(onClick = { onExpenseClick(expense.id) }, modifier = Modifier.fillMaxWidth()) {
                ValueRow(
                    label = expense.title,
                    value = formatDkk(expense.amountOre),
                    sub = stringResource(
                        R.string.expense_sub,
                        expense.payer?.name ?: "?",
                        pluralStringResource(R.plurals.people_count, expense.shares.size, expense.shares.size),
                        syncStatusText(expense.syncStatus),
                    ),
                )
            }
        }
    }
}

/** Lo-fi 08 - Expense list / transaction history. */
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    groupViewModel: GroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val detail by groupViewModel.group.collectAsStateWithLifecycle()
    val group = detail ?: return

    AppScreen(
        title = stringResource(R.string.expenses_history),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.add_expense_button), onAddExpense) },
    ) {
        Text(stringResource(R.string.all_activity_in, group.group.name), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (group.expenses.isEmpty()) Text(stringResource(R.string.no_expenses))
        group.expenses.forEach { expense ->
            SecondaryButton(
                text = stringResource(R.string.title_amount, expense.title, formatDkk(expense.amountOre)),
                onClick = { onExpenseClick(expense.id) },
            )
            Text(
                stringResource(
                    R.string.expense_sub,
                    expense.payer?.name ?: "?",
                    pluralStringResource(R.plurals.people_count, expense.shares.size, expense.shares.size),
                    syncStatusText(expense.syncStatus),
                ),
                color = syncStatusColor(expense.syncStatus),
            )
        }
    }
}

/** Lo-fi 12 - Balances & debts (F11). */
@Composable
fun BalancesScreen(
    onBack: () -> Unit,
    onRemind: (String) -> Unit,
    groupViewModel: GroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val detail by groupViewModel.group.collectAsStateWithLifecycle()
    val me by groupViewModel.currentUserId.collectAsStateWithLifecycle()
    val group = detail ?: return
    val owedToMe = group.settlements.filter { it.toId == me }
    val iOwe = group.settlements.filter { it.fromId == me }
    val unsynced = group.expenses.count { it.syncStatus != SyncStatus.SYNCED }

    AppScreen(title = stringResource(R.string.balances_debts), onBack = onBack) {
        InfoCard(title = stringResource(R.string.your_position)) {
            Text(stringResource(R.string.you_are_owed, formatDkk(owedToMe.sumOf { it.amountOre })))
            Text(stringResource(R.string.you_owe, formatDkk(iOwe.sumOf { it.amountOre })))
        }
        if (unsynced > 0) {
            Text(
                pluralStringResource(R.plurals.excluded_unsynced, unsynced, unsynced),
                color = MaterialTheme.colorScheme.error,
            )
        }
        SectionLabel(stringResource(R.string.suggested_settlements))
        if (group.settlements.isEmpty()) Text(stringResource(R.string.everyone_settled))
        group.settlements.forEach { s ->
            when {
                s.toId == me -> SecondaryButton(
                    text = stringResource(R.string.owes_you_remind, group.memberName(s.fromId), formatDkk(s.amountOre)),
                    onClick = { onRemind(s.fromId) },
                )
                s.fromId == me -> Text(stringResource(R.string.you_owe_person, group.memberName(s.toId), formatDkk(s.amountOre)))
                else -> Text(
                    stringResource(R.string.person_owes_person, group.memberName(s.fromId), group.memberName(s.toId), formatDkk(s.amountOre)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        InfoCard(title = stringResource(R.string.how_calculated)) {
            Text(stringResource(R.string.how_calculated_body))
            group.members.forEach { member ->
                val net = group.netBalances[member.id] ?: 0L
                Text(
                    when {
                        net > 0 -> stringResource(R.string.net_plus, member.name, formatDkk(net))
                        net < 0 -> stringResource(R.string.net_minus, member.name, formatDkk(net))
                        else -> stringResource(R.string.net_zero, member.name)
                    }
                )
            }
        }
    }
}

/** Lo-fi 13 + 22 - Send payment reminder, then "Sent". */
@Composable
fun ReminderScreen(
    onBack: () -> Unit,
    reminderViewModel: ReminderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by reminderViewModel.uiState.collectAsStateWithLifecycle()
    val sent by reminderViewModel.sentStatus.collectAsStateWithLifecycle()
    val name = state.to?.name ?: ""

    if (sent == null) {
        AppScreen(
            title = stringResource(R.string.send_reminder),
            onBack = onBack,
            bottomButton = { PrimaryButton(stringResource(R.string.send_reminder), reminderViewModel::send, enabled = state.to != null) },
        ) {
            Text(stringResource(R.string.reminder_to, name, state.groupName))
            Text(stringResource(R.string.outstanding, formatDkk(state.amountOre)), style = MaterialTheme.typography.headlineSmall)
            InfoCard(title = stringResource(R.string.message_preview)) {
                Text(stringResource(R.string.reminder_message, name.substringBefore(' '), state.me?.name?.substringBefore(' ') ?: "", formatDkk(state.amountOre), state.groupName))
            }
        }
    } else {
        AppScreen(
            title = stringResource(R.string.reminder_sent_title),
            onBack = onBack,
            bottomButton = { PrimaryButton(stringResource(R.string.back_to_balances), onBack) },
        ) {
            InfoCard(
                title = when (sent) {
                    SyncStatus.SYNCED -> stringResource(R.string.reminder_sent_title)
                    SyncStatus.FAILED -> stringResource(R.string.reminder_queued_title)
                    else -> stringResource(R.string.reminder_sending_title)
                },
                isError = sent == SyncStatus.FAILED,
            ) {
                Text(
                    when (sent) {
                        SyncStatus.SYNCED -> stringResource(R.string.reminder_sent_body, name, formatDkk(state.amountOre))
                        SyncStatus.FAILED -> stringResource(R.string.reminder_queued_body)
                        else -> stringResource(R.string.reminder_sending_body)
                    }
                )
            }
        }
    }
}

/** Lo-fi 05 - Create group. */
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    createGroupViewModel: CreateGroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val users by createGroupViewModel.allUsers.collectAsStateWithLifecycle()
    val me by createGroupViewModel.currentUserId.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }

    AppScreen(
        title = stringResource(R.string.create_group),
        onBack = onBack,
        bottomButton = {
            PrimaryButton(
                stringResource(R.string.create_group),
                onClick = { createGroupViewModel.create(name, description, selected, onCreated) },
                enabled = name.isNotBlank(),
            )
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_group_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.field_description)) }, modifier = Modifier.fillMaxWidth())
        MemberPicker(users = users, me = me, selected = selected, onChange = { selected = it })
    }
}

/** Lo-fi 07 - Group members (F05 add / remove). */
@Composable
fun MembersScreen(
    onBack: () -> Unit,
    groupViewModel: GroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val detail by groupViewModel.group.collectAsStateWithLifecycle()
    val users by groupViewModel.allUsers.collectAsStateWithLifecycle()
    val me by groupViewModel.currentUserId.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<List<String>?>(null) }
    // Start from the current members once they are loaded.
    LaunchedEffect(detail) {
        if (selected == null && detail != null) selected = detail!!.members.map { it.id }
    }
    val current = selected ?: return

    AppScreen(
        title = stringResource(R.string.group_members),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.save_members), { groupViewModel.saveMembers(current, onBack) }) },
    ) {
        MemberPicker(users = users, me = me, selected = current, onChange = { selected = it })
        Text(stringResource(R.string.remove_member_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Lo-fi 18 - Edit group (F06 name + description). */
@Composable
fun EditGroupScreen(
    onBack: () -> Unit,
    groupViewModel: GroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val detail by groupViewModel.group.collectAsStateWithLifecycle()
    val group = detail?.group ?: return
    var name by rememberSaveable { mutableStateOf(group.name) }
    var description by rememberSaveable { mutableStateOf(group.description) }

    AppScreen(
        title = stringResource(R.string.edit_group),
        onBack = onBack,
        bottomButton = {
            PrimaryButton(stringResource(R.string.save_changes), { groupViewModel.updateGroup(name, description, onBack) }, enabled = name.isNotBlank())
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_group_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.field_description)) }, modifier = Modifier.fillMaxWidth())
    }
}

/** Search + checkbox list of people. The current user is always in the group. */
@Composable
private fun MemberPicker(users: List<UserEntity>, me: String?, selected: List<String>, onChange: (List<String>) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        query, { query = it },
        label = { Text(stringResource(R.string.find_people)) },
        placeholder = { Text(stringResource(R.string.search_name_email)) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    SectionLabel(stringResource(R.string.selected_members))
    users
        .filter { query.isBlank() || it.name.contains(query, true) || it.email.contains(query, true) }
        .forEach { user ->
            val isMe = user.id == me
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isMe || user.id in selected,
                    enabled = !isMe,
                    onCheckedChange = { checked -> onChange(if (checked) selected + user.id else selected - user.id) },
                )
                Text(if (isMe) stringResource(R.string.name_you, user.name) else user.name)
            }
        }
}
