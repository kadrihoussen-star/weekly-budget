package com.example.weeklybudget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeklybudget.data.AppDatabase
import com.example.weeklybudget.data.Bill
import com.example.weeklybudget.data.Category
import com.example.weeklybudget.data.Debt
import com.example.weeklybudget.data.DebtPayment
import com.example.weeklybudget.data.Expense
import com.example.weeklybudget.data.MonthBudget
import com.example.weeklybudget.data.SavingsEntry
import com.example.weeklybudget.data.SavingsGoal
import com.example.weeklybudget.domain.Period
import com.example.weeklybudget.domain.WeekSummary
import com.example.weeklybudget.domain.computeWeeks
import com.example.weeklybudget.domain.periodOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DebtUi(val debt: Debt, val paid: Double, val history: List<DebtPayment>) {
    val remaining: Double get() = (debt.total - paid).coerceAtLeast(0.0)
    val progress: Float
        get() = if (debt.total <= 0) 0f else (paid / debt.total).toFloat().coerceIn(0f, 1f)
    /** القسط المخطط خصمه من الدخل هذه الفترة. */
    val monthlyDue: Double get() = minOf(debt.monthlyInstallment, remaining)
}

data class GoalUi(val goal: SavingsGoal, val saved: Double, val history: List<SavingsEntry>) {
    val remaining: Double get() = (goal.target - saved).coerceAtLeast(0.0)
    val progress: Float
        get() = if (goal.target <= 0) 0f else (saved / goal.target).toFloat().coerceIn(0f, 1f)
    val monthlyPlan: Double get() = minOf(goal.monthlyContribution, remaining)
}

data class UiState(
    val period: Period = periodOf(YearMonth.now(), 1),
    val hasBudget: Boolean = false,
    val income: Double = 0.0,
    val startDay: Int = 1,
    val rollover: Boolean = true,
    val categories: List<Category> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val debts: List<DebtUi> = emptyList(),
    val goals: List<GoalUi> = emptyList(),
    val billsTotal: Double = 0.0,
    val debtPlanned: Double = 0.0,
    val savingsPlanned: Double = 0.0,
    val weeks: List<WeekSummary> = emptyList(),
    val totalSpent: Double = 0.0,
    val currentWeek: Int? = null
) {
    val obligations: Double get() = billsTotal + debtPlanned + savingsPlanned
    val distributable: Double get() = income - obligations
    val remaining: Double get() = distributable - totalSpent
    val progress: Float
        get() = when {
            distributable <= 0.0 -> if (totalSpent > 0) 1f else 0f
            else -> (totalSpent / distributable).toFloat().coerceIn(0f, 1f)
        }
}

private data class Core(
    val budget: MonthBudget?,
    val latest: MonthBudget?,
    val expenses: List<Expense>,
    val bills: List<Bill>,
    val categories: List<Category>
)

