package com.eina.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/**
 * Grafico a barre arrotondate in stile reference: barre piene per i valori, traccia incassata
 * per i giorni vuoti. Canvas puro — nessuna dipendenza extra e nessun costo di avvio.
 */
@Composable
fun MiniBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 120.dp,
    highlightIndex: Int? = null
) {
    val island = EinaTheme.island
    val trackColor = island.sunken
    // Le barre prendono la rampa dell'accento; un colore passato a mano (per esempio quello di
    // un gruppo muscolare) resta pieno.
    val ramp = if (barColor == MaterialTheme.colorScheme.primary) {
        island.accentRamp
    } else {
        listOf(barColor, barColor)
    }
    val maxValue = values.maxOrNull()?.takeIf { it > 0f } ?: 1f

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.5f).coerceAtMost(20.dp.toPx())
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            values.forEachIndexed { index, value ->
                val centerX = slot * index + slot / 2f
                val left = centerX - barWidth / 2f

                // Traccia di fondo: da' ritmo anche quando il valore e' zero.
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius
                )

                val barHeight = (value / maxValue) * size.height
                if (barHeight > 0f) {
                    // La rampa e' verticale e calcolata sull'altezza piena del grafico, non
                    // sulla singola barra: cosi' due barre di altezza diversa hanno lo stesso
                    // colore alla stessa quota e il gruppo si legge come un blocco unico.
                    val brush = Brush.verticalGradient(
                        colors = ramp,
                        startY = 0f,
                        endY = size.height
                    )
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                        alpha = if (highlightIndex == null || highlightIndex == index) 1f else 0.55f
                    )
                }
            }
        }

        if (labels != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.xl),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (highlightIndex == index) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            island.textSecondary
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
