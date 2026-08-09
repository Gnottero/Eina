package com.eina.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.eina.app.R

/**
 * Inter (SIL Open Font License 1.1), bundlato in res/font. Fino al restyle il font era
 * FontFamily.Default, cioe' Roboto: perfettamente leggibile ma senza carattere, e con le cifre
 * di larghezza variabile che facevano ballare i numeri del cronometro e delle tabelle serie.
 */
val EinaFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/**
 * Taglio "Display" di Inter: stessa famiglia, disegno stretto pensato per i corpi grandi
 * (interlettera piu' fitta, terminali piu' corti). Va usato solo dai titoli e dai numeroni,
 * come SF Pro Display sta a SF Pro Text — sotto i 20sp peggiora la lettura invece di aiutarla.
 */
val EinaDisplayFontFamily = FontFamily(
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold, FontWeight.Bold)
)

/**
 * Cifre a larghezza fissa. Servono ovunque un numero cambi in continuazione (cronometro, timer
 * di recupero, durata dell'allenamento): con le cifre proporzionali la riga si allarga e si
 * stringe a ogni secondo.
 */
const val TabularFigures = "tnum"

val EinaTypography = Typography(
    // I numeroni delle metriche: corpo grande, interlettera negativa, taglio Display.
    displayLarge = TextStyle(
        fontFamily = EinaDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = TabularFigures
    ),
    displayMedium = TextStyle(
        fontFamily = EinaDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.1).sp,
        fontFeatureSettings = TabularFigures
    ),
    displaySmall = TextStyle(
        fontFamily = EinaDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = TabularFigures
    ),
    // Titolo di schermata.
    headlineMedium = TextStyle(
        fontFamily = EinaDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = EinaDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = EinaDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.1).sp
    ),
    bodySmall = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = (-0.1).sp
    ),
    // Etichette minuscole in maiuscoletto: qui l'interlettera va allargata, non stretta.
    labelMedium = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    )
)
