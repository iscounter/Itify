package com.openreachtube.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = ZRed,
    onPrimary = Color.White,
    secondary = ZRedDark,
    onSecondary = Color.White,
    background = ZBackground,
    onBackground = ZTextDark,
    surface = ZSurface,
    onSurface = ZTextDark,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = ZTextMuted,
    outline = ZOutline,
    error = Danger,
)

private val DarkScheme = darkColorScheme(
    primary = ZRed,
    onPrimary = Color.White,
    secondary = ZRedDark,
    background = Ink,
    surface = Surface,
    surfaceVariant = SurfaceHigh,
    onBackground = OnInk,
    onSurface = OnInk,
    onSurfaceVariant = Muted,
    error = Danger,
)

@Composable
fun OpenReachTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = OpenReachTypography,
        content = content,
    )
}
