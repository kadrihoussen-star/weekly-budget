package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.data.Bill

@Composable
fun BillsScreen(state: UiState, vm: BudgetViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val paidTotal = state.bills.filter { it.paid }.sumOf { it.amount }
    val unpaidTotal = state.billsTotal - paidTotal

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.ReceiptLong, androidx.compose.ui.graphics.Color(0xFFF97316), size = 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("إجمالي الفواتير", color = MaterialTheme.colorScheme.secondary)
                            Text(money(state.billsTotal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.bills.isNotEmpty()) {
                        val progress = if (state.billsTotal > 0) (paidTotal / state.billsTotal).toFloat() else 0f
                        ProgressBar(progress, Positive)
                        LabeledRow("المدفوع", money(paidTotal), Positive)
                        LabeledRow("المتبقي للدفع", money(unpaidTotal), if (unpaidTotal > 0) Negative else Positive)
                    }
                }
            }

            if (state.bills.isEmpty()) {
                item { EmptyHint(Icons.Filled.ReceiptLong, "لا توجد فواتير في هذه الفترة.") }
                item {
                    FilledTonalButton(onClick = vm::copyBillsFromPreviousMonth) {
                        Icon(Icons.Filled.ContentCopy, null)
                        Spacer(Modifier.width(8.dp))
                        Text("نسخ فواتير الشهر السابق")
                    }
                }
            }

            items(state.bills, key = { it.id }) { bill ->
                BillRow(bill, onToggle = { vm.toggleBillPaid(bill) }, onDelete = { vm.deleteBill(bill) })
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("فاتورة جديدة") }
        )
    }

    if (showAdd) {
        BillDialog(
            onDismiss = { showAdd = false },
            onSave = { name, amount, icon, day ->
                vm.addBill(name, amount, icon, day)
                showAdd = false
            }
        )
    }
}

@Composable
private fun BillRow(bill: Bill, onToggle: () -> Unit, onDelete: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconFor(bill.iconKey), if (bill.paid) Positive else androidx.compose.ui.graphics.Color(0xFFF97316))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    bill.name,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (bill.paid) TextDecoration.LineThrough else null
                )
                Text(
                    "يستحق يوم ${bill.dueDay} · ${money(bill.amount)}",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (bill.paid) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (bill.paid) "مدفوعة" else "غير مدفوعة",
                    tint = if (bill.paid) Positive else MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
