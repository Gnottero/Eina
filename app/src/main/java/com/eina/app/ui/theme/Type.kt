package com.eina.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.eina.app.R

/**
 * Inter (SIL Open Font License 1.1), bundled in res/font. The former FontFamily.Default (Roboto)
 * was readable but characterless, and its proportional digits made the stopwatch and set table
 * numbers jitter.
 */
val EinaFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/**
 * Inter's "Display" cut: same family, tighter drawing meant for large sizes (closer letter spacing,
 * shorter terminals). Reserved for titles and headline numbers — below 20sp it hurts readability.
 */
val EinaDisplayFontFamily = FontFamily(
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold, FontWeight.Bold)
)

/**
 * Tabular figures, needed wherever a number changes constantly (stopwatch, rest timer, workout
 * duration): with proportional digits the line widens and narrows every second.
 */
const val TabularFigures = "tnum"

val EinaTypography = Typography(
    // Headline metric numbers: large size, negative tracking, Display cut.
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
    // Screen title.
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
    // Tiny uppercase labels: here the tracking must be widened, not tightened.
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
