package com.hypereditor.nativegallery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF111215)
val DarkSurface = Color(0xFF191B20)
val DarkSurfaceElevated = Color(0xFF22252D)
val AccentPrimary = Color(0xFF38BDF8)
val TextPrimary = Color(0xFFF1F5F9)

private val DarkScheme = darkColorScheme(
    primary = AccentPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onPrimary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun HyperEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
