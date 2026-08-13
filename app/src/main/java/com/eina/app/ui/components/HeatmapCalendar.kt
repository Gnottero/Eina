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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * GitHub-style heatmap: one column per week, one row per day (Monday to Sunday). Intensity is the
 * day value against the period maximum, in four steps. Pure Canvas, no extra dependency.
 */
@Composable
fun HeatmapCalendar(
    valuesByDay: Map<LocalDate, Double>,
    modifier: Modifier = Modifier,
    weeks: Int = 18,
    today: LocalDate = LocalDate.now(),
    color: Color = MaterialTheme.colorScheme.primary
) {
    val island = EinaTheme.island
    val trackColor = island.sunken
    // Starts on the Monday of the oldest week shown, so the columns stay aligned.
    val lastMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())
    val maxValue = valuesByDay
        .filterKeys { !it.isBefore(firstMonday) && !it.isAfter(today) }
        .values.maxOrNull()?.takeIf { it > 0.0 } ?: 0.0

    val cellSpacing = 3.dp
    val cellSize = 14.dp

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellSize * 7 + cellSpacing * 6)
        ) {
            val spacingPx = cellSpacing.toPx()
            val cell = ((size.width - spacingPx * (weeks - 1)) / weeks)
                .coerceAtMost((size.height - spacingPx * 6) / 7f)
            val radius = CornerRadius(cell * 0.3f, cell * 0.3f)

            repeat(weeks) { weekIndex ->
                repeat(7) { dayIndex ->
                    val date = firstMonday.plusWeeks(weekIndex.toLong()).plusDays(dayIndex.toLong())
                    if (date.isAfter(today)) return@repeat
                    val value = valuesByDay[date] ?: 0.0
                    val level = when {
                        value <= 0.0 || maxValue <= 0.0 -> 0
                        value >= maxValue * 0.75 -> 4
                        value >= maxValue * 0.5 -> 3
                        value >= maxValue * 0.25 -> 2
                        else -> 1
                    }
                    val cellColor = when (level) {
                        0 -> trackColor
                        1 -> color.copy(alpha = 0.25f)
                        2 -> color.copy(alpha = 0.45f)
                        3 -> color.copy(alpha = 0.7f)
                        // The heaviest day is not a more saturated orange but the end of the ramp:
                        // the scale rises in intensity and then shifts hue, so peaks stand out even
                        // inside a fully trained week.
                        else -> island.accentRamp.last()
                    }
                    drawRoundRect(
                        color = cellColor,
                        topLeft = Offset(
                            x = weekIndex * (cell + spacingPx),
                            y = dayIndex * (cell + spacingPx)
                        ),
                        size = Size(cell, cell),
                        cornerRadius = radius
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = monthLabel(firstMonday),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary
            )
            Text(
                text = monthLabel(today),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary
            )
        }
    }
}

private fun monthLabel(date: LocalDate): String =
    date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() }
