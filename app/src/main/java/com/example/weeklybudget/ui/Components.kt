package com.example.weeklybudget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val DEFAULT_CURRENCY = "دج"

/** عملة العرض الحالية؛ حالة Compose فتتحدّث الشاشات تلقائيًا عند تغييرها من الملف الشخصي. */
object Currency {
    var label by mutableStateOf(DEFAULT_CURRENCY)
}

val CurrencyOptions = listOf("دج", "$", "€", "£", "ر.س", "د.إ", "د.م", "د.ت")

fun money(v: Double): String = String.format(Locale.US, "%,.0f", v) + " " + Currency.label

private val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ar"))

fun fmtDate(d: LocalDate): String = d.format(DayMonth)
fun fmtEpoch(epochDay: Long): String = fmtDate(LocalDate.ofEpochDay(epochDay))

/** يقبل الأرقام العربية الهندية والفاصلة العشرية. */
fun parseNumber(s: String): Double? =
    s.map { c -> if (c in '٠'..'٩') '0' + (c - '٠') else c }
        .joinToString("")
        .replace(',', '.')
        .trim()
        .toDoubleOrNull()

@Composable
fun progressColor(progress: Float, over: Boolean): Color = when {
    over || progress >= 1f -> Negative
    progress >= 0.8f -> Warning
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = border
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    track: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 4.dp)
    )
}

@Composable
fun EmptyHint(icon: ImageVector, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
        Text(text, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
fun IconPicker(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { key ->
            val isSelected = key == selected
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            IconBadge(
                icon = iconFor(key),
                tint = tint,
                modifier = Modifier
                    .then(
                        if (isSelected) Modifier.border(2.dp, tint, RoundedCornerShape(14.dp)) else Modifier
                    )
                    .clickable { onSelect(key) }
            )
        }
    }
}

@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد الحذف") },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("حذف", color = Negative) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AddExpenseFab(state: UiState, vm: BudgetViewModel, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    ExtendedFloatingActionButton(
        onClick = { show = true },
        modifier = modifier,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("مصروف جديد") }
    )
    if (show) {
        ExpenseDialog(
            period = state.period,
            categories = state.categories,
            onDismiss = { show = false },
            onSave = { amount, categoryId, note, date ->
                vm.addExpense(amount, categoryId, note, date)
                show = false
            }
        )
    }
}

/** صف أفقي صغير: عنوان على جهة وقيمة على الجهة الأخرى. */
@Composable
fun LabeledRow(
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
