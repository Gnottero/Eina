package com.eina.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 20.dp
val ButtonCornerRadius = 12.dp

// --- Stile "island": raggi piu' generosi per i contenitori flottanti, pill per nav e controlli.
val IslandCornerRadius = 28.dp
val TileCornerRadius = 24.dp
val PillShape = RoundedCornerShape(percent = 50)
val IslandShape = RoundedCornerShape(IslandCornerRadius)
val TileShape = RoundedCornerShape(TileCornerRadius)
val CardShape = RoundedCornerShape(CardCornerRadius)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

val EinaShapes = Shapes(
    extraSmall = RoundedCornerShape(ButtonCornerRadius),
    small = RoundedCornerShape(ButtonCornerRadius),
    medium = CardShape,
    large = TileShape,
    extraLarge = IslandShape
)
