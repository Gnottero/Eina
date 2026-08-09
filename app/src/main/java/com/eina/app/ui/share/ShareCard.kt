package com.eina.app.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.eina.app.R
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.components.formatDayMonth
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume

/** Dati minimi per disegnare la card: nessuna dipendenza da Compose o dal DB. */
data class ShareCardData(
    val dateLabel: String,
    val durationLabel: String,
    val volumeKg: Double,
    val setCount: Int,
    val prCount: Int
)

fun shareCardDataOf(summary: SessionSummary): ShareCardData = ShareCardData(
    dateLabel = "${formatDayMonth(summary.startTime)} · ${formatTime(summary.startTime)}",
    // Senza endTime la durata non e' ricostruibile: si stampa un trattino invece di un finto zero.
    durationLabel = summary.durationMinutes?.let { formatDuration(it) } ?: "—",
    volumeKg = summary.volumeKg,
    setCount = summary.setCount,
    prCount = summary.prCount
)

// --- Disegno ---------------------------------------------------------------
// DECISIONE: la card e' disegnata con android.graphics invece di catturare una view Compose:
// dimensione fissa e indipendente dallo schermo, dal tema attivo e dal ciclo di vita della UI.
// Formato quadrato piccolo (720px): si appoggia sopra una foto in una storia senza coprirla e
// resta leggibile anche in anteprima.

private const val CANVAS_SIZE = 720
private const val MARGIN = 24f
private const val CARD_RADIUS = 56f
private const val PADDING = 48f
private const val CARD_SIZE = CANVAS_SIZE - MARGIN * 2

private const val TILE_RADIUS = 28f
private const val TILE_GAP = 20f

private const val CARD_BG = 0xFFFFFFFF.toInt()
private const val TILE_BG = 0xFFEFEEEB.toInt()
private const val ACCENT = 0xFFF97348.toInt()
// Rampa dell'accento, la stessa del tema (ambra → arancio → magenta): la portano i numeri che
// contano e il filo sotto l'intestazione, cosi' l'immagine che gira sui social ha lo stesso
// segno della app invece di un arancio piatto.
private val ACCENT_RAMP = intArrayOf(0xFFFFA23A.toInt(), 0xFFF97348.toInt(), 0xFFF9436B.toInt())
private const val TEXT = 0xFF1C1B19.toInt()
private const val TEXT_SECONDARY = 0xFF7C7A75.toInt()

// Inter, lo stesso font dell'app. L'immagine condivisa e' il pezzo che gira fuori dall'app:
// disegnarla col sans di sistema (Roboto su un telefono, altro su un altro) la faceva sembrare
// di un'altra applicazione. Caricato una volta sola: getFont apre il file ogni volta.
private var interBold: Typeface? = null
private var interRegular: Typeface? = null

private fun loadTypefaces(context: Context) {
    if (interBold == null) interBold = ResourcesCompat.getFont(context, R.font.inter_display_bold)
    if (interRegular == null) interRegular = ResourcesCompat.getFont(context, R.font.inter_medium)
}

// Versione trasparente: il fondo e' la foto di chi condivide, quindi il testo va in bianco e
// con un'ombra portata, l'unico modo di restare leggibile sia su cielo che su asfalto.
private const val TEXT_ON_PHOTO = 0xFFFFFFFF.toInt()
private const val TEXT_ON_PHOTO_SECONDARY = 0xCCFFFFFF.toInt()
private const val TILE_BG_ON_PHOTO = 0x2EFFFFFF
private const val PHOTO_SHADOW = 0x99000000.toInt()

private fun textPaint(
    size: Float,
    color: Int,
    bold: Boolean = false,
    spacing: Float = 0f,
    shadow: Boolean = false
) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    textSize = size
    letterSpacing = spacing
    typeface = (if (bold) interBold else interRegular)
        ?: Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    if (shadow) setShadowLayer(size / 6f, 0f, size / 20f, PHOTO_SHADOW)
}

private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

