package com.example.sharedexpenses

import com.example.sharedexpenses.data.local.ActivityEntity
import com.example.sharedexpenses.data.local.ActivityKind
import com.example.sharedexpenses.data.local.AppDatabase
import com.example.sharedexpenses.data.local.ExpenseEntity
import com.example.sharedexpenses.data.local.ExpenseShareEntity
import com.example.sharedexpenses.data.local.GroupEntity
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.domain.DebtCalculator

/**
 * NF06: dummy data matching the lo-fi (Alex, Sam, Mia, "Weekend trip").
 * Log in with alex@example.com / password.
 */
object SeedData {
    const val DEMO_PASSWORD = "password"

    val alex = UserEntity("u_alex", "Alex Morgan", "alex@example.com", "+45 12 34 56 78", DEMO_PASSWORD)
    val sam = UserEntity("u_sam", "Sam Lee", "sam@example.com", "+45 22 33 44 55", DEMO_PASSWORD)
    val mia = UserEntity("u_mia", "Mia Chen", "mia@example.com", "+45 33 44 55 66", DEMO_PASSWORD)
    val taylor = UserEntity("u_taylor", "Taylor Kim", "taylor@example.com", "+45 44 55 66 77", DEMO_PASSWORD)

    suspend fun insert(db: AppDatabase) {
        val now = System.currentTimeMillis()
        listOf(alex, sam, mia, taylor).forEach { db.userDao().upsert(it) }

        val trip = GroupEntity("g_trip", "Weekend trip", "Shared costs for our weekend away", now - 3 * DAY)
        db.groupDao().upsert(trip)
        db.groupDao().replaceMembers(trip.id, listOf(alex.id, sam.id, mia.id))

        val everyone = listOf(alex.id, sam.id, mia.id)
        expense(db, "e_dinner", trip.id, "Dinner", 300_00, alex.id, everyone, now - 2 * DAY)
        expense(db, "e_taxi", trip.id, "Taxi", 120_00, sam.id, everyone, now - DAY)

        val flat = GroupEntity("g_flat", "Flatmates", "Rent, groceries and bills", now - 10 * DAY)
        db.groupDao().upsert(flat)
        db.groupDao().replaceMembers(flat.id, listOf(alex.id, taylor.id))
        expense(db, "e_internet", flat.id, "Internet", 299_00, taylor.id, listOf(alex.id, taylor.id), now - 5 * DAY)

        val activity = db.activityDao()
        activity.insert(ActivityEntity(0, ActivityKind.EXPENSE_ADDED, trip.id, trip.name, sam.name, "Taxi", 120_00, now - DAY))
        activity.insert(ActivityEntity(0, ActivityKind.EXPENSE_ADDED, trip.id, trip.name, alex.name, "Dinner", 300_00, now - 2 * DAY))
        activity.insert(ActivityEntity(0, ActivityKind.EXPENSE_ADDED, flat.id, flat.name, taylor.name, "Internet", 299_00, now - 5 * DAY))
    }

    private suspend fun expense(
        db: AppDatabase, id: String, groupId: String, title: String, amountOre: Long,
        payerId: String, participants: List<String>, createdAt: Long,
    ) {
        val shares = DebtCalculator.splitEvenly(amountOre, participants)
        db.expenseDao().insertWithShares(
            ExpenseEntity(id, groupId, title, amountOre, payerId, false, createdAt, SyncStatus.SYNCED),
            shares.map { (userId, share) -> ExpenseShareEntity(id, userId, share) },
        )
    }

    private const val DAY = 24L * 60 * 60 * 1000
}
