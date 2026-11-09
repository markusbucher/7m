package de.kirchheim.handball.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Farben wie in Version 1 (res/values und res/values-night). */
@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val fg: Color,
    val muted: Color,
    val primary: Color,
    val accent: Color,
    val onAccent: Color,
    val highlight: Color,
    val win: Color,
    val draw: Color,
    val loss: Color
)

private val Light = AppColors(
    bg = Color(0xFFF4F6F8), card = Color(0xFFFFFFFF), fg = Color(0xFF1B2430), muted = Color(0xFF5F6B7A),
    primary = Color(0xFF0065A3), accent = Color(0xFF0065A3), onAccent = Color.White, highlight = Color(0xFFDCEAF5),
    win = Color(0xFF1B7F3B), draw = Color(0xFF8A6D00), loss = Color(0xFFB3261E)
)

private val Dark = AppColors(
    bg = Color(0xFF10151B), card = Color(0xFF1A222C), fg = Color(0xFFE8EDF2), muted = Color(0xFF9AA6B5),
    primary = Color(0xFF0065A3), accent = Color(0xFF5BAEE8), onAccent = Color.White, highlight = Color(0xFF17304A),
    win = Color(0xFF4CC47A), draw = Color(0xFFE0B93A), loss = Color(0xFFFF8A80)
)

val LocalAppColors = staticCompositionLocalOf { Light }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(primary = c.accent, background = c.bg, surface = c.card, onBackground = c.fg, onSurface = c.fg)
    } else {
        lightColorScheme(primary = c.primary, background = c.bg, surface = c.card, onBackground = c.fg, onSurface = c.fg)
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
