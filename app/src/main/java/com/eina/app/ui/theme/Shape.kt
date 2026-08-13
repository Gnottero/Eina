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

// --- Island style: generous radii for floating containers, pills for nav and controls.
val IslandCornerRadius = 28.dp
val TileCornerRadius = 24.dp
val PillShape = RoundedCornerShape(percent = 50)

/**
 * Continuous corner (squircle) rather than a circular arc: curvature grows and fades gradually
 * instead of meeting the edge abruptly. The difference shows mostly on large radii like the 24-28dp
 * used here.
 *
 * The nine control points per corner are the known ratios of the continuous curve: three cubic
 * beziers per corner, no arcs.
 */
class SquircleShape(private val radius: Dp) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // 1.528665 is how far the continuous corner reaches along each edge: on a short element the
        // nominal radius does not fit and is reduced, which looks better than falling back to a
        // full-radius circular corner that turns a short card into a pill.
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
     * One corner, clockwise: [inDir] is the direction the path arrives from, [outDir] the one it
     * leaves by. Points are expressed as "how far back along the incoming edge" and "how far
     * forward along the outgoing edge", so the same ratios serve all four corners unmirrored.
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

/** Small squircle for fields and controls, where a large radius would distort the element. */
fun squircle(radius: Dp): Shape = SquircleShape(radius)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

// Material3 requires CornerBasedShape in its Shapes (it interpolates the radii), so these stay
// circular. They only affect the Material components not replaced here (menus, system dialogs);
// the app surfaces use IslandShape/TileShape.
val EinaShapes = Shapes(
    extraSmall = RoundedCornerShape(ButtonCornerRadius),
    small = RoundedCornerShape(ButtonCornerRadius),
    medium = RoundedCornerShape(CardCornerRadius),
    large = RoundedCornerShape(TileCornerRadius),
    extraLarge = RoundedCornerShape(IslandCornerRadius)
)
