package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SavingsDebtsScreen(state: UiState, vm: BudgetViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("الادخار") },
                icon = { Icon(Icons.Filled.AccountBalanceWallet, null) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("الطوارئ") },
                icon = { Icon(Icons.Filled.Shield, null) }
            )
            Tab(
                selected = tab == 2,
                onClick = { tab = 2 },
                text = { Text("الديون") },
                icon = { Icon(Icons.Filled.CreditCard, null) }
            )
        }
        when (tab) {
            0 -> SavingsTab(state, vm)
            1 -> EmergencyTab(state, vm)
            else -> DebtsTab(state, vm)
        }
    }
}

@Composable
private fun SavingsTab(state: UiState, vm: BudgetViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var recordFor by remember { mutableStateOf<GoalUi?>(null) }
    var deleteFor by remember { mutableStateOf<GoalUi?>(null) }
    val totalSaved = state.goals.sumOf { it.saved }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.AccountBalanceWallet, Positive, size = 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("إجمالي المدخر", color = MaterialTheme.colorScheme.secondary)
                            Text(money(totalSaved), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    LabeledRow("المخصوم من دخل هذه الفترة", money(state.savingsPlanned))
                }
            }
            if (state.goals.isEmpty()) {
                item { EmptyHint(Icons.Filled.AccountBalanceWallet, "أضف هدف ادخار وحدّد المبلغ الشهري.") }
            }
            items(state.goals, key = { it.goal.id }) { g ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.AccountBalanceWallet, Positive)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.goal.name, fontWeight = FontWeight.Bold)
                            Text("الهدف ${money(g.goal.target)}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { deleteFor = g }) {
                            Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    ProgressBar(g.progress, Positive)
                    LabeledRow("المدخر", money(g.saved), Positive)
                    LabeledRow("المتبقي للهدف", money(g.remaining))
                    LabeledRow("الادخار الشهري المخطط", money(g.monthlyPlan))
                    FilledTonalButton(onClick = { recordFor = g }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل ادخار")
                    }
                    if (g.history.isNotEmpty()) {
                        HorizontalDivider()
                        g.history.take(3).forEach { e ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(fmtEpoch(e.epochDay), Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
                                Text(money(e.amount), fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { vm.deleteSavingsEntry(e) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, "حذف", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("هدف ادخار") }
        )
    }

    if (showAdd) {
        GoalDialog(onDismiss = { showAdd = false }, onSave = { n, t, m ->
            vm.addGoal(n, t, m); showAdd = false
        })
    }
    recordFor?.let { g ->
        AmountDialog(
            title = "تسجيل ادخار في «${g.goal.name}»",
            initial = g.monthlyPlan,
            confirmLabel = "تسجيل",
            onDismiss = { recordFor = null },
            onSave = { vm.addSavingsEntry(g.goal.id, it); recordFor = null }
        )
    }
    deleteFor?.let { g ->
        ConfirmDialog(
            text = "سيُحذف الهدف «${g.goal.name}» مع كل ما سُجّل فيه.",
            onConfirm = { vm.deleteGoal(g.goal.id); deleteFor = null },
            onDismiss = { deleteFor = null }
        )
    }
}

@Composable
private fun DebtsTab(state: UiState, vm: BudgetViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<DebtUi?>(null) }
    var deleteFor by remember { mutableStateOf<DebtUi?>(null) }
    val totalRemaining = state.debts.sumOf { it.remaining }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.AccountBalance, Negative, size = 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("إجمالي المتبقي من الديون", color = MaterialTheme.colorScheme.secondary)
                            Text(money(totalRemaining), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    LabeledRow("الأقساط المخصومة من دخل هذه الفترة", money(state.debtPlanned))
                }
            }
            if (state.debts.isEmpty()) {
                item { EmptyHint(Icons.Filled.AccountBalance, "لا توجد ديون مسجّلة.") }
            }
            items(state.debts, key = { it.debt.id }) { d ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.AccountBalance, Negative)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.debt.name, fontWeight = FontWeight.Bold)
                            Text("الإجمالي ${money(d.debt.total)}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { deleteFor = d }) {
                            Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    ProgressBar(d.progress, Positive)
                    LabeledRow("المسدّد", money(d.paid), Positive)
                    LabeledRow("المتبقي", money(d.remaining), if (d.remaining > 0) Negative else Positive, bold = true)
                    LabeledRow("القسط الشهري", money(d.debt.monthlyInstallment))
                    FilledTonalButton(
                        onClick = { payFor = d },
                        enabled = d.remaining > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Payments, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (d.remaining > 0) "تسجيل تسديد" else "تم السداد بالكامل")
                    }
                    if (d.history.isNotEmpty()) {
                        HorizontalDivider()
                        d.history.take(3).forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(fmtEpoch(p.epochDay), Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
                                Text(money(p.amount), fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { vm.deleteDebtPayment(p) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, "حذف", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("دين جديد") }
        )
    }

    if (showAdd) {
        DebtDialog(onDismiss = { showAdd = false }, onSave = { n, t, i ->
            vm.addDebt(n, t, i); showAdd = false
        })
    }
    payFor?.let { d ->
        AmountDialog(
            title = "تسديد من «${d.debt.name}»",
            initial = d.monthlyDue,
            confirmLabel = "تسجيل",
            onDismiss = { payFor = null },
            onSave = { vm.addDebtPayment(d.debt.id, it); payFor = null }
        )
    }
    deleteFor?.let { d ->
        ConfirmDialog(
            text = "سيُحذف الدين «${d.debt.name}» مع سجل تسديداته.",
            onConfirm = { vm.deleteDebt(d.debt.id); deleteFor = null },
            onDismiss = { deleteFor = null }
        )
    }
}
