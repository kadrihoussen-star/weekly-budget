package com.example.weeklybudget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.data.Profile
import com.example.weeklybudget.data.WeekAdjustment
import com.example.weeklybudget.domain.CategorySpend
import com.example.weeklybudget.domain.WeekSummary

@Composable
fun HomeScreen(
    state: UiState,
    vm: BudgetViewModel,
    profile: Profile?,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var saveFor by remember { mutableStateOf<WeekSummary?>(null) }
    var coverFor by remember { mutableStateOf<WeekSummary?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GreetingRow(profile, onOpenProfile) }
            item { HeroCard(state) }

            if (!state.hasBudget) {
                item { SetupCard(onOpenSettings) }
            } else if (state.distributable < 0) {
                item { OverCommittedCard(state.distributable) }
            }

            item { ObligationsCard(state) }

            if (state.deficitTotal > 0) {
                item { DeficitCard(state) }
            }

            item { SectionHeader("الأسابيع الأربعة") }
            items(state.weeks, key = { it.index }) { week ->
                WeekCard(
                    week = week,
                    isCurrent = week.index == state.currentWeek,
                    onSave = { saveFor = week },
                    onCover = { coverFor = week },
                    onUndo = { vm.undoAdjustment(it) }
                )
            }
        }
        AddExpenseFab(state, vm, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }

    saveFor?.let { week ->
        SaveSurplusDialog(
            surplus = week.surplus,
            goals = state.goals,
            onDismiss = { saveFor = null },
            onSave = { amount, goalId ->
                vm.saveWeekSurplus(week.index, amount, goalId)
                saveFor = null
            }
        )
    }
    coverFor?.let { week ->
        val balance = state.emergency.balance.coerceAtLeast(0.0)
        val max = minOf(week.deficit, balance)
        LimitedAmountDialog(
            title = "تغطية عجز الأسبوع ${week.index + 1}",
            hint = "العجز ${money(week.deficit)} · رصيد الطوارئ ${money(balance)}",
            initial = max,
            max = max,
            confirmLabel = "تغطية",
            onDismiss = { coverFor = null },
            onSave = {
                vm.coverWeekDeficit(week.index, it)
                coverFor = null
            }
        )
    }
}

@Composable
private fun HeroCard(state: UiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(BrandGradient)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, null, tint = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${fmtDate(state.period.start)} - ${fmtDate(state.period.end)}",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Column {
                Text("المتبقي للمصروف", color = Color.White.copy(alpha = 0.8f))
                Text(
                    money(state.remaining),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            ProgressBar(
                progress = state.progress,
                color = Color.White,
                track = Color.White.copy(alpha = 0.25f),
                height = 10.dp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStat("الدخل", money(state.income))
                HeroStat("الالتزامات", money(state.obligations))
                HeroStat("المصروف", money(state.totalSpent))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SetupCard(onOpenSettings: () -> Unit) {
    AppCard {
        Text("ابدأ بتحديد دخل هذه الفترة", fontWeight = FontWeight.Bold)
        Text(
            "حدّد دخلك الشهري ويوم بداية الشهر، وسأقسّمه على 4 أسابيع بعد خصم الفواتير والادخار والديون.",
            color = MaterialTheme.colorScheme.secondary
        )
        Button(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, null)
            Spacer(Modifier.width(8.dp))
            Text("فتح الإعدادات")
        }
    }
}

@Composable
private fun OverCommittedCard(distributable: Double) {
    AppCard(border = BorderStroke(1.5.dp, Negative)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Warning, Negative)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("الالتزامات تتجاوز الدخل", fontWeight = FontWeight.Bold, color = Negative)
                Text(
                    "العجز ${money(-distributable)}. راجع الفواتير أو أقساط الديون أو مبالغ الادخار والطوارئ.",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ObligationsCard(state: UiState) {
    AppCard {
        SectionHeader("الالتزامات الشهرية")
        ObligationRow(Icons.Filled.ReceiptLong, "الفواتير", state.billsTotal, Color(0xFFF97316))
        ObligationRow(Icons.Filled.AccountBalance, "أقساط الديون", state.debtPlanned, Negative)
        ObligationRow(Icons.Filled.AccountBalanceWallet, "الادخار", state.savingsPlanned, Positive)
        ObligationRow(Icons.Filled.Shield, "صندوق الطوارئ", state.emergencyPlanned, Color(0xFF0EA5E9))
        HorizontalDivider()
        LabeledRow(
            "المتاح للتوزيع على الأسابيع", money(state.distributable), bold = true,
            valueColor = if (state.distributable < 0) Negative else MaterialTheme.colorScheme.onSurface
        )
        LabeledRow("نصيب كل أسبوع", money(state.distributable / 4))
    }
}

@Composable
private fun ObligationRow(icon: ImageVector, label: String, value: Double, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f))
        Text(money(value), fontWeight = FontWeight.SemiBold)
    }
}

/** سجل العجز: الأسابيع التي تجاوزت ميزانيتها. العجز لا يُرحَّل، ويمكن تغطيته من صندوق الطوارئ. */
@Composable
private fun DeficitCard(state: UiState) {
    AppCard(border = BorderStroke(1.5.dp, Negative.copy(alpha = 0.7f))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Warning, Negative)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("العجز المسجّل", fontWeight = FontWeight.Bold, color = Negative)
                Text(
                    "لا يُرحَّل العجز إلى الأسبوع التالي.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(money(state.deficitTotal), fontWeight = FontWeight.Bold, color = Negative)
        }
        state.weeks.filter { it.deficit > 0 }.forEach { w ->
            LabeledRow("الأسبوع ${w.index + 1}", money(w.deficit), Negative)
        }
    }
}

