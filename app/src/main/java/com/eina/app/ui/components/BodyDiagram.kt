package com.eina.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.eina.app.R
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.categoryFor

/**
 * Schema anatomico: due sagome (fronte e retro) divise nelle regioni muscolari del dataset.
 * I muscoli primari dell'esercizio sono a tinta piena, i secondari alla stessa tinta smorzata,
 * il resto del corpo resta della superficie incassata del tema.
 *
 * Le regioni sono poligoni a pochi vertici arrotondati (vedi [muscleShape]): bastano a farsi
 * riconoscere e non costano un file vettoriale per ogni muscolo. Sistema di coordinate fisso
 * 100x220 per figura, scalato al vero al momento del disegno.
 */
@Composable
fun BodyDiagram(
    muscleGroupsPrimary: List<String>,
    muscleGroupsSecondary: List<String>,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    val primary = muscleGroupsPrimary.map { it.lowercase() }.toSet()
    val secondary = muscleGroupsSecondary.map { it.lowercase() }.toSet() - primary

    val colorFor: (String?) -> Color = { muscle ->
        when {
            muscle == null -> island.sunken
            muscle in primary -> categoryFor(muscle).color
            muscle in secondary -> categoryFor(muscle).color.copy(alpha = 0.35f)
            else -> island.sunken
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        BodyView(
            regions = FrontRegions,
            label = stringResource(R.string.body_diagram_front),
            colorFor = colorFor,
            modifier = Modifier.weight(1f)
        )
        BodyView(
            regions = BackRegions,
            label = stringResource(R.string.body_diagram_back),
            colorFor = colorFor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BodyView(
    regions: List<BodyRegion>,
    label: String,
    colorFor: (String?) -> Color,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIEWPORT_WIDTH / VIEWPORT_HEIGHT)
        ) {
            val scaleX = size.width / VIEWPORT_WIDTH
            val scaleY = size.height / VIEWPORT_HEIGHT
            regions.forEach { region ->
                val path = muscleShape(region.points, scaleX, scaleY)
                drawPath(path, colorFor(region.muscle))
                // Un filo di contorno separa due regioni adiacenti dello stesso colore
                // (quadricipiti e adduttori, per esempio) che altrimenti si fonderebbero.
                drawPath(path, island.outlineSubtle, style = Stroke(width = 1f))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = island.textSecondary
        )
    }
}

private const val VIEWPORT_WIDTH = 100f
private const val VIEWPORT_HEIGHT = 220f

/** Una regione della figura: `muscle` e' la chiave del dataset, null per testa, mani e piedi. */
private data class BodyRegion(val muscle: String?, val points: List<Offset>)

private fun p(x: Float, y: Float) = Offset(x, y)

/** Riflette il poligono attorno all'asse verticale: le due meta' del corpo sono simmetriche. */
private fun List<Offset>.mirrored(): List<Offset> = map { Offset(VIEWPORT_WIDTH - it.x, it.y) }

/** Regione singola (sull'asse del corpo): compare una volta sola. */
private fun MutableList<BodyRegion>.single(muscle: String?, points: List<Offset>) {
    add(BodyRegion(muscle, points))
}

/** Regione pari: disegnata a sinistra e specchiata a destra. */
private fun MutableList<BodyRegion>.pair(muscle: String?, points: List<Offset>) {
    add(BodyRegion(muscle, points))
    add(BodyRegion(muscle, points.mirrored()))
}

/**
 * Poligono con gli spigoli smussati: si parte dal punto medio di ogni lato e si passa per i
 * vertici come punti di controllo. Poche coordinate, forme comunque morbide.
 */
private fun muscleShape(points: List<Offset>, scaleX: Float, scaleY: Float): Path {
    val path = Path()
    if (points.size < 3) return path
    val scaled = points.map { Offset(it.x * scaleX, it.y * scaleY) }
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    val start = mid(scaled.last(), scaled.first())
    path.moveTo(start.x, start.y)
    scaled.forEachIndexed { index, vertex ->
        val end = mid(vertex, scaled[(index + 1) % scaled.size])
        path.quadraticTo(vertex.x, vertex.y, end.x, end.y)
    }
    path.close()
    return path
}

// Parti condivise fra le due viste: cambiano i muscoli, non la sagoma.
private val Head = listOf(p(50f, 4f), p(60f, 12f), p(59f, 24f), p(50f, 29f), p(41f, 24f), p(40f, 12f))
private val Hand = listOf(p(20f, 95f), p(28f, 94f), p(29f, 110f), p(21f, 111f))
private val Foot = listOf(p(36f, 172f), p(47f, 172f), p(48f, 192f), p(34f, 192f))
private val Forearm = listOf(p(24f, 69f), p(31f, 67f), p(29f, 97f), p(21f, 99f), p(18f, 84f))
private val Deltoid = listOf(p(33f, 33f), p(24f, 38f), p(21f, 51f), p(29f, 55f), p(34f, 45f))
private val UpperArm = listOf(p(26f, 50f), p(33f, 48f), p(32f, 72f), p(24f, 74f), p(21f, 62f))

private val FrontRegions: List<BodyRegion> = buildList {
    single(null, Head)
    single("neck", listOf(p(43f, 26f), p(57f, 26f), p(58f, 34f), p(42f, 34f)))
    pair("traps", listOf(p(30f, 33f), p(43f, 28f), p(44f, 36f), p(33f, 39f)))
    pair("shoulders", Deltoid)
    pair("chest", listOf(p(33f, 36f), p(49f, 39f), p(49f, 60f), p(35f, 57f), p(30f, 45f)))
    pair("biceps", UpperArm)
    pair("forearms", Forearm)
    pair(null, Hand)
    single("abdominals", listOf(p(41f, 61f), p(59f, 61f), p(58f, 93f), p(50f, 98f), p(42f, 93f)))
    // Obliqui: stessa chiave "abdominals", il dataset non li distingue dal retto addominale.
    pair("abdominals", listOf(p(34f, 59f), p(40f, 62f), p(41f, 92f), p(35f, 85f), p(32f, 71f)))
    pair("abductors", listOf(p(31f, 90f), p(41f, 93f), p(40f, 108f), p(29f, 104f)))
    pair("quadriceps", listOf(p(33f, 104f), p(47f, 103f), p(46f, 145f), p(37f, 148f), p(31f, 124f)))
    pair("adductors", listOf(p(42f, 103f), p(49f, 103f), p(49f, 134f), p(43f, 136f)))
    pair("calves", listOf(p(36f, 142f), p(46f, 141f), p(45f, 176f), p(37f, 178f), p(34f, 160f)))
    pair(null, Foot)
}

private val BackRegions: List<BodyRegion> = buildList {
    single(null, Head)
    single("neck", listOf(p(43f, 26f), p(57f, 26f), p(58f, 32f), p(42f, 32f)))
    single("traps", listOf(p(38f, 31f), p(62f, 31f), p(64f, 44f), p(50f, 50f), p(36f, 44f)))
    pair("shoulders", Deltoid)
    pair("lats", listOf(p(32f, 44f), p(42f, 50f), p(42f, 78f), p(35f, 74f), p(29f, 60f)))
    single("middle back", listOf(p(43f, 48f), p(57f, 48f), p(58f, 70f), p(42f, 70f)))
    single("lower back", listOf(p(41f, 70f), p(59f, 70f), p(58f, 92f), p(42f, 92f)))
    pair("triceps", UpperArm)
    pair("forearms", Forearm)
    pair(null, Hand)
    pair("glutes", listOf(p(33f, 90f), p(49f, 90f), p(49f, 110f), p(34f, 108f)))
    pair("hamstrings", listOf(p(33f, 106f), p(48f, 105f), p(47f, 145f), p(37f, 148f), p(31f, 126f)))
    pair("calves", listOf(p(35f, 142f), p(46f, 141f), p(45f, 174f), p(36f, 177f), p(33f, 159f)))
    pair(null, Foot)
}
