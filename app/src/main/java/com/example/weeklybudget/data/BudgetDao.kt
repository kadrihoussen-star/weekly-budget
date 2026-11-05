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
    suspend fun insertSavingsEntry(entry: SavingsEntry): Long

    @Query("DELETE FROM savings_entry WHERE id = :id")
    suspend fun deleteSavingsEntryById(id: Long)

    // ---- تعديلات الأسابيع ----
    @Query("SELECT * FROM week_adjustment WHERE monthKey = :key ORDER BY id")
    fun observeAdjustments(key: String): Flow<List<WeekAdjustment>>

    @Insert
    suspend fun insertAdjustment(a: WeekAdjustment): Long

    @Query("DELETE FROM week_adjustment WHERE id = :id")
    suspend fun deleteAdjustment(id: Long)

    // ---- صندوق الطوارئ ----
    @Query("SELECT * FROM emergency_entry ORDER BY epochDay DESC, id DESC")
    fun observeEmergencyEntries(): Flow<List<EmergencyEntry>>

    @Insert
    suspend fun insertEmergencyEntry(e: EmergencyEntry): Long

    @Query("DELETE FROM emergency_entry WHERE id = :id")
    suspend fun deleteEmergencyEntry(id: Long)

    @Query("SELECT * FROM emergency_settings WHERE id = 0")
    fun observeEmergencySettings(): Flow<EmergencySettings?>

    @Query("SELECT * FROM emergency_settings WHERE id = 0")
    suspend fun emergencySettings(): EmergencySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmergencySettings(s: EmergencySettings)

    // ---- الملف الشخصي ----
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observeProfile(): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun profile(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(p: Profile)

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    @Query("SELECT * FROM week_adjustment") suspend fun allAdjustments(): List<WeekAdjustment>
    @Query("SELECT * FROM emergency_entry") suspend fun allEmergencyEntries(): List<EmergencyEntry>
    @Query("DELETE FROM week_adjustment") suspend fun clearAdjustments()
    @Query("DELETE FROM emergency_entry") suspend fun clearEmergencyEntries()
    @Query("DELETE FROM emergency_settings") suspend fun clearEmergencySettings()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAdjustments(items: List<WeekAdjustment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEmergencyEntries(items: List<EmergencyEntry>)

    @Delete
    suspend fun deleteSavingsEntry(entry: SavingsEntry)

    @Query("DELETE FROM savings_entry WHERE goalId = :goalId")
    suspend fun deleteSavingsEntries(goalId: Long)

    // ---- النسخ الاحتياطي: قراءة كل الجداول ----
    @Query("SELECT * FROM month_budget") suspend fun allBudgets(): List<MonthBudget>
    @Query("SELECT * FROM category") suspend fun allCategories(): List<Category>
    @Query("SELECT * FROM expense") suspend fun allExpenses(): List<Expense>
    @Query("SELECT * FROM bill") suspend fun allBills(): List<Bill>
    @Query("SELECT * FROM debt") suspend fun allDebts(): List<Debt>
    @Query("SELECT * FROM debt_payment") suspend fun allDebtPayments(): List<DebtPayment>
    @Query("SELECT * FROM savings_goal") suspend fun allGoals(): List<SavingsGoal>
    @Query("SELECT * FROM savings_entry") suspend fun allSavingsEntries(): List<SavingsEntry>

    // ---- الاستعادة: مسح ثم إدراج بالمعرّفات الأصلية ----
    @Query("DELETE FROM month_budget") suspend fun clearBudgets()
    @Query("DELETE FROM category") suspend fun clearCategories()
    @Query("DELETE FROM expense") suspend fun clearExpenses()
    @Query("DELETE FROM bill") suspend fun clearBills()
    @Query("DELETE FROM debt") suspend fun clearDebts()
    @Query("DELETE FROM debt_payment") suspend fun clearDebtPayments()
    @Query("DELETE FROM savings_goal") suspend fun clearGoals()
    @Query("DELETE FROM savings_entry") suspend fun clearSavingsEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudgets(items: List<MonthBudget>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCategories(items: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putExpenses(items: List<Expense>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBills(items: List<Bill>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebts(items: List<Debt>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebtPayments(items: List<DebtPayment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(items: List<SavingsGoal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSavingsEntries(items: List<SavingsEntry>)
}
