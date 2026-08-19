package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

// Final palette; the app is light-only.
// No neutral grey is left anywhere: the non-white surfaces are warm off-whites (cream page, ivory
// sunken), so a page is made of paper and white islands and nothing on it reads as Android chrome.
// They are warm but not tinted orange: the accent stays the only actual colour.
val LightBackground = Color(0xFFF7F2ED)
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
 * Sunken surface: tracks, inactive chips, fields, sheet rows. A warm ivory instead of a grey: the
 * neutral version was the only cold thing on a warm page and read as a panel laid on the card. It
 * stays lighter than the page background, so the controls sitting there (see IslandSecondaryButton)
 * are legible without the outline they used to need.
 */
val LightSurfaceSunken = Color(0xFFFBF8F4)

/**
 * Even lighter sunken surface, for large areas *inside* a white card: the summary set table and the
 * weight and reps fields. With the full tint those boxes would weigh more than the numbers they
 * hold.
 */
val LightSurfaceSunkenSoft = Color(0xFFFDFBF9)
val LightTextSecondary = Color(0xFF8A7D75)
/**
 * Kept only as a *fill* for separators and disabled tracks. Nothing draws a hairline border with
 * it any more: shape, shadow and tint carry the separation instead of a line.
 */
val LightOutlineSubtle = Color(0xFFF2ECE6)
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
    // "Other" is the quietest colour of the set: it is not a muscle group but the absence of one.
    // Warm taupe rather than grey, so it belongs to the same palette as the rest.
    val Other = Color(0xFFC0A99C)
}
