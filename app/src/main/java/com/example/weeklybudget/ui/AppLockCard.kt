package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.security.LockManager

private enum class LockDialog { Setup, Change, Disable }

@Composable
fun AppLockCard(lock: LockManager) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(lock.isEnabled) }
    var biometric by remember { mutableStateOf(lock.biometricEnabled) }
    var dialog by remember { mutableStateOf<LockDialog?>(null) }
    val biometricAvailable = remember { biometricAvailable(context) }

    AppCard {
        SectionHeader("قفل التطبيق")
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Lock, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("القفل برمز سري", fontWeight = FontWeight.SemiBold)
                Text(
                    "يُطلب الرمز عند فتح التطبيق وعند العودة إليه، وتُخفى محتوياته في قائمة التطبيقات الأخيرة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { dialog = if (it) LockDialog.Setup else LockDialog.Disable }
            )
        }

        if (enabled) {
            if (biometricAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("الفتح بالبصمة", fontWeight = FontWeight.SemiBold)
                        Text(
                            "تُستعمل مع الرمز، ويبقى الرمز بديلًا دائمًا.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = biometric,
                        onCheckedChange = { lock.biometricEnabled = it; biometric = it }
                    )
                }
            }
            OutlinedButton(onClick = { dialog = LockDialog.Change }, modifier = Modifier.fillMaxWidth()) {
                Text("تغيير الرمز")
            }
        }
    }

    when (dialog) {
        LockDialog.Setup -> PinFormDialog(
            title = "إنشاء رمز سري",
            labels = listOf("الرمز الجديد (4 إلى 6 أرقام)", "تأكيد الرمز"),
            onDismiss = { dialog = null }
        ) { v ->
            validateNewPin(v[0], v[1]) ?: run {
                lock.setPin(v[0])
                enabled = true
                dialog = null
                null
            }
        }

        LockDialog.Change -> PinFormDialog(
            title = "تغيير الرمز",
            labels = listOf("الرمز الحالي", "الرمز الجديد (4 إلى 6 أرقام)", "تأكيد الرمز الجديد"),
            onDismiss = { dialog = null }
        ) { v ->
            if (!lock.verify(v[0])) "الرمز الحالي غير صحيح"
            else validateNewPin(v[1], v[2]) ?: run {
                lock.setPin(v[1])
                dialog = null
                null
            }
        }

        LockDialog.Disable -> PinFormDialog(
            title = "إيقاف القفل",
            labels = listOf("الرمز الحالي"),
            onDismiss = { dialog = null }
        ) { v ->
            if (!lock.verify(v[0])) "الرمز غير صحيح"
            else {
                lock.disable()
                enabled = false
                biometric = false
                dialog = null
                null
            }
        }

        null -> Unit
    }
}

private fun validateNewPin(pin: String, confirm: String): String? = when {
    pin.length !in 4..6 -> "الرمز يجب أن يكون من 4 إلى 6 أرقام"
    pin != confirm -> "الرمزان غير متطابقين"
    else -> null
}

/** [onSubmit] يعيد رسالة خطأ لعرضها، أو null عند النجاح (وهو من يغلق النافذة). */
@Composable
private fun PinFormDialog(
    title: String,
    labels: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (List<String>) -> String?
) {
    var values by remember { mutableStateOf(List(labels.size) { "" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                labels.forEachIndexed { i, label ->
                    OutlinedTextField(
                        value = values[i],
                        onValueChange = { v ->
                            values = values.toMutableList().also { it[i] = v.filter(Char::isDigit).take(6) }
                        },
                        label = { Text(label) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                error?.let { Text(it, color = Negative) }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSubmit(values) }) { Text("تأكيد") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
