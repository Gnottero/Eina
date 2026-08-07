package com.eina.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.categoryFor

/**
 * Diagramma corpo semplificato (non anatomico): regioni stilizzate colorate in base
 * a muscoli primari (piena opacità) e secondari (opacità ridotta) dell'esercizio.
 */
@Composable
fun BodyDiagram(
    muscleGroupsPrimary: List<String>,
    muscleGroupsSecondary: List<String>,
    modifier: Modifier = Modifier
) {
    val primaryCategories = muscleGroupsPrimary.map { categoryFor(it) }.toSet()
    val secondaryCategories = muscleGroupsSecondary.map { categoryFor(it) }.toSet() - primaryCategories

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val idleColor = Color(0xFFE0E0E0)

        fun colorFor(region: MuscleGroupCategory): Color = when {
            primaryCategories.contains(region) -> region.color
            secondaryCategories.contains(region) -> region.color.copy(alpha = 0.4f)
            else -> idleColor
        }

        val w = size.width
        val h = size.height

        // Testa/collo
        drawRoundRegion(Offset(w * 0.42f, h * 0.02f), Size(w * 0.16f, h * 0.1f), colorFor(MuscleGroupCategory.OTHER))
        // Spalle
        drawRoundRegion(Offset(w * 0.22f, h * 0.13f), Size(w * 0.56f, h * 0.08f), colorFor(MuscleGroupCategory.SHOULDERS))
        // Petto/schiena (torace)
        drawRoundRegion(Offset(w * 0.3f, h * 0.22f), Size(w * 0.4f, h * 0.16f), colorFor(MuscleGroupCategory.CHEST_PUSH))
        // Braccia (due lati)
        drawRoundRegion(Offset(w * 0.08f, h * 0.22f), Size(w * 0.14f, h * 0.28f), colorFor(MuscleGroupCategory.ARMS))
        drawRoundRegion(Offset(w * 0.78f, h * 0.22f), Size(w * 0.14f, h * 0.28f), colorFor(MuscleGroupCategory.ARMS))
        // Core
        drawRoundRegion(Offset(w * 0.32f, h * 0.4f), Size(w * 0.36f, h * 0.16f), colorFor(MuscleGroupCategory.CORE))
        // Schiena/lati (rappresentata come fascia dietro il core)
        drawRoundRegion(Offset(w * 0.24f, h * 0.4f), Size(w * 0.08f, h * 0.16f), colorFor(MuscleGroupCategory.BACK_PULL))
        drawRoundRegion(Offset(w * 0.68f, h * 0.4f), Size(w * 0.08f, h * 0.16f), colorFor(MuscleGroupCategory.BACK_PULL))
        // Gambe
        drawRoundRegion(Offset(w * 0.28f, h * 0.58f), Size(w * 0.18f, h * 0.4f), colorFor(MuscleGroupCategory.LEGS))
        drawRoundRegion(Offset(w * 0.54f, h * 0.58f), Size(w * 0.18f, h * 0.4f), colorFor(MuscleGroupCategory.LEGS))
    }
}

private fun DrawScope.drawRoundRegion(offset: Offset, size: Size, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = offset,
        size = size,
        cornerRadius = CornerRadius(size.minDimension * 0.25f, size.minDimension * 0.25f)
    )
}
