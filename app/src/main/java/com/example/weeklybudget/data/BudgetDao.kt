package com.example.weeklybudget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // ---- الميزانية ----
    @Query("SELECT * FROM month_budget WHERE monthKey = :key")
    fun observeBudget(key: String): Flow<MonthBudget?>

    @Query("SELECT * FROM month_budget ORDER BY monthKey DESC LIMIT 1")
    fun observeLatestBudget(): Flow<MonthBudget?>

    @Query("SELECT * FROM month_budget ORDER BY monthKey DESC LIMIT 1")
    suspend fun latestBudget(): MonthBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthBudget)

    // ---- الفئات ----
    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM category")
    suspend fun countCategories(): Int

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // ---- المصاريف ----
    @Query("SELECT * FROM expense WHERE monthKey = :key ORDER BY epochDay DESC, id DESC")
    fun observeExpenses(key: String): Flow<List<Expense>>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // ---- الفواتير ----
    @Query("SELECT * FROM bill WHERE monthKey = :key ORDER BY dueDay, id")
    fun observeBills(key: String): Flow<List<Bill>>

    @Query("SELECT * FROM bill WHERE monthKey = :key ORDER BY dueDay, id")
    suspend fun billsOf(key: String): List<Bill>

    @Insert
    suspend fun insertBill(bill: Bill)

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    // ---- الديون ----
    @Query("SELECT * FROM debt ORDER BY id")
    fun observeDebts(): Flow<List<Debt>>

    @Insert
    suspend fun insertDebt(debt: Debt)

    @Query("DELETE FROM debt WHERE id = :id")
    suspend fun deleteDebt(id: Long)

    @Query("SELECT * FROM debt_payment ORDER BY epochDay DESC, id DESC")
    fun observeDebtPayments(): Flow<List<DebtPayment>>

    @Insert
    suspend fun insertDebtPayment(payment: DebtPayment)

    @Delete
    suspend fun deleteDebtPayment(payment: DebtPayment)

    @Query("DELETE FROM debt_payment WHERE debtId = :debtId")
    suspend fun deleteDebtPayments(debtId: Long)

    // ---- الادخار ----
    @Query("SELECT * FROM savings_goal ORDER BY id")
    fun observeGoals(): Flow<List<SavingsGoal>>

    @Insert
    suspend fun insertGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goal WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM savings_entry ORDER BY epochDay DESC, id DESC")
    fun observeSavingsEntries(): Flow<List<SavingsEntry>>

    @Insert
    suspend fun insertSavingsEntry(entry: SavingsEntry)

    @Delete
    suspend fun deleteSavingsEntry(entry: SavingsEntry)

    @Query("DELETE FROM savings_entry WHERE goalId = :goalId")
    suspend fun deleteSavingsEntries(goalId: Long)
}
