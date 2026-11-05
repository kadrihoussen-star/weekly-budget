package com.example.weeklybudget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.weeklybudget.R

/**
 * شاشة التسجيل عند أول فتح. الرقم يُحفظ على الهاتف فقط ولا يُرسَل رمز تحقق،
 * لأن التحقق برسالة SMS يحتاج خادمًا.
 */
@Composable
fun RegisterScreen(vm: BudgetViewModel) {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(Countries.first()) }
    var phone by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val nameOk = name.trim().length >= 2
    val normalized = normalizePhone(country.code, phone)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(BrandGradient)
                .statusBarsPadding()
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp))) {
                    Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
                }
                Text(AppInfo.NAME, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("أهلًا بك! أنشئ حسابك للبدء", color = Color.White.copy(alpha = 0.85f))
            }
        }

        Column(
            Modifier
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("إنشاء حساب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                singleLine = true,
                isError = showErrors && !nameOk,
                supportingText = { if (showErrors && !nameOk) Text("أدخل اسمًا من حرفين على الأقل") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(displayPhone(country.code))
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        Countries.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name}  ${displayPhone(c.code)}") },
                                onClick = { country = c; menuOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(15) },
                    label = { Text("رقم الهاتف") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                    singleLine = true,
                    isError = showErrors && normalized == null,
                    supportingText = {
                        if (showErrors && normalized == null) {
                            Text(if (country.code == "+213") "مثال: 0550 12 34 56" else "رقم غير صالح")
                        }
                    },
                    textStyle = TextStyle(textDirection = TextDirection.Ltr),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    showErrors = true
                    if (nameOk && normalized != null) vm.registerProfile(name.trim(), normalized)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("تسجيل", fontWeight = FontWeight.Bold) }

            Text(
                "يُحفظ اسمك ورقمك على هذا الهاتف فقط ولا يُرسَلان إلى أي جهة، ولا يُرسَل رمز تحقق. " +
                    "يمكنك تعديلهما أو حذفهما لاحقًا من الملف الشخصي.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
