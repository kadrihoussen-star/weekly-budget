package com.example.weeklybudget.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Positive = Color(0xFF16A34A)
val Warning = Color(0xFFF59E0B)
val Negative = Color(0xFFDC2626)

val BrandGradient = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF14B8A6)))

/** ألوان تُوزَّع على الفئات بحسب رقمها. */
private val CategoryPalette = listOf(
    Color(0xFF0EA5E9), Color(0xFFF97316), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF10B981), Color(0xFFEAB308), Color(0xFF6366F1), Color(0xFFEF4444)
)

fun categoryColor(id: Long): Color = CategoryPalette[((id % CategoryPalette.size) + CategoryPalette.size).toInt() % CategoryPalette.size]

private val Light = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF64748B),
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurface = Color(0xFF0F172A),
    onBackground = Color(0xFF0F172A)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    secondary = Color(0xFF94A3B8),
    background = Color(0xFF0B1220),
    surface = Color(0xFF162033),
    surfaceVariant = Color(0xFF2B3A52),
    onSurface = Color(0xFFE2E8F0),
    onBackground = Color(0xFFE2E8F0)
)

@Composable
fun WeeklyBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
