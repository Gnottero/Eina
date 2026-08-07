package com.eina.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceSunken,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutlineSubtle,
    outlineVariant = LightOutlineSubtle,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground
)

private val DarkColors = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceSunken,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutlineSubtle,
    outlineVariant = DarkOutlineSubtle,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground
)

/**
 * Token extra dello stile "island" che Material3 non modella: colore/alpha dell'ombra morbida
 * delle isole e superficie incassata per tracce, chip inattive e campi di testo.
 */
@Immutable
data class EinaIslandColors(
    val sunken: Color,
    val textSecondary: Color,
    val outlineSubtle: Color,
    val shadow: Color,
    val isDark: Boolean
)

private val LightIslandColors = EinaIslandColors(
    sunken = LightSurfaceSunken,
    textSecondary = LightTextSecondary,
    outlineSubtle = LightOutlineSubtle,
    shadow = Color(0xFF1C1C1E),
    isDark = false
)

private val DarkIslandColors = EinaIslandColors(
    sunken = DarkSurfaceSunken,
    textSecondary = DarkTextSecondary,
    outlineSubtle = DarkOutlineSubtle,
    shadow = Color(0xFF000000),
    isDark = true
)

val LocalEinaIslandColors = staticCompositionLocalOf { LightIslandColors }

object EinaTheme {
    val island: EinaIslandColors
        @Composable @ReadOnlyComposable
        get() = LocalEinaIslandColors.current
}

@Composable
fun EinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val islandColors = if (darkTheme) DarkIslandColors else LightIslandColors
    CompositionLocalProvider(LocalEinaIslandColors provides islandColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EinaTypography,
            shapes = EinaShapes,
            content = content
        )
    }
}
