package com.example.sharedexpenses.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** PENDING = saved locally, not yet accepted by the server. FAILED = last attempt failed, retry queued. */
enum class SyncStatus { PENDING, SYNCED, FAILED }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    // Mock authentication (NF06) - never a real password.
    val password: String,
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    /** Amount in øre (1 DKK = 100 øre) so we never round with Double. */
    val amountOre: Long,
    val payerId: String,
    val hasReceipt: Boolean,
    val createdAt: Long,
    val syncStatus: SyncStatus,
)

@Entity(tableName = "expense_shares", primaryKeys = ["expenseId", "userId"])
data class ExpenseShareEntity(
    val expenseId: String,
    val userId: String,
    val shareOre: Long,
)

enum class ActivityKind { EXPENSE_ADDED, GROUP_CREATED, MEMBERS_UPDATED, REMINDER_SENT, SYNC_COMPLETE }

/** One row in the Activity/Notifications feed. Text is built in the UI from kind + fields. */
@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ActivityKind,
    val groupId: String?,
    val groupName: String,
    val actorName: String,
    val title: String,
    val amountOre: Long,
    val createdAt: Long,
)

enum class RequestType { CREATE_GROUP, UPDATE_GROUP, UPDATE_MEMBERS, ADD_EXPENSE, SEND_REMINDER, UPDATE_PROFILE }

/**
 * Outbox: every change that must reach the (mock) server is queued here first.
 * The SyncWorker sends them and retries failed ones in the background (NF01).
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RequestType,
    /** Id of the row the request is about (expense id, group id, ...). */
    val refId: String,
    /** Short label shown to the user, e.g. "Groceries". */
    val label: String,
    val amountOre: Long = 0,
    val status: SyncStatus = SyncStatus.PENDING,
    val attempts: Int = 0,
    val createdAt: Long,
)
