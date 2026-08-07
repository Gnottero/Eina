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
// resta leggibile anche in anteprima. Sempre chiara: un'immagine condivisa finisce su sfondi
// altrui e il bianco e' l'unico che regge ovunque.

private const val CANVAS_SIZE = 720
private const val MARGIN = 24f
private const val CARD_RADIUS = 44f
private const val PADDING = 48f
private const val CARD_SIZE = CANVAS_SIZE - MARGIN * 2

private const val CARD_BG = 0xFFFFFFFF.toInt()
private const val ACCENT = 0xFF7A5AF8.toInt()
private const val TEXT = 0xFF16151F.toInt()
private const val TEXT_SECONDARY = 0xFF7C7A93.toInt()
private const val HAIRLINE = 0xFFE7E4F3.toInt()

private fun textPaint(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        letterSpacing = spacing
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
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
 */
fun renderShareCard(context: Context, data: ShareCardData): Bitmap {
    val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardTop = MARGIN
    val cardLeft = MARGIN
    val cardRight = CANVAS_SIZE - MARGIN
    canvas.drawRoundRect(
        RectF(cardLeft, cardTop, cardRight, cardTop + CARD_SIZE),
        CARD_RADIUS,
        CARD_RADIUS,
        fill(CARD_BG)
    )

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
        textPaint(28f, TEXT, bold = true, spacing = 0.16f)
    )
    canvas.drawRightAligned(
        data.dateLabel,
        right,
        headerBaseline - 6f,
        textPaint(22f, TEXT_SECONDARY)
    )

    canvas.drawRect(left, cardTop + 132f, right, cardTop + 133f, fill(HAIRLINE))

    // Griglia 2x2: e' tutto il contenuto del widget.
    val metrics = listOf(
        Triple("DURATA", data.durationLabel, TEXT),
        Triple("VOLUME", "${formatVolume(data.volumeKg)} kg", ACCENT),
        Triple("SERIE", data.setCount.toString(), TEXT),
        Triple("PR", data.prCount.toString(), if (data.prCount > 0) ACCENT else TEXT)
    )
    val columnWidth = contentWidth / 2f
    metrics.forEachIndexed { index, (label, value, color) ->
        val columnLeft = left + columnWidth * (index % 2)
        val rowTop = cardTop + if (index < 2) 250f else 480f
        canvas.drawText(label, columnLeft, rowTop, textPaint(22f, TEXT_SECONDARY, spacing = 0.14f))
        val valuePaint = fitted(value, columnWidth - 24f, textPaint(76f, color, bold = true), minSize = 44f)
        canvas.drawText(value, columnLeft, rowTop + 92f, valuePaint)
    }

    return bitmap
}
