package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.example.weeklybudget.data.Category
import com.example.weeklybudget.data.Expense
import java.time.LocalDate

@Composable
fun ExpensesScreen(state: UiState, vm: BudgetViewModel) {
    val byId = state.categories.associateBy { it.id }
    val fallback = Category(id = -1, name = "أخرى", iconKey = "other")
    val grouped = state.expenses
        .groupBy { state.period.weekIndexOf(LocalDate.ofEpochDay(it.epochDay)) }
        .toSortedMap()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.expenses.isEmpty()) {
                item { EmptyHint(Icons.Filled.Payments, "لا توجد مصاريف في هذه الفترة بعد.") }
            }
            grouped.forEach { (weekIndex, list) ->
                item(key = "h$weekIndex") {
                    Row(Modifier.padding(top = 6.dp)) {
                        SectionHeader("الأسبوع ${weekIndex + 1}", Modifier.weight(1f))
                        Text(
                            money(list.sumOf { it.amount }),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                items(list, key = { it.id }) { e ->
                    ExpenseRow(e, byId[e.categoryId] ?: fallback, onDelete = { vm.deleteExpense(e) })
                }
            }
        }
        AddExpenseFab(state, vm, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun ExpenseRow(expense: Expense, category: Category, onDelete: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconFor(category.iconKey), categoryColor(category.id))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.SemiBold)
                Text(
                    fmtEpoch(expense.epochDay) + if (expense.note.isNotBlank()) " · ${expense.note}" else "",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(money(expense.amount), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