/**
 * Angolo continuo, gli stessi rapporti di [com.eina.app.ui.theme.SquircleShape]: la card e le
 * tessere dell'immagine condivisa devono avere la curva delle schermate, non quella di
 * `drawRoundRect`. Non si puo' riusare la classe Compose: qui si disegna con android.graphics.
 */
private fun squirclePath(rect: RectF, radius: Float): Path {
    val r = radius.coerceAtMost(minOf(rect.width(), rect.height()) / 2f / CORNER_EXTENT)
    val path = Path()
    corner(path, rect.right, rect.top, 1f, 0f, 0f, 1f, r, first = true)
    corner(path, rect.right, rect.bottom, 0f, 1f, -1f, 0f, r, first = false)
    corner(path, rect.left, rect.bottom, -1f, 0f, 0f, -1f, r, first = false)
    corner(path, rect.left, rect.top, 0f, -1f, 1f, 0f, r, first = false)
    path.close()
    return path
}

private const val CORNER_EXTENT = 1.528665f

@Suppress("LongParameterList")
private fun corner(
    path: Path,
    cx: Float,
    cy: Float,
    inX: Float,
    inY: Float,
    outX: Float,
    outY: Float,
    r: Float,
    first: Boolean
) {
    fun px(back: Float, forward: Float) = cx - inX * back * r + outX * forward * r
    fun py(back: Float, forward: Float) = cy - inY * back * r + outY * forward * r
    if (first) path.moveTo(px(CORNER_EXTENT, 0f), py(CORNER_EXTENT, 0f))
    else path.lineTo(px(CORNER_EXTENT, 0f), py(CORNER_EXTENT, 0f))
    path.cubicTo(
        px(1.088485f, 0f), py(1.088485f, 0f),
        px(0.868407f, 0f), py(0.868407f, 0f),
        px(0.631494f, 0.074911f), py(0.631494f, 0.074911f)
    )
    path.cubicTo(
        px(0.372824f, 0.221602f), py(0.372824f, 0.221602f),
        px(0.221602f, 0.372824f), py(0.221602f, 0.372824f),
        px(0.074911f, 0.631494f), py(0.074911f, 0.631494f)
    )
    path.cubicTo(
        px(0f, 0.868407f), py(0f, 0.868407f),
        px(0f, 1.088485f), py(0f, 1.088485f),
        px(0f, CORNER_EXTENT), py(0f, CORNER_EXTENT)
    )
}

/** Sfumatura della rampa sul tratto orizzontale indicato. */
private fun rampShader(left: Float, right: Float, colors: IntArray) =
    LinearGradient(left, 0f, right, 0f, colors, null, Shader.TileMode.CLAMP)

private fun Canvas.drawRightAligned(text: String, right: Float, y: Float, paint: TextPaint) {
    drawText(text, right - paint.measureText(text), y, paint)
}

/** Riduce il corpo finche' il testo non entra nella larghezza disponibile. */
private fun fitted(text: String, maxWidth: Float, paint: TextPaint, minSize: Float): TextPaint {
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) {
        paint.textSize -= 2f
    }
    return paint
}

/**
 * Marchio dell'app, disegnato dal vettoriale condiviso con l'icona di sistema
 * (res/drawable/ic_eina_logo.xml): un solo file da toccare se il logo cambia.
 */
private fun Canvas.drawLogoMark(context: Context, left: Float, top: Float, size: Float) {
    val logo = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_eina_logo, context.theme) ?: return
    logo.setBounds(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
    logo.draw(this)
}

/**
 * Widget quadrato da storia: marchio, data e le quattro metriche essenziali in griglia 2x2
 * (durata, volume, serie, PR). Niente elenco esercizi: deve restare piccolo e leggibile.
 *
 * Un'immagine sola, con un interruttore: `transparent` toglie la tessera bianca e lascia il
 * solo testo su fondo vuoto, da appoggiare sopra una propria foto nella storia (il PNG
 * conserva il canale alfa). Il testo passa al bianco con ombra portata, l'unico modo di
 * restare leggibile sia su cielo che su asfalto.
 */
