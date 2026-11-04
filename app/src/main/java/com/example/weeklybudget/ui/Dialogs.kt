@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.data.Category
import com.example.weeklybudget.domain.Period
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
private fun DateField(period: Period, date: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.DateRange, contentDescription = null)
        Text("  التاريخ: ${fmtDate(date)}")
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return period.contains(d)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("موافق") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
fun ExpenseDialog(
    period: Period,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, categoryId: Long, note: String, date: LocalDate) -> Unit
) {
    val today = LocalDate.now()
    var amountText by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: -1L) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(if (period.contains(today)) today else period.start) }

    val amount = parseNumber(amountText)
    val valid = amount != null && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مصروف جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountField(amountText, { amountText = it }, "المبلغ ($CURRENCY)")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { c ->
                        FilterChip(
                            selected = c.id == categoryId,
                            onClick = { categoryId = c.id },
                            label = { Text(c.name) },
                            leadingIcon = { Icon(iconFor(c.iconKey), null, Modifier.size(18.dp)) }
                        )
                    }
                }
                DateField(period, date) { date = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(amount!!, categoryId, note.trim(), date) }, enabled = valid) {
                Text("إضافة")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun BillDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, iconKey: String, dueDay: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf("1") }
    var icon by remember { mutableStateOf(BillIconKeys.first()) }

    val amount = parseNumber(amountText)
    val day = parseNumber(dayText)?.toInt()
    val valid = name.isNotBlank() && amount != null && amount > 0 && day != null && day in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فاتورة شهرية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم الفاتورة (كهرباء، إنترنت...)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                AmountField(amountText, { amountText = it }, "المبلغ ($CURRENCY)")
                AmountField(dayText, { dayText = it }, "يوم الاستحقاق (1 - 31)")
                IconPicker(BillIconKeys, icon) { icon = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(name.trim(), amount!!, icon, day!!) }, enabled = valid) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun DebtDialog(onDismiss: () -> Unit, onSave: (name: String, total: Double, installment: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var installmentText by remember { mutableStateOf("") }

    val total = parseNumber(totalText)
    val installment = parseNumber(installmentText)
    val valid = name.isNotBlank() && total != null && total > 0 && installment != null && installment >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دين جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم الدين أو الدائن") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                AmountField(totalText, { totalText = it }, "إجمالي الدين ($CURRENCY)")
                AmountField(installmentText, { installmentText = it }, "القسط الشهري ($CURRENCY)")
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(name.trim(), total!!, installment!!) }, enabled = valid) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun GoalDialog(onDismiss: () -> Unit, onSave: (name: String, target: Double, monthly: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var monthlyText by remember { mutableStateOf("") }

    val target = parseNumber(targetText)
    val monthly = parseNumber(monthlyText)
    val valid = name.isNotBlank() && target != null && target > 0 && monthly != null && monthly >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("هدف ادخار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم الهدف (سفر، سيارة...)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                AmountField(targetText, { targetText = it }, "المبلغ المستهدف ($CURRENCY)")
                AmountField(monthlyText, { monthlyText = it }, "الادخار الشهري ($CURRENCY)")
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(name.trim(), target!!, monthly!!) }, enabled = valid) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AmountDialog(
    title: String,
    initial: Double,
    confirmLabel: String,
    allowZero: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toLong().toString() else "") }
    val value = parseNumber(text)
    val valid = value != null && (value > 0 || (allowZero && value == 0.0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AmountField(text, { text = it }, "المبلغ ($CURRENCY)") },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(value!!) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun CategoryDialog(onDismiss: () -> Unit, onSave: (name: String, iconKey: String, limit: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(CategoryIconKeys.first()) }

    val limit = if (limitText.isBlank()) 0.0 else parseNumber(limitText)
    val valid = name.isNotBlank() && limit != null && limit >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فئة جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم الفئة") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                AmountField(limitText, { limitText = it }, "سقف أسبوعي (اختياري)")
                IconPicker(CategoryIconKeys, icon) { icon = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(name.trim(), icon, limit!!) }, enabled = valid) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
