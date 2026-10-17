package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MishaDarkColorScheme = darkColorScheme(
    primary = CyanCore,
    onPrimary = DarkBg,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = CyanGlow,
    secondary = PurpleCore,
    onSecondary = DarkBg,
    secondaryContainer = PurpleDeep,
    onSecondaryContainer = PurpleGlow,
    tertiary = NeonEmerald,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderPurple,
    error = NeonCrimson,
    onError = TextPrimary
)

@Composable
fun MishaDarkTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MishaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MishaDarkTheme(darkTheme, dynamicColor, content)
}

