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
    /**
     * Whether a change of [progress] is eased into. Off for a ring that is itself a clock: the
     * rest countdown moves five times a second, and each move restarted a 700ms tween the next one
     * interrupted, so the ring trailed behind the number written inside it.
     */
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val island = EinaTheme.island
    val target = progress.coerceIn(0f, 1f)
    // The animation is not merely ignored when off: left running it would keep easing towards a
    // target that moves again before it arrives, recomposing the ring on every frame of a workout.
    val animated = if (animate) {
        val eased by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 700),
            label = "ringProgress"
        )
        eased
    } else {
        target
    }

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
                    //
                    // A stretch is always kept back for the return to the first colour, even when
                    // the arc is full: laid over the whole lap the ramp ended on magenta exactly
                    // where it started on amber, and a closed ring had a hard edge at twelve
                    // o'clock. Below that share nothing changes, so a partial ring still ends on
                    // magenta at its head.
                    val rampEnd = minOf(animated, 1f - ReturnShare)
                    val stops = island.accentRamp.mapIndexed { index, color ->
                        val position = index.toFloat() / (island.accentRamp.size - 1)
                        (position * rampEnd) to color
                    } + (1f to island.accentRamp.first())
                    // The last stop returns to the first colour: over the empty stretch of a
                    // partial ring, where nothing is drawn except the round start cap that bleeds
                    // back past zero, and over the kept-back share of a full one.
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

/**
 * Share of the lap the ramp leaves to fade back to its first colour. Small enough that a ring
 * below 82% is drawn exactly as before, wide enough that a closed one comes back round to amber
 * instead of butting magenta against it.
 */
private const val ReturnShare = 0.18f
