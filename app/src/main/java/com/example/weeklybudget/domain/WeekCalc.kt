package com.example.weeklybudget.domain

import com.example.weeklybudget.data.Category
import com.example.weeklybudget.data.Expense
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

const val WEEKS = 4

/**
 * فترة الميزانية: من [start] إلى [end] شاملة.
 * تُقسَّم دائمًا إلى 4 أسابيع؛ كل أسبوع 7 أيام، والأسبوع الرابع يستوعب بقية الأيام (حتى 10 أيام).
 */
data class Period(val ym: YearMonth, val start: LocalDate, val end: LocalDate) {
    fun contains(d: LocalDate): Boolean = !d.isBefore(start) && !d.isAfter(end)

    fun weekIndexOf(d: LocalDate): Int {
        val offset = ChronoUnit.DAYS.between(start, d).toInt()
        return (offset / 7).coerceIn(0, WEEKS - 1)
    }

    fun weekStart(i: Int): LocalDate = start.plusDays(7L * i)
    fun weekEnd(i: Int): LocalDate = if (i == WEEKS - 1) end else weekStart(i).plusDays(6)
}

fun periodOf(ym: YearMonth, startDay: Int): Period {
    val s = ym.atDay(startDay.coerceIn(1, ym.lengthOfMonth()))
    val next = ym.plusMonths(1)
    val nextStart = next.atDay(startDay.coerceIn(1, next.lengthOfMonth()))
    return Period(ym, s, nextStart.minusDays(1))
}

data class CategorySpend(val category: Category, val spent: Double) {
    val limit: Double get() = category.weeklyLimit
    val ratio: Float
        get() = if (limit > 0) (spent / limit).toFloat() else 0f
}

data class WeekSummary(
    val index: Int,
    val start: LocalDate,
    val end: LocalDate,
    val baseBudget: Double,
    val carried: Double,
    val spent: Double,
    val byCategory: List<CategorySpend>
) {
    val available: Double get() = baseBudget + carried
    val remaining: Double get() = available - spent
    val progress: Float
        get() = when {
            available <= 0.0 -> if (spent > 0) 1f else 0f
            else -> (spent / available).toFloat().coerceIn(0f, 1f)
        }
    val isOver: Boolean get() = remaining < 0
}

/**
 * [distributable] = الدخل − (الفواتير + أقساط الديون + الادخار المخطط).
 * يُقسَّم بالتساوي على 4 أسابيع.
 */
fun computeWeeks(
    period: Period,
    distributable: Double,
    rollover: Boolean,
    expenses: List<Expense>,
    categories: List<Category>
): List<WeekSummary> {
    val base = distributable / WEEKS
    val other = Category(id = -1, name = "أخرى", iconKey = "other")
    val known = categories.map { it.id }.toSet()

    val perWeek = Array(WEEKS) { HashMap<Long, Double>() }
    expenses.forEach { e ->
        val w = period.weekIndexOf(LocalDate.ofEpochDay(e.epochDay))
        val id = if (e.categoryId in known) e.categoryId else -1L
        perWeek[w][id] = (perWeek[w][id] ?: 0.0) + e.amount
    }

    var carry = 0.0
    return (0 until WEEKS).map { i ->
        val map = perWeek[i]
        val rows = categories.map { CategorySpend(it, map[it.id] ?: 0.0) } +
            (map[-1L]?.let { listOf(CategorySpend(other, it)) } ?: emptyList())
        val week = WeekSummary(
            index = i,
            start = period.weekStart(i),
            end = period.weekEnd(i),
            baseBudget = base,
            carried = if (rollover) carry else 0.0,
            spent = map.values.sum(),
            byCategory = rows
        )
        carry = week.remaining
        week
    }
}
