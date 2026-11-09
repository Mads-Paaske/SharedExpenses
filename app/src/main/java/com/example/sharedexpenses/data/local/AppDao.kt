package com.example.sharedexpenses.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun observe(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun get(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)
}

@Dao
interface GroupDao {
    @Query("SELECT g.* FROM groups g JOIN group_members m ON m.groupId = g.id WHERE m.userId = :userId ORDER BY g.createdAt")
    fun observeGroupsFor(userId: String): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun observe(id: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun get(id: String): GroupEntity?

    @Query("SELECT u.* FROM users u JOIN group_members m ON m.userId = u.id WHERE m.groupId = :groupId ORDER BY u.name")
    fun observeMembers(groupId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM group_members")
    fun observeAllMemberships(): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearMembers(groupId: String)

    @Transaction
    suspend fun replaceMembers(groupId: String, userIds: List<String>) {
        clearMembers(groupId)
        addMembers(userIds.map { GroupMemberEntity(groupId, it) })
    }
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeForGroup(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun observe(id: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    fun observeShares(expenseId: String): Flow<List<ExpenseShareEntity>>

    @Query("SELECT * FROM expense_shares")
    fun observeAllShares(): Flow<List<ExpenseShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<ExpenseShareEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Transaction
    suspend fun insertWithShares(expense: ExpenseEntity, shares: List<ExpenseShareEntity>) {
        upsert(expense)
        insertShares(shares)
    }

    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: SyncStatus)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Insert
    suspend fun insert(activity: ActivityEntity)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE status != 'SYNCED' ORDER BY createdAt")
    suspend fun unsent(): List<OutboxEntity>

    @Query("SELECT * FROM outbox ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox WHERE id = :id")
    fun observe(id: Long): Flow<OutboxEntity?>

    @Insert
    suspend fun insert(item: OutboxEntity): Long

    @Update
    suspend fun update(item: OutboxEntity)
}
