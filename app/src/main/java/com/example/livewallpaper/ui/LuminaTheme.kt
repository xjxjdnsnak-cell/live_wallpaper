package com.example.livewallpaper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LuminaBackground = Color(0xFFF8F7FB)
val LuminaSurface = Color(0xFFFFFFFF)
val LuminaPrimary = Color(0xFF7E6AAE)
val LuminaPrimaryDark = Color(0xFF4B3B78)
val LuminaSecondary = Color(0xFFEEEAF7)
val LuminaText = Color(0xFF1F1F28)
val LuminaMuted = Color(0xFF6F6B7A)

private val LuminaColorScheme = lightColorScheme(
    primary = LuminaPrimary,
    onPrimary = Color.White,
    secondary = LuminaSecondary,
    onSecondary = LuminaPrimaryDark,
    background = LuminaBackground,
    onBackground = LuminaText,
    surface = LuminaSurface,
    onSurface = LuminaText,
    surfaceVariant = LuminaSecondary,
    onSurfaceVariant = LuminaMuted,
    error = Color(0xFFB5486A),
)

@Composable
fun LuminaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuminaColorScheme,
        content = content,
    )
}
