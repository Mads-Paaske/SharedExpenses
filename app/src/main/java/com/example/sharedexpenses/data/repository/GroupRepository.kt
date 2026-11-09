package com.example.sharedexpenses.data.repository

import com.example.sharedexpenses.data.local.ActivityDao
import com.example.sharedexpenses.data.local.ActivityEntity
import com.example.sharedexpenses.data.local.ActivityKind
import com.example.sharedexpenses.data.local.ExpenseDao
import com.example.sharedexpenses.data.local.ExpenseEntity
import com.example.sharedexpenses.data.local.ExpenseShareEntity
import com.example.sharedexpenses.data.local.GroupDao
import com.example.sharedexpenses.data.local.GroupEntity
import com.example.sharedexpenses.data.local.RequestType
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.local.UserDao
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.domain.DebtCalculator
import com.example.sharedexpenses.domain.ExpenseItem
import com.example.sharedexpenses.domain.ExpenseSplit
import com.example.sharedexpenses.domain.GroupDetail
import com.example.sharedexpenses.domain.GroupSummary
import com.example.sharedexpenses.domain.toSplit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

/**
 * Groups, members, expenses and reminders.
 * Every write: 1) goes to the local database right away (the UI never waits for the network, NF02),
 * 2) is queued in the outbox so the SyncWorker sends it in the background (NF01).
 */
class GroupRepository(
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val userDao: UserDao,
    private val activityDao: ActivityDao,
    private val sync: SyncRepository,
) {
    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    fun observeGroupsFor(userId: String): Flow<List<GroupSummary>> = combine(
        groupDao.observeGroupsFor(userId),
        groupDao.observeAllMemberships(),
        expenseDao.observeAll(),
        expenseDao.observeAllShares(),
    ) { groups, memberships, expenses, shares ->
        val sharesByExpense = shares.groupBy { it.expenseId }
        groups.map { group ->
            val synced = expenses.filter { it.groupId == group.id && it.syncStatus == SyncStatus.SYNCED }
            val net = DebtCalculator.netBalances(
                synced.map { e -> e.toSplit(sharesByExpense[e.id].orEmpty().associate { it.userId to it.shareOre }) }
            )
            GroupSummary(
                id = group.id,
                name = group.name,
                memberCount = memberships.count { it.groupId == group.id },
                myBalanceOre = net[userId] ?: 0L,
            )
        }
    }

    fun observeGroup(groupId: String): Flow<GroupDetail?> = combine(
        groupDao.observe(groupId),
        groupDao.observeMembers(groupId),
        expenseDao.observeForGroup(groupId),
        expenseDao.observeAllShares(),
        userDao.observeAll(),
    ) { group, members, expenses, shares, users ->
        if (group == null) return@combine null
        val sharesByExpense = shares.groupBy { it.expenseId }
            .mapValues { (_, list) -> list.associate { it.userId to it.shareOre } }
        val items = expenses.map { it.toItem(sharesByExpense[it.id].orEmpty(), users) }
        val net = DebtCalculator.netBalances(
            items.filter { it.syncStatus == SyncStatus.SYNCED }
                .map { ExpenseSplit(it.payerId, it.amountOre, it.shares) },
            members.map { it.id },
        )
        GroupDetail(group, members, items, net, DebtCalculator.settle(net))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeExpense(expenseId: String): Flow<ExpenseItem?> =
        expenseDao.observe(expenseId).flatMapLatest { expense ->
            if (expense == null) flowOf(null)
            else combine(expenseDao.observeShares(expenseId), userDao.observeAll()) { shares, users ->
                expense.toItem(shares.associate { it.userId to it.shareOre }, users)
            }
        }

    suspend fun createGroup(name: String, description: String, memberIds: List<String>, actor: UserEntity): String {
        val id = UUID.randomUUID().toString()
        groupDao.upsert(GroupEntity(id, name, description, System.currentTimeMillis()))
        groupDao.replaceMembers(id, (memberIds + actor.id).distinct())
        activity(ActivityKind.GROUP_CREATED, id, name, actor.name, name, 0)
        sync.enqueue(RequestType.CREATE_GROUP, id, name)
        return id
    }

    suspend fun updateGroup(groupId: String, name: String, description: String) {
        val old = groupDao.get(groupId) ?: return
        groupDao.upsert(old.copy(name = name, description = description))
        sync.enqueue(RequestType.UPDATE_GROUP, groupId, name)
    }

    suspend fun updateMembers(groupId: String, memberIds: List<String>, actor: UserEntity) {
        val group = groupDao.get(groupId) ?: return
        groupDao.replaceMembers(groupId, memberIds)
        activity(ActivityKind.MEMBERS_UPDATED, groupId, group.name, actor.name, group.name, 0)
        sync.enqueue(RequestType.UPDATE_MEMBERS, groupId, group.name)
    }

    /** F07 + F09. Saved locally as PENDING; balances include it once the server has it (SYNCED). */
    suspend fun addExpense(
        groupId: String,
        title: String,
        amountOre: Long,
        payerId: String,
        shares: Map<String, Long>,
        hasReceipt: Boolean,
        actor: UserEntity,
    ): String {
        val id = UUID.randomUUID().toString()
        val expense = ExpenseEntity(id, groupId, title, amountOre, payerId, hasReceipt, System.currentTimeMillis(), SyncStatus.PENDING)
        expenseDao.insertWithShares(expense, shares.map { (userId, share) -> ExpenseShareEntity(id, userId, share) })
        val groupName = groupDao.get(groupId)?.name ?: ""
        activity(ActivityKind.EXPENSE_ADDED, groupId, groupName, actor.name, title, amountOre)
        sync.enqueue(RequestType.ADD_EXPENSE, id, title, amountOre)
        return id
    }

    /** F13. Returns the outbox id so the screen can show pending / sent / failed. */
    suspend fun sendReminder(groupId: String, to: UserEntity, amountOre: Long, actor: UserEntity): Long {
        val groupName = groupDao.get(groupId)?.name ?: ""
        activity(ActivityKind.REMINDER_SENT, groupId, groupName, actor.name, to.name, amountOre)
        return sync.enqueue(RequestType.SEND_REMINDER, to.id, to.name, amountOre)
    }

    private suspend fun activity(kind: ActivityKind, groupId: String?, groupName: String, actor: String, title: String, amountOre: Long) {
        activityDao.insert(ActivityEntity(0, kind, groupId, groupName, actor, title, amountOre, System.currentTimeMillis()))
    }

    private fun ExpenseEntity.toItem(shares: Map<String, Long>, users: List<UserEntity>) = ExpenseItem(
        id = id, groupId = groupId, title = title, amountOre = amountOre, payerId = payerId,
        payer = users.firstOrNull { it.id == payerId }, shares = shares,
        shareNames = users.filter { it.id in shares }.associate { it.id to it.name },
        hasReceipt = hasReceipt, createdAt = createdAt, syncStatus = syncStatus,
    )
}
