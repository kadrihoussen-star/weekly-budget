package com.example.weeklybudget.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private fun toast(context: Context, text: String) =
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

@Composable
fun BackupCard(vm: BudgetViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<String?>(null) }
    val fileName = "weekly-budget-backup-${LocalDate.now()}.json"

    // حفظ النسخة في مكان يختاره المستخدم (Google Drive أو ذاكرة الهاتف).
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: error("no stream")
                }
            }.isSuccess
            toast(context, if (ok) "تم حفظ النسخة الاحتياطية" else "تعذّر حفظ الملف")
        }
    }

    // اختيار ملف نسخة لاستعادته.
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()
            }
            if (text == null) toast(context, "تعذّرت قراءة الملف") else pendingImport = text
        }
    }

    AppCard {
        SectionHeader("النسخ الاحتياطي")
        Text(
            "احفظ بياناتك في ملف على Google Drive أو أرسله إلى بريدك الإلكتروني، " +
                "واستعده لاحقًا على هذا الهاتف أو أي هاتف آخر.",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = { saveLauncher.launch(fileName) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CloudUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("حفظ نسخة احتياطية")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val json = vm.exportJson()
                            val file = withContext(Dispatchers.IO) {
                                val dir = File(context.cacheDir, "backups").apply { mkdirs() }
                                File(dir, fileName).apply { writeText(json) }
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - ${AppInfo.NAME}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "إرسال النسخة عبر"))
                        } catch (e: Exception) {
                            toast(context, "تعذّر تجهيز الملف للإرسال")
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Email, null)
                Spacer(Modifier.width(6.dp))
                Text("إرسال بالإيميل")
            }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.CloudDownload, null)
                Spacer(Modifier.width(6.dp))
                Text("استعادة")
            }
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("استعادة النسخة؟") },
            text = { Text("ستُستبدل كل بياناتك الحالية (الدخل والمصاريف والفواتير والادخار والديون) بمحتوى الملف.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val ok = vm.importJson(text)
                        toast(context, if (ok) "تمت استعادة البيانات" else "الملف غير صالح، لم يتغير شيء")
                    }
                }) { Text("استعادة", color = Negative) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("إلغاء") } }
        )
    }
}
