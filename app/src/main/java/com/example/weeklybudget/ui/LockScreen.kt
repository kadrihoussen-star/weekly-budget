package com.example.weeklybudget.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.weeklybudget.security.LockManager
import kotlinx.coroutines.delay

private const val MAX_FAILURES = 5
private const val BLOCK_MILLIS = 30_000L

fun biometricAvailable(context: Context): Boolean =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

private fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}

private fun showBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("فتح ${AppInfo.NAME}")
        .setNegativeButtonText("استعمال الرمز")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}

/** شاشة القفل: لوحة أرقام، وبعد 5 محاولات خاطئة يُحظر الإدخال 30 ثانية. */
@Composable
fun LockScreen(lock: LockManager, promptKey: Int, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val length = lock.pinLength
    val canUseBiometric = remember { lock.biometricEnabled && activity != null && biometricAvailable(context) }

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var failures by remember { mutableIntStateOf(0) }
    var blockedUntil by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val blocked = now < blockedUntil

    // يُعاد عرض البصمة كلما عاد التطبيق إلى الواجهة (promptKey يتغير في onResume).
    LaunchedEffect(promptKey) {
        if (canUseBiometric) activity?.let { showBiometric(it, onUnlocked) }
    }
    LaunchedEffect(blockedUntil) {
        while (System.currentTimeMillis() < blockedUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }

    fun onDigit(d: String) {
        if (blocked || pin.length >= length) return
        error = false
        pin += d
        if (pin.length == length) {
            if (lock.verify(pin)) {
                onUnlocked()
            } else {
                pin = ""
                error = true
                failures++
                if (failures >= MAX_FAILURES) {
                    failures = 0
                    now = System.currentTimeMillis()
                    blockedUntil = now + BLOCK_MILLIS
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandGradient)
            // يمنع وصول اللمسات إلى المحتوى خلف شاشة القفل.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(44.dp))
            Text(AppInfo.NAME, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    blocked -> "محاولات كثيرة، حاول بعد ${((blockedUntil - now) / 1000).coerceAtLeast(1)} ثانية"
                    error -> "الرمز غير صحيح"
                    else -> "أدخل الرمز السري"
                },
                color = Color.White.copy(alpha = 0.9f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                repeat(length) { i ->
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (i < pin.length) Color.White else Color.White.copy(alpha = 0.3f))
                    )
                }
            }

            // لوحة الأرقام تبقى من اليسار إلى اليمين مهما كان اتجاه الواجهة.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { d -> KeyButton(onClick = { onDigit(d) }) { KeyLabel(d) } }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        if (canUseBiometric) {
                            KeyButton(onClick = { activity?.let { showBiometric(it, onUnlocked) } }) {
                                Icon(Icons.Filled.Fingerprint, "بصمة", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        } else {
                            Spacer(Modifier.size(72.dp))
                        }
                        KeyButton(onClick = { onDigit("0") }) { KeyLabel("0") }
                        KeyButton(onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }) { KeyLabel("⌫") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KeyLabel(text: String) {
    Text(text, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun KeyButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
