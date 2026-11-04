package com.example.weeklybudget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM month_budget WHERE monthKey = :key")
    fun observeBudget(key: String): Flow<MonthBudget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthBudget)

    @Query("SELECT * FROM expense WHERE monthKey = :key ORDER BY epochDay DESC, id DESC")
    fun observeExpenses(key: String): Flow<List<Expense>>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}
