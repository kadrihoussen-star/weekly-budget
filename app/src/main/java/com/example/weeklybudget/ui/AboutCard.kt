package com.example.weeklybudget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.example.weeklybudget.R

@Composable
fun AboutCard() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            (info.versionName ?: "-") to PackageInfoCompat.getLongVersionCode(info)
        }.getOrDefault("-" to 0L)
    }

    AppCard {
        SectionHeader("حول التطبيق")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(AppInfo.NAME, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("الإصدار ${version.first}", color = MaterialTheme.colorScheme.secondary)
            }
        }

        Text(AppInfo.DESCRIPTION, color = MaterialTheme.colorScheme.secondary)

        HorizontalDivider()
        LabeledRow("المطوّر", AppInfo.DEVELOPER, bold = true)
        LabeledRow("الإصدار", version.first)
        LabeledRow("رقم البناء", version.second.toString())
        LabeledRow("تاريخ الإصدار", AppInfo.RELEASE_DATE)
        LabeledRow("يعمل على", "أندرويد 8.0 فما فوق")
    }
}
