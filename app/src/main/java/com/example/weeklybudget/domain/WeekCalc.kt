package com.example.weeklybudget.domain

import com.example.weeklybudget.data.Expense
import java.time.LocalDate
import java.time.YearMonth

/** ملخص أسبوع واحد من الشهر. */
data class WeekSummary(
    val index: Int,          // يبدأ من 0
    val startDay: Int,
    val endDay: Int,
    val baseBudget: Double,  // نصيب الأسبوع من الدخل
    val carried: Double,     // المرحّل من الأسبوع السابق (قد يكون سالبًا)
    val spent: Double
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

/** رقم الأسبوع (من 0) الذي ينتمي إليه اليوم. الأسابيع: 1-7، 8-14، 15-21، 22-28، 29-نهاية الشهر. */
fun weekIndexOfDay(day: Int): Int = (day - 1) / 7

fun weeksInMonth(ym: YearMonth): Int = (ym.lengthOfMonth() + 6) / 7

fun currentWeekIndex(ym: YearMonth, today: LocalDate = LocalDate.now()): Int? =
    if (YearMonth.from(today) == ym) weekIndexOfDay(today.dayOfMonth) else null

fun computeWeeks(
    ym: YearMonth,
    income: Double,
    rollover: Boolean,
    expenses: List<Expense>
): List<WeekSummary> {
    val weeks = weeksInMonth(ym)
    val base = if (weeks > 0) income / weeks else 0.0
    val spentPerWeek = DoubleArray(weeks)
    expenses.forEach { e ->
        val day = LocalDate.ofEpochDay(e.epochDay).dayOfMonth
        spentPerWeek[weekIndexOfDay(day).coerceIn(0, weeks - 1)] += e.amount
    }

    val result = ArrayList<WeekSummary>(weeks)
    var carry = 0.0
    for (i in 0 until weeks) {
        val start = i * 7 + 1
        val end = minOf(start + 6, ym.lengthOfMonth())
        val summary = WeekSummary(i, start, end, base, if (rollover) carry else 0.0, spentPerWeek[i])
        result += summary
        carry = summary.remaining
    }
    return result
}
