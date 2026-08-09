package com.eina.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val CardCornerRadius = 20.dp
val ButtonCornerRadius = 12.dp

// --- Stile "island": raggi generosi per i contenitori flottanti, pill per nav e controlli.
val IslandCornerRadius = 28.dp
val TileCornerRadius = 24.dp
val PillShape = RoundedCornerShape(percent = 50)

/**
 * Angolo continuo (squircle), non arco di cerchio: la curvatura cresce e cala in modo graduale
 * invece di attaccarsi di colpo al lato. E' la differenza che si vede fra una card Android
 * qualsiasi e una schermata iOS, e si nota soprattutto sui raggi grandi come i nostri 24-28dp.
 *
 * I nove punti di controllo per angolo sono i rapporti noti della curva continua di Apple: tre
 * bezier cubiche per angolo, nessun arco.
 */
class SquircleShape(private val radius: Dp) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // 1,528665 e' quanto l'angolo continuo "invade" ogni lato: su un elemento basso il raggio
        // nominale non ci sta e va ridotto. Ridurlo e' meglio che ripiegare sull'angolo
        // circolare a raggio pieno, che su una card bassa la trasformava in una pastiglia.
        val maxRadius = minOf(size.width, size.height) / 2f / CORNER_EXTENT
        val r = with(density) { radius.toPx() }.coerceIn(0f, maxRadius)
        if (r <= 0f) return Outline.Rectangle(size.toRect())
        val w = size.width
        val h = size.height
        val path = Path()
        corner(path, Offset(w, 0f), Offset(1f, 0f), Offset(0f, 1f), r, first = true)
        corner(path, Offset(w, h), Offset(0f, 1f), Offset(-1f, 0f), r, first = false)
        corner(path, Offset(0f, h), Offset(-1f, 0f), Offset(0f, -1f), r, first = false)
        corner(path, Offset(0f, 0f), Offset(0f, -1f), Offset(1f, 0f), r, first = false)
        path.close()
        return Outline.Generic(path)
    }

    /**
     * Un angolo, in senso orario: [inDir] e' la direzione con cui il tracciato ci arriva,
     * [outDir] quella con cui riparte. I punti si esprimono come "quanto indietro sul lato in
     * entrata" e "quanto avanti sul lato in uscita", cosi' gli stessi rapporti servono tutti e
     * quattro gli angoli senza riscriverli specchiati.
     */
    private fun corner(path: Path, c: Offset, inDir: Offset, outDir: Offset, r: Float, first: Boolean) {
        fun p(back: Float, forward: Float) = Offset(
            c.x - inDir.x * back * r + outDir.x * forward * r,
            c.y - inDir.y * back * r + outDir.y * forward * r
        )
        val start = p(CORNER_EXTENT, 0f)
        if (first) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
        cubic(path, p(1.088485f, 0f), p(0.868407f, 0f), p(0.631494f, 0.074911f))
        cubic(path, p(0.372824f, 0.221602f), p(0.221602f, 0.372824f), p(0.074911f, 0.631494f))
        cubic(path, p(0f, 0.868407f), p(0f, 1.088485f), p(0f, CORNER_EXTENT))
    }

    private fun cubic(path: Path, a: Offset, b: Offset, end: Offset) {
        path.cubicTo(a.x, a.y, b.x, b.y, end.x, end.y)
    }

    private fun Size.toRect() = androidx.compose.ui.geometry.Rect(Offset.Zero, this)

    override fun equals(other: Any?): Boolean = other is SquircleShape && other.radius == radius

    override fun hashCode(): Int = radius.hashCode()

    private companion object {
        const val CORNER_EXTENT = 1.528665f
    }
}

val IslandShape: Shape = SquircleShape(IslandCornerRadius)
val TileShape: Shape = SquircleShape(TileCornerRadius)
val CardShape: Shape = SquircleShape(CardCornerRadius)

/** Squircle piccolo per campi e controlli, dove il raggio grande sformerebbe l'elemento. */
fun squircle(radius: Dp): Shape = SquircleShape(radius)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// Material3 vuole CornerBasedShape nei suoi Shapes (deve saper interpolare i raggi), quindi qui
// restano angoli circolari: li usano solo i componenti Material che non abbiamo sostituito
// (menu, dialog di sistema). Le superfici dell'app passano da IslandShape/TileShape.
val EinaShapes = Shapes(
    extraSmall = RoundedCornerShape(ButtonCornerRadius),
    small = RoundedCornerShape(ButtonCornerRadius),
    medium = RoundedCornerShape(CardCornerRadius),
    large = RoundedCornerShape(TileCornerRadius),
    extraLarge = RoundedCornerShape(IslandCornerRadius)
)
