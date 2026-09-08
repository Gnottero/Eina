package com.eina.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Label stacked over a number, with the label centred on the number alone.
 *
 * A plain centred column would centre the label on "1.240 kg", unit included, which pushes the word
 * off the axis of the figure it names by half the width of the unit. Here the number is centred in
 * the column, the unit hangs off its right, and the label sits on the number's own centre.
 */
@Composable
fun MetricColumn(
    modifier: Modifier = Modifier,
    gap: Dp = 4.dp,
    /** Lifts the unit off the bottom of the number so the two sit on the same optical line. */
    unitBottomPadding: Dp = 3.dp,
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
    unit: (@Composable () -> Unit)? = null
) {
    Layout(
        modifier = modifier,
        content = {
            label()
            value()
            unit?.invoke()
        }
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelPlaceable = measurables[0].measure(loose)
        val valuePlaceable = measurables[1].measure(loose)
        val unitPlaceable = measurables.getOrNull(2)?.measure(loose)
        val unitWidth = unitPlaceable?.width ?: 0

        val contentWidth = max(labelPlaceable.width, valuePlaceable.width + unitWidth)
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        }

        val gapPx = gap.roundToPx()
        val height = labelPlaceable.height + gapPx + valuePlaceable.height

        layout(width, height) {
            // The number takes the centre, but never so far right that the unit falls off the edge.
            val valueX = ((width - valuePlaceable.width) / 2)
                .coerceAtMost(width - valuePlaceable.width - unitWidth)
                .coerceAtLeast(0)
            val labelX = (valueX + valuePlaceable.width / 2 - labelPlaceable.width / 2)
                .coerceIn(0, max(0, width - labelPlaceable.width))

            labelPlaceable.place(labelX, 0)
            val valueY = labelPlaceable.height + gapPx
            valuePlaceable.place(valueX, valueY)
            unitPlaceable?.place(
                valueX + valuePlaceable.width,
                valueY + valuePlaceable.height - unitPlaceable.height - unitBottomPadding.roundToPx()
            )
        }
    }
}
