package com.example.weeklybudget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ميزانية فترة واحدة. المفتاح هو الشهر الذي تبدأ فيه الفترة ("2026-11")،
 * وتبدأ الفترة فعليًا في اليوم [startDay] من ذلك الشهر وتنتهي قبل اليوم نفسه من الشهر التالي.
 */
@Entity(tableName = "month_budget")
data class MonthBudget(
    @PrimaryKey val monthKey: String,
    val income: Double,
    val startDay: Int = 1,
    /** ترحيل المتبقي (أو العجز) من الأسبوع إلى الأسبوع التالي. */
    val rollover: Boolean = true
)

/** فئة مصروف أسبوعية (النقل، الغداء، المصروف الشخصي، أو فئة يضيفها المستخدم). */
@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    /** سقف أسبوعي اختياري؛ 0 يعني بلا سقف. */
    val weeklyLimit: Double = 0.0,
    val sortOrder: Int = 0
)

@Entity(tableName = "expense", indices = [Index("monthKey"), Index("categoryId")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val epochDay: Long,
    val amount: Double,
    val categoryId: Long,
    val note: String = ""
)

/** فاتورة شهرية تخص فترة بعينها. */
@Entity(tableName = "bill", indices = [Index("monthKey")])
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val name: String,
    val amount: Double,
    val iconKey: String,
    val dueDay: Int,
    val paid: Boolean = false
)

@Entity(tableName = "debt")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val total: Double,
    val monthlyInstallment: Double
)

@Entity(tableName = "debt_payment", indices = [Index("debtId")])
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val epochDay: Long,
    val amount: Double
)

@Entity(tableName = "savings_goal")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Double,
    val monthlyContribution: Double
)

@Entity(tableName = "savings_entry", indices = [Index("goalId")])
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val epochDay: Long,
    val amount: Double
)
