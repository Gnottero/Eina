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
 * Anello di avanzamento: traccia incassata piu' arco colorato con la rampa dell'accento e
 * terminali tondi. E' il pezzo che regge la Dashboard — un numero solo, letto da lontano,
 * con la quota di completamento visibile senza leggere niente.
 *
 * [progress] oltre 1 non fa girare l'anello una seconda volta: resta pieno, perche' un secondo
 * giro sovrapposto renderebbe illeggibile quanto manca.
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
                // La rampa parte da mezzogiorno come l'arco: senza la rotazione il gradiente
                // comincerebbe alle 3 e il salto di colore cadrebbe in mezzo all'arco pieno.
                rotate(degrees = -90f) {
                    // La rampa va distribuita sull'arco disegnato, non sul giro intero: con un
                    // sweepGradient a stop uniformi, a un terzo di anello si vedrebbe solo
                    // l'ambra, e la cucitura fra ultimo e primo colore spunterebbe a mezzogiorno
                    // come una macchia magenta sotto il terminale.
                    val stops = island.accentRamp.mapIndexed { index, color ->
                        val position = index.toFloat() / (island.accentRamp.size - 1)
                        (position * animated) to color
                    } + (1f to island.accentRamp.first())
                    // L'ultimo stop torna al primo colore per il tratto vuoto: li' non si disegna
                    // niente tranne il terminale tondo dell'inizio, che sborda all'indietro oltre
                    // lo zero. Senza, quel terminale pescava il magenta di fine rampa e a
                    // mezzogiorno spuntava una macchia rosa.
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
