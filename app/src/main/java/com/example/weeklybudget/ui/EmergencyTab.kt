package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val EmergencyColor = Color(0xFF0EA5E9)

@Composable
fun EmergencyTab(state: UiState, vm: BudgetViewModel) {
    val e = state.emergency
    var showDeposit by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Shield, EmergencyColor, size = 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("رصيد صندوق الطوارئ", color = MaterialTheme.colorScheme.secondary)
                        Text(
                            money(e.balance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Edit, "إعدادات الصندوق", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (e.target > 0) {
                    ProgressBar(e.progress, EmergencyColor)
                    LabeledRow("الهدف", money(e.target))
                    LabeledRow("المتبقي للهدف", money(e.remaining))
                } else {
                    Text(
                        "لم تحدد هدفًا للصندوق بعد. اضغط على القلم لتحديده.",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LabeledRow("المساهمة الشهرية المخصومة من الدخل", money(e.monthlyPlan))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDeposit = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إيداع")
                    }
                    OutlinedButton(
                        onClick = { showWithdraw = true },
                        enabled = e.balance > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Remove, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("سحب")
                    }
                }
            }
        }

        item {
            Text(
                "تُستعمل هذه الأموال لتغطية العجز الأسبوعي من بطاقة الأسبوع في الشاشة الرئيسية، أو لأي طارئ.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        item { SectionHeader("السجل") }
        if (e.entries.isEmpty()) {
            item { EmptyHint(Icons.Filled.Shield, "لا توجد حركات في الصندوق بعد.") }
        }
        items(e.entries, key = { it.id }) { entry ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        if (entry.amount >= 0) Icons.Filled.Add else Icons.Filled.Remove,
                        if (entry.amount >= 0) Positive else Negative,
                        size = 34.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (entry.note.isNotBlank()) entry.note else if (entry.amount >= 0) "إيداع" else "سحب",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            fmtEpoch(entry.epochDay),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        (if (entry.amount >= 0) "+" else "-") + money(kotlin.math.abs(entry.amount)),
                        fontWeight = FontWeight.Bold,
                        color = if (entry.amount >= 0) Positive else Negative
                    )
                    // القيود الناشئة من الأسابيع يُتراجع عنها من بطاقة الأسبوع نفسها.
                    if (entry.source.isEmpty()) {
                        IconButton(onClick = { vm.deleteEmergencyEntry(entry) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, "حذف", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    if (showDeposit) {
        AmountDialog(
            title = "إيداع في صندوق الطوارئ",
            initial = 0.0,
            confirmLabel = "إيداع",
            onDismiss = { showDeposit = false },
            onSave = { vm.addEmergencyEntry(it, ""); showDeposit = false }
        )
    }
    if (showWithdraw) {
        LimitedAmountDialog(
            title = "سحب من صندوق الطوارئ",
            hint = "الرصيد المتاح ${money(e.balance)}",
            initial = 0.0,
            max = e.balance,
            confirmLabel = "سحب",
            onDismiss = { showWithdraw = false },
            onSave = { vm.addEmergencyEntry(-it, ""); showWithdraw = false }
        )
    }
    if (showSettings) {
        EmergencySettingsDialog(
            target = e.target,
            monthly = e.monthlyContribution,
            onDismiss = { showSettings = false },
            onSave = { t, m -> vm.saveEmergencySettings(t, m); showSettings = false }
        )
    }
}
