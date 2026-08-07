package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

val LightBackground = Color(0xFFF7F7F8)
val LightSurface = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF1C1C1E)
val DarkSurface = Color(0xFF2C2C2E)

val AccentPrimary = Color(0xFFFF6A3D)

// --- Stile "island": superfici incassate, testo secondario e bordi appena percettibili.
// DECISIONE: non introduce nuovi colori d'accento, solo neutri derivati dai token di CLAUDE.md,
// cosi' le isole bianche restano leggibili sul background chiaro senza cambiare palette.
val LightSurfaceSunken = Color(0xFFECEDF0)
val LightTextSecondary = Color(0xFF8A8A8E)
val LightOutlineSubtle = Color(0xFFE6E6EA)

val DarkSurfaceSunken = Color(0xFF232325)
val DarkTextSecondary = Color(0xFF98989E)
val DarkOutlineSubtle = Color(0xFF3A3A3C)

val LightOnBackground = Color(0xFF1C1C1E)
val DarkOnBackground = Color(0xFFF7F7F8)

object MuscleGroupColors {
    val ChestPush = Color(0xFFFF6B6B)
    val BackPull = Color(0xFF4D96FF)
    val Legs = Color(0xFF51CF66)
    val Shoulders = Color(0xFF9775FA)
    val Arms = Color(0xFFFFB020)
    val Core = Color(0xFF20C997)
    val Cardio = Color(0xFFF06595)
}
