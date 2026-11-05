package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** ادخار فائض أسبوع: اختيار المبلغ والوجهة (صندوق الطوارئ أو أحد أهداف الادخار). */
@Composable
fun SaveSurplusDialog(
    surplus: Double,
    goals: List<GoalUi>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, goalId: Long) -> Unit
) {
    var text by remember { mutableStateOf(surplus.toLong().toString()) }
    var dest by remember { mutableStateOf(EMERGENCY_ID) }
    val amount = parseNumber(text)
    val valid = amount != null && amount > 0 && amount <= surplus + 0.5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ادخار فائض الأسبوع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "المتاح للادخار: ${money(surplus)}",
                    color = MaterialTheme.colorScheme.secondary
                )
                AmountField(text, { text = it }, "المبلغ (${Currency.label})")
                Text("ادّخره في:", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = dest == EMERGENCY_ID,
                            onClick = { dest = EMERGENCY_ID },
                            label = { Text("صندوق الطوارئ") },
                            leadingIcon = { Icon(Icons.Filled.Shield, null, Modifier.size(18.dp)) }
                        )
                    }
                    items(goals, key = { it.goal.id }) { g ->
                        FilterChip(
                            selected = dest == g.goal.id,
                            onClick = { dest = g.goal.id },
                            label = { Text(g.goal.name) },
                            leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, null, Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(amount!!, dest) }, enabled = valid) { Text("ادخار") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

/** مبلغ لا يتجاوز [max] (تغطية عجز أو سحب من الطوارئ). */
@Composable
fun LimitedAmountDialog(
    title: String,
    hint: String,
    initial: Double,
    max: Double,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toLong().toString() else "") }
    val value = parseNumber(text)
    val valid = value != null && value > 0 && value <= max + 0.5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(hint, color = MaterialTheme.colorScheme.secondary)
                AmountField(text, { text = it }, "المبلغ (${Currency.label})")
                if (value != null && value > max + 0.5) {
                    Text("المبلغ أكبر من الحد المسموح (${money(max)})", color = Negative)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(value!!) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun EmergencySettingsDialog(
    target: Double,
    monthly: Double,
    onDismiss: () -> Unit,
    onSave: (target: Double, monthly: Double) -> Unit
) {
    var targetText by remember { mutableStateOf(if (target > 0) target.toLong().toString() else "") }
    var monthlyText by remember { mutableStateOf(if (monthly > 0) monthly.toLong().toString() else "") }

    val t = if (targetText.isBlank()) 0.0 else parseNumber(targetText)
    val m = if (monthlyText.isBlank()) 0.0 else parseNumber(monthlyText)
    val valid = t != null && t >= 0 && m != null && m >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات صندوق الطوارئ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountField(targetText, { targetText = it }, "المبلغ المستهدف (اختياري)")
                AmountField(monthlyText, { monthlyText = it }, "المساهمة الشهرية من الدخل (اختياري)")
                Text(
                    "تُخصم المساهمة الشهرية من الدخل قبل تقسيمه على الأسابيع، وتتوقف عند بلوغ الهدف.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(t!!, m!!) }, enabled = valid) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
