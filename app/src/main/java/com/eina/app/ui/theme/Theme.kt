package com.eina.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AccentPrimary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1C1E)
)

private val DarkColors = darkColorScheme(
    primary = AccentPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF7F7F8),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF7F7F8)
)

@Composable
fun EinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = EinaTypography,
        shapes = EinaShapes,
        content = content
    )
}
