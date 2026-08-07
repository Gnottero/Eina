package com.eina.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO: manca asset font Inter (Regular/Medium/SemiBold/Bold) in res/font — non presente in assets/seed.
// Placeholder con FontFamily.Default finche' non viene fornito il font.
val EinaFontFamily = FontFamily.Default

val EinaTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)
