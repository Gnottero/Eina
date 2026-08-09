package com.eina.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// DECISIONE: l'app e' solo in light mode (bianco + viola). Niente schema scuro e niente
// aggancio a isSystemInDarkTheme: il tema scuro dell'OS non deve cambiare la palette.
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
 * Token extra dello stile "island" che Material3 non modella: colore/alpha dell'ombra morbida
 * delle isole e superficie incassata per tracce, chip inattive e campi di testo.
 */
@Immutable
data class EinaIslandColors(
    val sunken: Color,
    val textSecondary: Color,
    val outlineSubtle: Color,
    val shadow: Color,
    val isDark: Boolean,
    /** Rampa dell'accento (ambra -> magenta) per hero, anelli e marchio. */
    val accentRamp: List<Color>
)

private val LightIslandColors = EinaIslandColors(
    sunken = LightSurfaceSunken,
    textSecondary = LightTextSecondary,
    outlineSubtle = LightOutlineSubtle,
    // Ombra neutra fredda: quella marrone tingeva il bianco delle isole di beige.
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
