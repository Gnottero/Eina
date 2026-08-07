package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

// Palette definitiva: app solo in light mode, bianco + viola.
// I neutri hanno una punta di viola cosi' le isole bianche staccano dal background senza bordi.
val LightBackground = Color(0xFFF6F5FB)
val LightSurface = Color(0xFFFFFFFF)

/** Accento primario: viola. CTA, stati attivi, valori in evidenza. */
val AccentPrimary = Color(0xFF7A5AF8)
val AccentPrimaryDark = Color(0xFF5B3FD6)
val AccentPrimarySoft = Color(0xFFEDE8FF)

val LightSurfaceSunken = Color(0xFFEFEDF7)
val LightTextSecondary = Color(0xFF7C7A93)
val LightOutlineSubtle = Color(0xFFE7E4F3)
val LightOnBackground = Color(0xFF1B1830)

object MuscleGroupColors {
    val ChestPush = Color(0xFFFF6B6B)
    val BackPull = Color(0xFF4D96FF)
    val Legs = Color(0xFF51CF66)
    val Shoulders = Color(0xFF9775FA)
    val Arms = Color(0xFFFFB020)
    val Core = Color(0xFF20C997)
    val Cardio = Color(0xFFF06595)
}