@Composable
private fun WeekCard(
    week: WeekSummary,
    isCurrent: Boolean,
    onSave: () -> Unit,
    onCover: () -> Unit,
    onUndo: (WeekAdjustment) -> Unit
) {
    AppCard(border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.DateRange, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الأسبوع ${week.index + 1}", fontWeight = FontWeight.Bold)
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "الحالي",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "${fmtDate(week.start)} - ${fmtDate(week.end)}",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("المتبقي", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                Text(
                    money(week.remaining),
                    fontWeight = FontWeight.Bold,
                    color = if (week.remaining < 0) Negative else Positive
                )
            }
        }

        ProgressBar(week.progress, progressColor(week.progress, week.isOver))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("المتاح ${money(week.available)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Text("المصروف ${money(week.spent)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        if (week.carried > 0) {
            Text(
                "يتضمن فائضًا مرحّلًا ${money(week.carried)}",
                style = MaterialTheme.typography.bodySmall,
                color = Positive
            )
        }
        if (week.deficit > 0) {
            Text(
                "عجز ${money(week.deficit)} — لا يُرحَّل إلى الأسبوع التالي",
                style = MaterialTheme.typography.bodySmall,
                color = Negative
            )
        }

        if (week.surplus > 0 || week.deficit > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (week.surplus > 0) {
                    FilledTonalButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AccountBalanceWallet, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ادخر المتبقي")
                    }
                }
                if (week.deficit > 0) {
                    FilledTonalButton(onClick = onCover, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("غطِّ من الطوارئ")
                    }
                }
            }
        }

        week.adjustments.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (a.kind == WeekAdjustment.SAVE) "ادُّخر " else "غُطّي من الطوارئ ") + money(a.amount),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (a.kind == WeekAdjustment.SAVE) Positive else MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = { onUndo(a) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "تراجع", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        HorizontalDivider()
        week.byCategory.forEach { CategoryLine(it) }
    }
}

@Composable
private fun CategoryLine(cs: CategorySpend) {
    val c = cs.category
    val hasLimit = c.weeklyLimit > 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(iconFor(c.iconKey), categoryColor(c.id), size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.name)
                Text(
                    if (hasLimit) "${money(cs.spent)} / ${money(c.weeklyLimit)}" else money(cs.spent),
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasLimit && cs.spent > c.weeklyLimit) Negative else MaterialTheme.colorScheme.onSurface
                )
            }
            if (hasLimit) {
                ProgressBar(
                    progress = cs.ratio,
                    color = progressColor(cs.ratio, cs.spent > c.weeklyLimit),
                    height = 5.dp
                )
            }
        }
    }
}
