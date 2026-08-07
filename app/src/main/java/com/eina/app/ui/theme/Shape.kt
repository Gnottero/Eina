package com.eina.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 20.dp
val ButtonCornerRadius = 12.dp

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

val EinaShapes = Shapes(
    small = RoundedCornerShape(ButtonCornerRadius),
    medium = RoundedCornerShape(CardCornerRadius),
    large = RoundedCornerShape(CardCornerRadius)
)
