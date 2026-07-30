package com.anhem.translator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iOS-inspired dark palette
val ColorAccent = Color(0xFF007AFF)
val ColorWhite = Color(0xFFFFFFFF)
val ColorBlack = Color(0xFF000000)
val ColorSurface = Color(0xFF1C1C1E)
val ColorSurfaceVariant = Color(0xFF2C2C2E)
val ColorSeparator = Color(0xFF38383A)
val ColorSecondary = Color(0xFF98989D)
val ColorGreen = Color(0xFF34C759)
val ColorRed = Color(0xFFFF3B30)

private val DarkColorScheme = darkColorScheme(
    primary = ColorAccent,
    background = ColorBlack,
    surface = ColorSurface,
    onSurface = ColorWhite,
    surfaceVariant = ColorSurfaceVariant,
    outline = ColorSeparator,
)

@Composable
fun AnhemTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
