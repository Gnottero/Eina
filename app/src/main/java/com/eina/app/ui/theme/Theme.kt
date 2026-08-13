package com.eina.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// DECISIONE: the app is light-only. No dark scheme and no isSystemInDarkTheme hook: the OS dark
// theme must not change the palette.
private val LightColors = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentPrimarySoft,
    onPrimaryContainer = AccentPrimaryDark,
    secondary = AccentPrimaryDark,
    onSecondary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceSunken,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutlineSubtle,
    outlineVariant = LightOutlineSubtle,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground
)

/**
 * Extra island tokens Material3 does not model: colour and alpha of the soft island shadow, and the
 * sunken surface used by tracks, inactive chips and text fields.
 */
@Immutable
data class EinaIslandColors(
    val sunken: Color,
    /** Light sunken surface inside white cards; see [LightSurfaceSunkenSoft]. */
    val sunkenSoft: Color,
    val textSecondary: Color,
    val outlineSubtle: Color,
    val shadow: Color,
    val isDark: Boolean,
    /** Accent ramp (amber to magenta) for hero surfaces, rings and the mark. */
    val accentRamp: List<Color>
)

private val LightIslandColors = EinaIslandColors(
    sunken = LightSurfaceSunken,
    sunkenSoft = LightSurfaceSunkenSoft,
    textSecondary = LightTextSecondary,
    outlineSubtle = LightOutlineSubtle,
    // Cool neutral shadow: the brown one tinted the white of the islands beige.
    shadow = Color(0xFF1A1714),
    isDark = false,
    accentRamp = listOf(AccentRampStart, AccentPrimary, AccentRampEnd)
)

val LocalEinaIslandColors = staticCompositionLocalOf { LightIslandColors }

object EinaTheme {
    val island: EinaIslandColors
        @Composable @ReadOnlyComposable
        get() = LocalEinaIslandColors.current
}

@Composable
fun EinaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEinaIslandColors provides LightIslandColors) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = EinaTypography,
            shapes = EinaShapes,
            content = content
        )
    }
}
