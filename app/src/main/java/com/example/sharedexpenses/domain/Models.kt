package com.example.sharedexpenses.domain

import com.example.sharedexpenses.data.local.ExpenseEntity
import com.example.sharedexpenses.data.local.GroupEntity
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.local.UserEntity

/** Row on "My groups". */
data class GroupSummary(
    val id: String,
    val name: String,
    val memberCount: Int,
    /** Current user's balance in this group. Positive = owed money. */
    val myBalanceOre: Long,
)

data class ExpenseItem(
    val id: String,
    val groupId: String,
    val title: String,
    val amountOre: Long,
    val payerId: String,
    val payer: UserEntity?,
    val shares: Map<String, Long>,
    /** Names of the people in [shares], by user id. */
    val shareNames: Map<String, String>,
    val hasReceipt: Boolean,
    val createdAt: Long,
    val syncStatus: SyncStatus,
)

/** Everything the group screens need, already calculated. */
data class GroupDetail(
    val group: GroupEntity,
    val members: List<UserEntity>,
    val expenses: List<ExpenseItem>,
    /** Only SYNCED expenses count - unsynced ones are "excluded from shared balances until synced" (lo-fi 23). */
    val netBalances: Map<String, Long>,
    val settlements: List<Settlement>,
) {
    fun memberName(id: String): String = members.firstOrNull { it.id == id }?.name ?: "?"
}

fun ExpenseEntity.toSplit(shares: Map<String, Long>) = ExpenseSplit(payerId, amountOre, shares)
