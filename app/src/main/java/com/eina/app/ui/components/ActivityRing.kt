package com.eina.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme

/**
 * Progress ring: sunken track plus a coloured arc with the accent ramp and round caps.
 *
 * A [progress] above 1 does not start a second lap: the ring stays full, since an overlapping lap
 * would make the remaining share unreadable.
 */
@Composable
fun ActivityRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 116.dp,
    strokeWidth: Dp = 13.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val island = EinaTheme.island
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "ringProgress"
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = island.sunken,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                // The ramp starts at twelve o'clock like the arc: without the rotation the gradient
                // would begin at three and its colour seam would fall inside the filled arc.
                rotate(degrees = -90f) {
                    // The ramp spreads over the drawn arc, not the whole circle: with uniform
                    // sweepGradient stops a third of a ring would show amber only, and the seam
                    // between last and first colour would surface at twelve o'clock.
                    val stops = island.accentRamp.mapIndexed { index, color ->
                        val position = index.toFloat() / (island.accentRamp.size - 1)
                        (position * animated) to color
                    } + (1f to island.accentRamp.first())
                    // The last stop returns to the first colour for the empty stretch: nothing is
                    // drawn there except the round start cap, which bleeds back past zero and
                    // would otherwise pick up the magenta at the end of the ramp.
                    drawArc(
                        brush = Brush.sweepGradient(*stops.toTypedArray()),
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
        content()
    }
}
