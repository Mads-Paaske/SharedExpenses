package com.example.sharedexpenses.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local database = the app's single source of truth.
 * Screens only read from here, so previously loaded data stays available offline (NF03).
 */
@Database(
    entities = [
        UserEntity::class, GroupEntity::class, GroupMemberEntity::class,
        ExpenseEntity::class, ExpenseShareEntity::class, ActivityEntity::class, OutboxEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun activityDao(): ActivityDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "shared_expenses.db").build()
    }
}
