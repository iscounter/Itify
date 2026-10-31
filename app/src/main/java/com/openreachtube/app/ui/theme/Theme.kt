package com.openreachtube.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = Primary,
    secondary = PrimaryDim,
    background = Ink,
    surface = Surface,
    surfaceVariant = SurfaceHigh,
    onPrimary = Ink,
    onBackground = OnInk,
    onSurface = OnInk,
    onSurfaceVariant = Muted,
    error = Danger,
)

@Composable
fun OpenReachTheme(content: @Composable () -> Unit) {
    // Dark-first video app; light theme intentionally not themed yet.
    MaterialTheme(colorScheme = DarkScheme, typography = OpenReachTypography, content = content)
}
