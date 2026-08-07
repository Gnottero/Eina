package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

// Palette definitiva: app solo in light mode, bianco + arancio tramonto.
// I neutri hanno una punta di caldo cosi' le isole bianche staccano dal background senza bordi.
val LightBackground = Color(0xFFFBF6F2)
val LightSurface = Color(0xFFFFFFFF)

/** Accento primario: arancio tramonto. CTA, stati attivi, valori in evidenza. */
val AccentPrimary = Color(0xFFF97348)
val AccentPrimaryDark = Color(0xFFD4501F)
val AccentPrimarySoft = Color(0xFFFFEADF)

val LightSurfaceSunken = Color(0xFFF5EDE7)
val LightTextSecondary = Color(0xFF8A7D75)
val LightOutlineSubtle = Color(0xFFF0E5DD)
val LightOnBackground = Color(0xFF2A1B14)

object MuscleGroupColors {
    val ChestPush = Color(0xFFFF6B6B)
    val BackPull = Color(0xFF4D96FF)
    val Legs = Color(0xFF51CF66)
    val Shoulders = Color(0xFF9775FA)
    val Arms = Color(0xFFFFB020)
    val Core = Color(0xFF20C997)
    val Cardio = Color(0xFFF06595)
}
