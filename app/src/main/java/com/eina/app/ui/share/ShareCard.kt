package com.eina.app.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
private const val CARD_RADIUS = 44f
private const val PADDING = 48f
private const val CARD_SIZE = CANVAS_SIZE - MARGIN * 2

private const val CARD_BG = 0xFFFFFFFF.toInt()
private const val ACCENT = 0xFFF97348.toInt()
private const val TEXT = 0xFF1C1B19.toInt()
private const val TEXT_SECONDARY = 0xFF7C7A75.toInt()
private const val HAIRLINE = 0xFFE7E5E1.toInt()

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
private const val ACCENT_ON_PHOTO = 0xFFFFB07A.toInt()
private const val HAIRLINE_ON_PHOTO = 0x66FFFFFF
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
    val accentColor = if (onPhoto) ACCENT_ON_PHOTO else ACCENT
    val hairlineColor = if (onPhoto) HAIRLINE_ON_PHOTO else HAIRLINE

    val cardTop = MARGIN
    val cardLeft = MARGIN
    val cardRight = CANVAS_SIZE - MARGIN
    // In trasparenza non si disegna nessun fondo: sotto il testo il bitmap resta vuoto.
    if (!onPhoto) {
        canvas.drawRoundRect(
            RectF(cardLeft, cardTop, cardRight, cardTop + CARD_SIZE),
            CARD_RADIUS,
            CARD_RADIUS,
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

    canvas.drawRect(left, cardTop + 132f, right, cardTop + 133f, fill(hairlineColor))

    // Griglia 2x2: e' tutto il contenuto del widget.
    val metrics = listOf(
        Triple(context.getString(R.string.share_card_duration), data.durationLabel, textColor),
        Triple(context.getString(R.string.share_card_volume), "${formatVolume(data.volumeKg)} kg", accentColor),
        Triple(context.getString(R.string.share_card_sets), data.setCount.toString(), textColor),
        Triple(
            context.getString(R.string.share_card_pr),
            data.prCount.toString(),
            if (data.prCount > 0) accentColor else textColor
        )
    )
    val columnWidth = contentWidth / 2f
    metrics.forEachIndexed { index, (label, value, color) ->
        val columnLeft = left + columnWidth * (index % 2)
        val rowTop = cardTop + if (index < 2) 250f else 480f
        canvas.drawText(
            label,
            columnLeft,
            rowTop,
            textPaint(22f, secondaryColor, spacing = 0.14f, shadow = onPhoto)
        )
        val valuePaint = fitted(
            value,
            columnWidth - 24f,
            textPaint(76f, color, bold = true, shadow = onPhoto),
            minSize = 44f
        )
        canvas.drawText(value, columnLeft, rowTop + 92f, valuePaint)
    }

    return bitmap
}
