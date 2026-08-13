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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/**
 * Line chart with a gradient area, used where the trend matters more than the absolute value.
 * Pure Canvas, like MiniBarChart.
 */
@Composable
fun MiniLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 120.dp
) {
    val island = EinaTheme.island
    val trackColor = island.sunken

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (values.isEmpty()) return@Canvas
            val min = values.min()
            val max = values.max()
            val span = max - min
            val padding = size.height * 0.12f
            val usable = size.height - padding * 2

            fun pointAt(index: Int): Offset {
                val x = if (values.size == 1) size.width / 2f else size.width * index / (values.size - 1)
                // All values equal: the line sits at mid height. Flattened at the bottom, where
                // normalising over an invented span would put it, it would read as a zero.
                val ratio = if (span > 0f) (values[index] - min) / span else 0.5f
                val y = padding + usable * (1f - ratio)
                return Offset(x, y)
            }

            // Sunken baseline: gives a reference even with few points.
            drawLine(
                color = trackColor,
                start = Offset(0f, size.height - padding),
                end = Offset(size.width, size.height - padding),
                strokeWidth = 2.dp.toPx()
            )

            if (values.size == 1) {
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = pointAt(0))
                return@Canvas
            }

            val path = Path().apply {
                moveTo(pointAt(0).x, pointAt(0).y)
                for (index in 1 until values.size) {
                    val point = pointAt(index)
                    lineTo(point.x, point.y)
                }
            }
            val areaPath = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = areaPath,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            drawPath(path = path, color = lineColor, style = Stroke(width = 3.dp.toPx()))
            drawCircle(color = lineColor, radius = 5.dp.toPx(), center = pointAt(values.lastIndex))
        }

        if (labels != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.xl),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = island.textSecondary
                    )
                }
            }
        }
    }
}