private data class Commitments(
    val debts: List<Debt>,
    val debtPayments: List<DebtPayment>,
    val goals: List<SavingsGoal>,
    val entries: List<SavingsEntry>
)

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).budgetDao()
    private val month = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch {
            if (dao.countCategories() == 0) {
                dao.insertCategory(Category(name = "النقل", iconKey = "bus", sortOrder = 0))
                dao.insertCategory(Category(name = "الغداء", iconKey = "food", sortOrder = 1))
                dao.insertCategory(Category(name = "مصروف شخصي", iconKey = "person", sortOrder = 2))
            }
            // إن كان اليوم قبل بداية فترة هذا الشهر، فالفترة الجارية هي فترة الشهر السابق.
            dao.latestBudget()?.let { latest ->
                val today = LocalDate.now()
                val start = periodOf(YearMonth.from(today), latest.startDay).start
                if (today.isBefore(start)) month.value = YearMonth.from(today).minusMonths(1)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState> = month.flatMapLatest { ym ->
        val key = ym.toString()
        val core = combine(
            dao.observeBudget(key),
            dao.observeLatestBudget(),
            dao.observeExpenses(key),
            dao.observeBills(key),
            dao.observeCategories()
        ) { b, l, e, bl, c -> Core(b, l, e, bl, c) }
        val commitments = combine(
            dao.observeDebts(),
            dao.observeDebtPayments(),
            dao.observeGoals(),
            dao.observeSavingsEntries()
        ) { d, dp, g, se -> Commitments(d, dp, g, se) }
        combine(core, commitments) { c, k -> build(ym, c, k) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun build(ym: YearMonth, c: Core, k: Commitments): UiState {
        val income = c.budget?.income ?: 0.0
        val startDay = c.budget?.startDay ?: c.latest?.startDay ?: 1
        val rollover = c.budget?.rollover ?: true
        val period = periodOf(ym, startDay)

        val debts = k.debts.map { d ->
            val h = k.debtPayments.filter { it.debtId == d.id }
            DebtUi(d, h.sumOf { it.amount }, h)
        }
        val goals = k.goals.map { g ->
            val h = k.entries.filter { it.goalId == g.id }
            GoalUi(g, h.sumOf { it.amount }, h)
        }
        val billsTotal = c.bills.sumOf { it.amount }
        val debtPlanned = debts.sumOf { it.monthlyDue }
        val savingsPlanned = goals.sumOf { it.monthlyPlan }
        val distributable = income - billsTotal - debtPlanned - savingsPlanned

        val today = LocalDate.now()
        return UiState(
            period = period,
            hasBudget = c.budget != null,
            income = income,
            startDay = startDay,
            rollover = rollover,
            categories = c.categories,
            expenses = c.expenses,
            bills = c.bills,
            debts = debts,
            goals = goals,
            billsTotal = billsTotal,
            debtPlanned = debtPlanned,
            savingsPlanned = savingsPlanned,
            weeks = computeWeeks(period, distributable, rollover, c.expenses, c.categories),
            totalSpent = c.expenses.sumOf { it.amount },
            currentWeek = if (period.contains(today)) period.weekIndexOf(today) else null
        )
    }

    // ---- التنقل بين الفترات ----
    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    // ---- الدخل والفترة ----
    fun saveBudget(income: Double, startDay: Int, rollover: Boolean) = viewModelScope.launch {
        dao.upsertBudget(MonthBudget(month.value.toString(), income, startDay.coerceIn(1, 31), rollover))
    }

    // ---- المصاريف ----
    fun addExpense(amount: Double, categoryId: Long, note: String, date: LocalDate) = viewModelScope.launch {
        dao.insertExpense(
            Expense(
                monthKey = month.value.toString(),
                epochDay = date.toEpochDay(),
                amount = amount,
                categoryId = categoryId,
                note = note
            )
        )
    }

    fun deleteExpense(e: Expense) = viewModelScope.launch { dao.deleteExpense(e) }

    // ---- الفئات ----
    fun addCategory(name: String, iconKey: String, weeklyLimit: Double) = viewModelScope.launch {
        val order = state.value.categories.size
        dao.insertCategory(Category(name = name, iconKey = iconKey, weeklyLimit = weeklyLimit, sortOrder = order))
    }

    fun updateCategoryLimit(c: Category, limit: Double) = viewModelScope.launch {
        dao.updateCategory(c.copy(weeklyLimit = limit))
    }

    fun deleteCategory(c: Category) = viewModelScope.launch { dao.deleteCategory(c) }

    // ---- الفواتير ----
    fun addBill(name: String, amount: Double, iconKey: String, dueDay: Int) = viewModelScope.launch {
        dao.insertBill(Bill(monthKey = month.value.toString(), name = name, amount = amount, iconKey = iconKey, dueDay = dueDay))
    }

    fun toggleBillPaid(b: Bill) = viewModelScope.launch { dao.updateBill(b.copy(paid = !b.paid)) }
    fun deleteBill(b: Bill) = viewModelScope.launch { dao.deleteBill(b) }

    fun copyBillsFromPreviousMonth() = viewModelScope.launch {
        val previous = dao.billsOf(month.value.minusMonths(1).toString())
        previous.forEach { dao.insertBill(it.copy(id = 0, monthKey = month.value.toString(), paid = false)) }
    }

    // ---- الديون ----
    fun addDebt(name: String, total: Double, installment: Double) = viewModelScope.launch {
        dao.insertDebt(Debt(name = name, total = total, monthlyInstallment = installment))
    }

    fun deleteDebt(id: Long) = viewModelScope.launch {
        dao.deleteDebtPayments(id)
        dao.deleteDebt(id)
    }

    fun addDebtPayment(debtId: Long, amount: Double) = viewModelScope.launch {
        dao.insertDebtPayment(DebtPayment(debtId = debtId, epochDay = LocalDate.now().toEpochDay(), amount = amount))
    }

    fun deleteDebtPayment(p: DebtPayment) = viewModelScope.launch { dao.deleteDebtPayment(p) }

    // ---- الادخار ----
    fun addGoal(name: String, target: Double, monthly: Double) = viewModelScope.launch {
        dao.insertGoal(SavingsGoal(name = name, target = target, monthlyContribution = monthly))
    }

    fun deleteGoal(id: Long) = viewModelScope.launch {
        dao.deleteSavingsEntries(id)
        dao.deleteGoal(id)
    }

    fun addSavingsEntry(goalId: Long, amount: Double) = viewModelScope.launch {
        dao.insertSavingsEntry(SavingsEntry(goalId = goalId, epochDay = LocalDate.now().toEpochDay(), amount = amount))
    }

    fun deleteSavingsEntry(e: SavingsEntry) = viewModelScope.launch { dao.deleteSavingsEntry(e) }
}
