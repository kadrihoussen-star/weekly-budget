package com.example.weeklybudget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MonthBudget::class, Category::class, Expense::class, Bill::class,
        Debt::class, DebtPayment::class, SavingsGoal::class, SavingsEntry::class,
        WeekAdjustment::class, EmergencyEntry::class, EmergencySettings::class,
        Profile::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** إضافة جدول الملف الشخصي دون المساس ببقية البيانات. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (" +
                        "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, `city` TEXT NOT NULL, `currency` TEXT NOT NULL, " +
                        "`photoPath` TEXT NOT NULL, `createdEpochDay` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weekly_budget.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
