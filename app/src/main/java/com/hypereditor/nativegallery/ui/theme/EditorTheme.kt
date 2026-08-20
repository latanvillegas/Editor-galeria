package com.hypereditor.nativegallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark AMOLED: Pure Black (#000000)
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF000000)
val AmoledSurfaceVariant = Color(0xFF141416)
val AmoledOutline = Color(0xFF27272A)
val AmoledTextPrimary = Color(0xFFF8FAFC)
val AmoledTextSecondary = Color(0xFF94A3B8)
val AmoledAccent = Color(0xFF38BDF8)

// Light Pure White: Pure White (#FFFFFF)
val PureWhiteBackground = Color(0xFFFFFFFF)
val PureWhiteSurface = Color(0xFFFFFFFF)
val PureWhiteSurfaceVariant = Color(0xFFF1F5F9)
val PureWhiteOutline = Color(0xFFE2E8F0)
val PureWhiteTextPrimary = Color(0xFF0F172A)
val PureWhiteTextSecondary = Color(0xFF64748B)
val PureWhiteAccent = Color(0xFF0284C7)

private val DarkAmoledScheme = darkColorScheme(
    primary = AmoledAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledOutline,
    outlineVariant = Color(0xFF1E2024),
    inverseSurface = PureWhiteBackground,
    inverseOnSurface = PureWhiteTextPrimary
)

private val LightPureScheme = lightColorScheme(
    primary = PureWhiteAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    background = PureWhiteBackground,
    onBackground = PureWhiteTextPrimary,
    surface = PureWhiteSurface,
    onSurface = PureWhiteTextPrimary,
    surfaceVariant = PureWhiteSurfaceVariant,
    onSurfaceVariant = PureWhiteTextSecondary,
    outline = PureWhiteOutline,
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = AmoledBackground,
    inverseOnSurface = AmoledTextPrimary
)

@Composable
fun HyperEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkAmoledScheme else LightPureScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

