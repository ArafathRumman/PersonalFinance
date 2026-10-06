package com.taka.personalfinance.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

private val LightFinance = FinanceColors(
    income = Color(0xFF1E9E6A),
    expense = Color(0xFFE5484D),
    warning = Color(0xFFF59E0B),
    heroStart = Color(0xFF2F6BED),
    heroEnd = Color(0xFF6C8CFF),
)

private val DarkFinance = FinanceColors(
    income = Color(0xFF5FD39A),
    expense = Color(0xFFFF8A8E),
    warning = Color(0xFFFFC454),
    heroStart = Color(0xFF2A4FB8),
    heroEnd = Color(0xFF3E6AE0),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinance }

val MaterialTheme.finance: FinanceColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinanceColors.current

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B1F52),
    secondary = Color(0xFF5B6B8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E8F3),
    onSecondaryContainer = Color(0xFF1B2538),
    tertiary = Color(0xFF14A38B),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF16181D),
    surface = Color.White,
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFE9ECF2),
    onSurfaceVariant = Color(0xFF5A6070),
    outline = Color(0xFFB5BBC8),
    outlineVariant = Color(0xFFDDE1E9),
    error = Color(0xFFD93036),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AA9FF),
    onPrimary = Color(0xFF0A1B4D),
    primaryContainer = Color(0xFF223A7A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFA9B6D3),
    onSecondary = Color(0xFF1B2538),
    secondaryContainer = Color(0xFF2B3550),
    onSecondaryContainer = Color(0xFFDCE3F5),
    tertiary = Color(0xFF55D6BE),
    background = Color(0xFF0B0C0F),
    onBackground = Color(0xFFE8EAEE),
    surface = Color(0xFF1A1C21),
    onSurface = Color(0xFFE8EAEE),
    surfaceVariant = Color(0xFF262932),
    onSurfaceVariant = Color(0xFFA7ADBB),
    outline = Color(0xFF545A68),
    outlineVariant = Color(0xFF343843),
    error = Color(0xFFFF8A8E),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PersonalFinanceTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFinanceColors provides (if (dark) DarkFinance else LightFinance)) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            shapes = AppShapes,
            content = content,
        )
    }
}

val ChartPalette = listOf(
    Color(0xFF3B6EF5), Color(0xFF14A38B), Color(0xFFF59E0B), Color(0xFFE5484D), Color(0xFF8B5CF6),
    Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF84CC16), Color(0xFFF97316), Color(0xFF64748B),
)
