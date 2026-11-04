package com.example.weeklybudget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** ميزانية شهر واحد. المفتاح بصيغة "2026-11". */
@Entity(tableName = "month_budget")
data class MonthBudget(
    @PrimaryKey val monthKey: String,
    val income: Double,
    /** ترحيل المتبقي (أو العجز) من الأسبوع إلى الأسبوع التالي. */
    val rollover: Boolean = true
)

@Entity(tableName = "expense", indices = [Index("monthKey")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val epochDay: Long,
    val amount: Double,
    val category: String,
    val note: String = ""
)
