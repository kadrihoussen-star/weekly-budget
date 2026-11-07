package com.example.weeklybudget.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weeklybudget.data.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime

fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 5 -> "ليلة سعيدة"
        hour < 12 -> "صباح الخير"
        hour < 18 -> "طاب يومك"
        else -> "مساء الخير"
    }
}

/** يقتطع صورة مربعة 512×512 من الصورة المختارة، ويحفظها داخل مساحة التطبيق، ويعيد مسارها. */
suspend fun saveProfilePhoto(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                val longest = maxOf(info.size.width, info.size.height)
                decoder.setTargetSampleSize((longest / 1024).coerceAtLeast(1))
            }
        } else {
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }!!
        }
        val side = minOf(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    }.getOrNull()
}

/** صورة المستخدم، أو حرفه الأول على خلفية متدرجة إن لم تُحدَّد صورة أو اسم. */
@Composable
fun Avatar(profile: Profile?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(profile?.photoPath) {
        profile?.photoPath?.takeIf { it.isNotBlank() }?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandGradient),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "صورة الملف الشخصي", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            val initial = profile?.name?.trim()?.firstOrNull()?.uppercase() ?: "؟"
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        }
    }
}

/** تحية أعلى الشاشة الرئيسية؛ تفتح الملف الشخصي عند الضغط. */
@Composable
fun GreetingRow(profile: Profile?, onClick: () -> Unit) {
    val name = profile?.name?.takeIf { it.isNotBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(profile, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            Text(name ?: "أضف ملفك الشخصي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Icon(Icons.Filled.ChevronLeft, contentDescription = "الملف الشخصي", tint = MaterialTheme.colorScheme.secondary)
    }
}

/** بطاقة مختصرة في الإعدادات تفتح الملف الشخصي. */
@Composable
fun ProfileSummaryCard(profile: Profile?, onClick: () -> Unit) {
    val name = profile?.name?.takeIf { it.isNotBlank() }
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name ?: "الملف الشخصي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (name != null) "عرض الملف وتعديله" else "أضف اسمك وصورتك وعملتك",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Filled.ChevronLeft, contentDescription = "عرض الملف الشخصي", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}
