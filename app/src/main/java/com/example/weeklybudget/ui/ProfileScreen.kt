@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.weeklybudget.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.data.Profile
import kotlinx.coroutines.launch

private data class Badge(val icon: ImageVector, val title: String, val hint: String, val achieved: Boolean)

@Composable
fun ProfileScreen(state: UiState, profile: Profile, vm: BudgetViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var city by remember(profile.city) { mutableStateOf(profile.city) }
    var currency by remember(profile.currency) { mutableStateOf(profile.currency) }
    var confirmLogout by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = saveProfilePhoto(context, uri)
            if (path != null) vm.setProfilePhoto(path)
            else Toast.makeText(context, "تعذّر استعمال الصورة", Toast.LENGTH_SHORT).show()
        }
    }

    val nameOk = name.trim().length >= 2
    val changed = name.trim() != profile.name || email.trim() != profile.email ||
        city.trim() != profile.city || currency != profile.currency

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("الملف الشخصي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // السهم يشير إلى اليمين لأن الاتجاه من اليمين إلى اليسار.
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- الترويسة ----
            AppCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box {
                        Avatar(profile, 104.dp)
                        IconButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Filled.CameraAlt, "تغيير الصورة", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(displayPhone(profile.phone), color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "عضو منذ ${fmtEpoch(profile.createdEpochDay)}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (profile.photoPath.isNotBlank()) {
                        TextButton(onClick = { vm.setProfilePhoto("") }) { Text("إزالة الصورة") }
                    }
                }
            }

            // ---- الأوسمة ----
            val badges = listOf(
                Badge(Icons.Filled.AccountBalanceWallet, "مدّخر", "ادّخر مبلغًا في هدف أو من فائض أسبوع",
                    state.goals.any { it.saved > 0 } || state.totalSaved > 0),
                Badge(Icons.Filled.Shield, "محصَّن", "أودع في صندوق الطوارئ",
                    state.emergency.balance > 0),
                Badge(Icons.Filled.EmojiEvents, "منضبط", "أنهِ فترة بلا عجز أسبوعي",
                    state.hasBudget && state.totalSpent > 0 && state.deficitTotal == 0.0),
                Badge(Icons.Filled.CheckCircle, "متحرر من الدين", "سدّد دينًا بالكامل",
                    state.debts.isNotEmpty() && state.debts.all { it.remaining == 0.0 })
            )
            AppCard {
                SectionHeader("الأوسمة (${badges.count { it.achieved }}/${badges.size})")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(badges) { b ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(78.dp)) {
                            IconBadge(
                                b.icon,
                                if (b.achieved) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                                size = 52.dp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                b.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (b.achieved) FontWeight.Bold else FontWeight.Normal,
                                color = if (b.achieved) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                badges.filter { !it.achieved }.firstOrNull()?.let {
                    Text("الوسام التالي: «${it.title}» — ${it.hint}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            // ---- ملخص مالي ----
            val totalPaid = state.debts.sumOf { it.paid }
            AppCard {
                SectionHeader("ملخصي المالي")
                SummaryRow(Icons.Filled.AccountBalanceWallet, "إجمالي المدّخر في الأهداف", money(state.goals.sumOf { it.saved }), Positive)
                SummaryRow(Icons.Filled.Shield, "رصيد صندوق الطوارئ", money(state.emergency.balance), Color(0xFF0EA5E9))
                SummaryRow(Icons.Filled.AccountBalance, "المتبقي من الديون", money(state.debts.sumOf { it.remaining }), Negative)
                SummaryRow(Icons.Filled.CheckCircle, "إجمالي ما سُدِّد من الديون", money(totalPaid), Positive)
                HorizontalDivider()
                LabeledRow("عدد مصاريف هذه الفترة", state.expenses.size.toString())
            }

            // ---- تعديل البيانات ----
            AppCard {
                SectionHeader("بياناتي")
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("الاسم") }, singleLine = true,
                    isError = !nameOk, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني (اختياري)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city, onValueChange = { city = it },
                    label = { Text("الولاية أو المدينة (اختياري)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("عملة العرض", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CurrencyOptions) { c ->
                        FilterChip(selected = c == currency, onClick = { currency = c }, label = { Text(c) })
                    }
                }
                Button(
                    onClick = { vm.updateProfile(name.trim(), email.trim(), city.trim(), currency) },
                    enabled = nameOk && changed,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("حفظ التعديلات") }
            }

            OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Logout, null, tint = Negative)
                Spacer(Modifier.width(8.dp))
                Text("تسجيل الخروج", color = Negative)
            }
            Text(
                "تسجيل الخروج يحذف ملفك الشخصي من هذا الهاتف فقط، وتبقى بياناتك المالية كما هي.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            text = "سيُحذف ملفك الشخصي وصورتك، وسيُطلب منك التسجيل من جديد. بياناتك المالية لن تتأثر.",
            onConfirm = {
                confirmLogout = false
                vm.logout()
                onBack()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
