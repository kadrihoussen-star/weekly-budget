package com.example.weeklybudget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeklybudget.data.AppDatabase
import com.example.weeklybudget.data.Expense
import com.example.weeklybudget.data.MonthBudget
import com.example.weeklybudget.domain.WeekSummary
import com.example.weeklybudget.domain.computeWeeks
import com.example.weeklybudget.domain.currentWeekIndex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class UiState(
    val month: YearMonth = YearMonth.now(),
    val income: Double = 0.0,
    val rollover: Boolean = true,
    val hasBudget: Boolean = false,
    val weeks: List<WeekSummary> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val currentWeek: Int? = null
) {
    val remaining: Double get() = income - totalSpent
    val progress: Float
        get() = if (income <= 0.0) 0f else (totalSpent / income).toFloat().coerceIn(0f, 1f)
}

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).budgetDao()
    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState> = month.flatMapLatest { ym ->
        val key = ym.toString()
        combine(dao.observeBudget(key), dao.observeExpenses(key)) { budget, expenses ->
            val income = budget?.income ?: 0.0
            val rollover = budget?.rollover ?: true
            UiState(
                month = ym,
                income = income,
                rollover = rollover,
                hasBudget = budget != null,
                weeks = computeWeeks(ym, income, rollover, expenses),
                expenses = expenses,
                totalSpent = expenses.sumOf { it.amount },
                currentWeek = currentWeekIndex(ym)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    fun saveBudget(income: Double, rollover: Boolean) = viewModelScope.launch {
        dao.upsertBudget(MonthBudget(month.value.toString(), income, rollover))
    }

    fun addExpense(amount: Double, category: String, note: String, day: Int) = viewModelScope.launch {
        val ym = month.value
        val safeDay = day.coerceIn(1, ym.lengthOfMonth())
        dao.insertExpense(
            Expense(
                monthKey = ym.toString(),
                epochDay = ym.atDay(safeDay).toEpochDay(),
                amount = amount,
                category = category,
                note = note
            )
        )
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch { dao.deleteExpense(expense) }
}
