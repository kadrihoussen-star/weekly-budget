package com.example.weeklybudget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weeklybudget.data.Expense
import com.example.weeklybudget.domain.WeekSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val CURRENCY = "دج"
private val Categories = listOf("طعام", "مواصلات", "فواتير", "صحة", "تعليم", "ترفيه", "أخرى")

private fun money(v: Double): String = String.format(Locale.US, "%,.0f", v) + " " + CURRENCY

private fun monthTitle(ym: YearMonth): String =
    ym.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ar")) + " " + ym.year

/** يقبل الأرقام العربية الهندية والفاصلة العشرية. */
private fun parseNumber(s: String): Double? =
    s.map { c -> if (c in '٠'..'٩') '0' + (c - '٠') else c }
        .joinToString("")
        .replace(',', '.')
        .trim()
        .toDoubleOrNull()

@Composable
private fun remainingColor(value: Double): Color = if (value < 0) Negative else Positive

@Composable
private fun progressColor(progress: Float, over: Boolean): Color = when {
    over -> Negative
    progress >= 0.8f -> Warning
    else -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: BudgetViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(monthTitle(state.month), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::previousMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "الشهر السابق")
                    }
                },
                actions = {
                    IconButton(onClick = vm::nextMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر التالي")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (state.hasBudget) showExpenseDialog = true else showIncomeDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("مصروف جديد") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SummaryCard(state, onEditIncome = { showIncomeDialog = true }) }

            item { SectionTitle("الأسابيع (${state.weeks.size})") }
            items(state.weeks, key = { it.index }) { week ->
                WeekCard(week, isCurrent = week.index == state.currentWeek)
            }

            item { SectionTitle("المصاريف") }
            if (state.expenses.isEmpty()) {
                item {
                    Text(
                        "لا توجد مصاريف في هذا الشهر بعد.",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(state.expenses, key = { it.id }) { expense ->
                    ExpenseRow(expense, onDelete = { vm.deleteExpense(expense) })
                }
            }
        }
    }

    if (showIncomeDialog) {
        IncomeDialog(
            initialIncome = state.income,
            initialRollover = state.rollover,
            onDismiss = { showIncomeDialog = false },
            onSave = { income, rollover ->
                vm.saveBudget(income, rollover)
                showIncomeDialog = false
            }
        )
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            month = state.month,
            onDismiss = { showExpenseDialog = false },
            onSave = { amount, category, note, day ->
                vm.addExpense(amount, category, note, day)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SummaryCard(state: UiState, onEditIncome: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("دخل الشهر", color = MaterialTheme.colorScheme.secondary)
                    Text(
                        if (state.hasBudget) money(state.income) else "لم يُحدَّد بعد",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onEditIncome) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الدخل")
                }
            }

            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor(state.progress, state.remaining < 0),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("المصروف", color = MaterialTheme.colorScheme.secondary)
                    Text(money(state.totalSpent), fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المتبقي", color = MaterialTheme.colorScheme.secondary)
                    Text(
                        money(state.remaining),
                        fontWeight = FontWeight.SemiBold,
                        color = remainingColor(state.remaining)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCard(week: WeekSummary, isCurrent: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "الأسبوع ${week.index + 1}" + if (isCurrent) " (الحالي)" else "",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "من ${week.startDay} إلى ${week.endDay}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LinearProgressIndicator(
                progress = { week.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = progressColor(week.progress, week.isOver),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            LabeledRow("الميزانية", money(week.baseBudget))
            if (week.carried != 0.0) {
                LabeledRow(
                    "المرحّل من الأسبوع السابق",
                    money(week.carried),
                    valueColor = remainingColor(week.carried)
                )
            }
            LabeledRow("المصروف", money(week.spent))
            LabeledRow(
                "المتبقي",
                money(week.remaining),
                valueColor = remainingColor(week.remaining),
                bold = true
            )
        }
    }
}

@Composable
private fun LabeledRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.secondary)
        Text(
            value,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onDelete: () -> Unit) {
    val day = LocalDate.ofEpochDay(expense.epochDay).dayOfMonth
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ListItem(
            headlineContent = { Text(expense.category, fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text(
                    "يوم $day" + if (expense.note.isNotBlank()) " · ${expense.note}" else ""
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(money(expense.amount), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                    }
                }
            }
        )
    }
}

@Composable
private fun IncomeDialog(
    initialIncome: Double,
    initialRollover: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean) -> Unit
) {
    var text by remember { mutableStateOf(if (initialIncome > 0) initialIncome.toLong().toString() else "") }
    var rollover by remember { mutableStateOf(initialRollover) }
    val value = parseNumber(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دخل الشهر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("المبلغ ($CURRENCY)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ترحيل المتبقي", fontWeight = FontWeight.SemiBold)
                        Text(
                            "يُضاف ما تبقّى من الأسبوع (أو يُخصم عجزه) إلى الأسبوع التالي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(checked = rollover, onCheckedChange = { rollover = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let { onSave(it, rollover) } },
                enabled = value != null && value >= 0
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ExpenseDialog(
    month: YearMonth,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Int) -> Unit
) {
    val today = LocalDate.now()
    val defaultDay = if (YearMonth.from(today) == month) today.dayOfMonth else 1

    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.first()) }
    var note by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf(defaultDay.toString()) }

    val amount = parseNumber(amountText)
    val day = parseNumber(dayText)?.toInt()
    val valid = amount != null && amount > 0 && day != null && day in 1..month.lengthOfMonth()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مصروف جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ ($CURRENCY)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Categories) { c ->
                        FilterChip(
                            selected = c == category,
                            onClick = { category = c },
                            label = { Text(c) }
                        )
                    }
                }
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it },
                    label = { Text("اليوم من الشهر (1 - ${month.lengthOfMonth()})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة (اختياري)") },
                    singleLine = true
                )
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onSave(amount!!, category, note.trim(), day!!) },
                enabled = valid
            ) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
