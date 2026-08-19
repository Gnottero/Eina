package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

// Final palette; the app is light-only.
// The neutrals dropped their brown cast (the former #FBF6F2 background yellowed the exercise
// artwork and made white islands look dirty). They are now barely warm greys, so the only colour
// on a page comes from the accent.
val LightBackground = Color(0xFFF4F3F1)
val LightSurface = Color(0xFFFFFFFF)

/** Primary accent, sunset orange: CTAs, active states, headline values. */
val AccentPrimary = Color(0xFFF97348)
val AccentPrimaryDark = Color(0xFFD4501F)
val AccentPrimarySoft = Color(0xFFFFEADF)

/**
 * Accent ramp, amber to magenta, for large surfaces (Dashboard hero, progress rings, the mark): a
 * solid orange over a wide area reads as a block of paint, while the ramp adds depth without
 * introducing colours foreign to the palette.
 */
val AccentRampStart = Color(0xFFFFA23A)
val AccentRampEnd = Color(0xFFF9436B)

/**
 * Sunken surface: tracks, inactive chips, fields, sheet rows. Kept a whisper away from white — the
 * former #EFEEEB read as a grey panel laid on the card and weighed more than what it held. It is
 * only ever used *inside* a white surface: on the page background it would be invisible, and the
 * controls that sit there (see IslandSecondaryButton) are white islands with a thin outline.
 */
val LightSurfaceSunken = Color(0xFFF6F5F3)

/**
 * Even lighter sunken surface, for large areas *inside* a white card: the summary set table and the
 * weight and reps fields. With the full grey those boxes weighed more than the numbers they hold.
 */
val LightSurfaceSunkenSoft = Color(0xFFFAFAF8)
val LightTextSecondary = Color(0xFF7C7A75)
val LightOutlineSubtle = Color(0xFFEDECE9)
val LightOnBackground = Color(0xFF1C1B19)

/**
 * One colour per measured quantity. Not a new palette: the muscle group colours reused with a
 * fixed meaning, so metrics are recognisable without reading the label.
 *
 * The colour goes on the icon and the label, never on the number: in a row of three metrics a
 * tinted value would hide which one matters.
 */
object MetricColors {
    val Volume = AccentPrimary
    val Sets = Color(0xFF9775FA)
    val Duration = Color(0xFF4D96FF)
    val Records = AccentRampEnd
    val Bodyweight = Color(0xFF20C997)
    val Streak = Color(0xFFF06595)
    // Watch data: the heart takes the red of the muscle palette, calories the amber that opens the
    // accent ramp. No new colours.
    val Heart = Color(0xFFFF6B6B)
    val Calories = Color(0xFFFFA23A)
}

object MuscleGroupColors {
    val ChestPush = Color(0xFFFF6B6B)
    val BackPull = Color(0xFF4D96FF)
    val Legs = Color(0xFF51CF66)
    val Shoulders = Color(0xFF9775FA)
    val Arms = Color(0xFFFFB020)
    val Core = Color(0xFF20C997)
    val Cardio = Color(0xFFF06595)
    val Neck = Color(0xFFA9746E)
    // "Other" is deliberately grey: it is not a muscle group but the absence of one.
    val Other = Color(0xFF9E9E9E)
}
