package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.data.Category
import com.example.weeklybudget.security.LockManager

@Composable
fun SettingsScreen(state: UiState, vm: BudgetViewModel, lock: LockManager) {
    var incomeText by remember(state.period.ym, state.income) {
        mutableStateOf(if (state.income > 0) state.income.toLong().toString() else "")
    }
    var startDay by remember(state.period.ym, state.startDay) { mutableIntStateOf(state.startDay) }
    var rollover by remember(state.period.ym, state.rollover) { mutableStateOf(state.rollover) }

    var showAddCategory by remember { mutableStateOf(false) }
    var editLimitFor by remember { mutableStateOf<Category?>(null) }
    var deleteFor by remember { mutableStateOf<Category?>(null) }

    val income = parseNumber(incomeText)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppCard {
            SectionHeader("الدخل والفترة")
            AmountField(incomeText, { incomeText = it }, "دخل هذه الفترة ($CURRENCY)")

            Text("يوم بداية الشهر", fontWeight = FontWeight.SemiBold)
            Text(
                "تبدأ الفترة في هذا اليوم من كل شهر (مثلًا يوم استلام الراتب) وتنتهي قبله بيوم في الشهر التالي.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = { if (startDay > 1) startDay-- }) {
                    Icon(Icons.Filled.Remove, "إنقاص")
                }
                Text(
                    startDay.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
                FilledTonalIconButton(onClick = { if (startDay < 31) startDay++ }) {
                    Icon(Icons.Filled.Add, "زيادة")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ترحيل الفائض", fontWeight = FontWeight.SemiBold)
                    Text(
                        "يُضاف فائض الأسبوع إلى الأسبوع التالي. أما العجز فلا يُرحَّل أبدًا، ويُسجَّل في بطاقة العجز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(checked = rollover, onCheckedChange = { rollover = it })
            }

            Button(
                onClick = { income?.let { vm.saveBudget(it, startDay, rollover) } },
                enabled = income != null && income >= 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ") }

            Text(
                "الفترة الحالية: ${fmtDate(state.period.start)} - ${fmtDate(state.period.end)}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        AppCard {
            SectionHeader("الفئات الأسبوعية")
            Text(
                "فئات المصروف التي تظهر في كل أسبوع. السقف الأسبوعي اختياري وينبّهك عند تجاوزه.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
            state.categories.forEachIndexed { index, c ->
                if (index > 0) HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(iconFor(c.iconKey), categoryColor(c.id), size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (c.weeklyLimit > 0) "السقف ${money(c.weeklyLimit)} أسبوعيًا" else "بلا سقف",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { editLimitFor = c }) {
                        Icon(Icons.Filled.Edit, "تعديل السقف", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { deleteFor = c }) {
                        Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            OutlinedButton(onClick = { showAddCategory = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("إضافة فئة جديدة")
            }
        }

        AppLockCard(lock)

        BackupCard(vm)

        AboutCard()
    }

    if (showAddCategory) {
        CategoryDialog(onDismiss = { showAddCategory = false }, onSave = { n, i, l ->
            vm.addCategory(n, i, l); showAddCategory = false
        })
    }
    editLimitFor?.let { c ->
        AmountDialog(
            title = "السقف الأسبوعي لـ «${c.name}»",
            initial = c.weeklyLimit,
            confirmLabel = "حفظ",
            allowZero = true,
            onDismiss = { editLimitFor = null },
            onSave = { vm.updateCategoryLimit(c, it); editLimitFor = null }
        )
    }
    deleteFor?.let { c ->
        ConfirmDialog(
            text = "ستُحذف الفئة «${c.name}»، وتظهر مصاريفها السابقة تحت «أخرى».",
            onConfirm = { vm.deleteCategory(c); deleteFor = null },
            onDismiss = { deleteFor = null }
        )
    }
}