fun renderShareCard(
    context: Context,
    data: ShareCardData,
    transparent: Boolean = false
): Bitmap {
    loadTypefaces(context)
    val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val onPhoto = transparent

    val textColor = if (onPhoto) TEXT_ON_PHOTO else TEXT
    val secondaryColor = if (onPhoto) TEXT_ON_PHOTO_SECONDARY else TEXT_SECONDARY
    // Anche sulla foto l'accento e' l'arancio dell'app, pieno e senza sfumatura: i toni
    // schiariti piu' la rampa facevano sembrare i numeri al neon.
    val accentColor = ACCENT
    val ramp = ACCENT_RAMP
    val tileColor = if (onPhoto) TILE_BG_ON_PHOTO else TILE_BG

    val cardTop = MARGIN
    val cardLeft = MARGIN
    val cardRight = CANVAS_SIZE - MARGIN
    // In trasparenza non si disegna nessun fondo: sotto il testo il bitmap resta vuoto.
    if (!onPhoto) {
        canvas.drawPath(
            squirclePath(RectF(cardLeft, cardTop, cardRight, cardTop + CARD_SIZE), CARD_RADIUS),
            fill(CARD_BG)
        )
    }

    val left = cardLeft + PADDING
    val right = cardRight - PADDING
    val contentWidth = right - left

    // Intestazione: marchio + nome a sinistra, data a destra.
    val logoSize = 44f
    val headerBaseline = cardTop + 96f
    canvas.drawLogoMark(context, left, headerBaseline - logoSize + 6f, logoSize)
    canvas.drawText(
        "EINA",
        left + logoSize + 16f,
        headerBaseline - 4f,
        textPaint(28f, textColor, bold = true, spacing = 0.16f, shadow = onPhoto)
    )
    canvas.drawRightAligned(
        data.dateLabel,
        right,
        headerBaseline - 6f,
        textPaint(22f, secondaryColor, shadow = onPhoto)
    )

    // Filo della rampa al posto del capello grigio: e' la firma del tema.
    canvas.drawPath(
        squirclePath(RectF(left, cardTop + 128f, right, cardTop + 134f), 3f),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = rampShader(left, right, ramp) }
    )

    // Griglia 2x2 di tessere: e' tutto il contenuto del widget.
    val metrics = listOf(
        Triple(context.getString(R.string.share_card_duration), data.durationLabel, false),
        Triple(context.getString(R.string.share_card_volume), "${formatVolume(data.volumeKg)} kg", true),
        Triple(context.getString(R.string.share_card_sets), data.setCount.toString(), false),
        Triple(context.getString(R.string.share_card_pr), data.prCount.toString(), data.prCount > 0)
    )
    val tileWidth = (contentWidth - TILE_GAP) / 2f
    val tileHeight = 200f
    metrics.forEachIndexed { index, (label, value, ramped) ->
        val tileLeft = left + (tileWidth + TILE_GAP) * (index % 2)
        val tileTop = cardTop + 176f + (tileHeight + TILE_GAP) * (index / 2)
        canvas.drawPath(
            squirclePath(RectF(tileLeft, tileTop, tileLeft + tileWidth, tileTop + tileHeight), TILE_RADIUS),
            fill(tileColor)
        )
        canvas.drawText(
            label,
            tileLeft + 32f,
            tileTop + 58f,
            textPaint(22f, secondaryColor, spacing = 0.14f, shadow = onPhoto)
        )
        val valuePaint = fitted(
            value,
            tileWidth - 64f,
            textPaint(72f, if (ramped) accentColor else textColor, bold = true, shadow = onPhoto),
            minSize = 40f
        )
        // Sulla tessera bianca il numero in evidenza porta la rampa: si applica al tratto del
        // testo, quindi va misurata sul testo e non sulla tessera. Sulla foto no, resta
        // arancio pieno.
        if (ramped && !onPhoto) {
            valuePaint.shader = rampShader(
                tileLeft + 32f,
                tileLeft + 32f + valuePaint.measureText(value),
                ramp
            )
        }
        canvas.drawText(value, tileLeft + 32f, tileTop + 152f, valuePaint)
    }

    return bitmap
}
