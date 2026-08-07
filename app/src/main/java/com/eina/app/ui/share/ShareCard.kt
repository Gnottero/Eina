package com.eina.app.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
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
    val exerciseCount: Int,
    val prCount: Int,
    val streakDays: Int
)

fun shareCardDataOf(
    summary: SessionSummary,
    streakDays: Int = 0
): ShareCardData = ShareCardData(
    dateLabel = "${formatDayMonth(summary.startTime)} · ${formatTime(summary.startTime)}",
    // Senza endTime la durata non e' ricostruibile: si stampa un trattino invece di un finto zero.
    durationLabel = summary.durationMinutes?.let { formatDuration(it) } ?: "—",
    volumeKg = summary.volumeKg,
    setCount = summary.setCount,
    exerciseCount = summary.exerciseNames.size,
    prCount = summary.prCount,
    streakDays = streakDays
)

// --- Disegno ---------------------------------------------------------------
// DECISIONE: la card e' disegnata con android.graphics invece di catturare una view Compose:
// dimensione fissa e indipendente dallo schermo, dal tema attivo e dal ciclo di vita della UI.
// Formato "widget da storia" (stile Strava): un rettangolo basso e largo con margine trasparente
// intorno, cosi' si appoggia sopra una foto in Instagram senza coprirla. Sempre chiaro: un'immagine
// condivisa finisce su sfondi altrui e il bianco e' l'unico che regge ovunque.

private const val CARD_WIDTH = 1080
private const val MARGIN = 32f
private const val CARD_RADIUS = 56f
private const val PADDING = 64f
private const val CARD_HEIGHT = 476f

private const val CARD_BG = 0xFFFFFFFF.toInt()
private const val ACCENT = 0xFF7A5AF8.toInt()
private const val ACCENT_SOFT = 0xFFEDE8FF.toInt()
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

/** Pillola con testo centrato verticalmente sulla sua altezza. */
private fun Canvas.drawPill(
    label: String,
    left: Float,
    centerY: Float,
    background: Int,
    paint: TextPaint
) {
    val height = 60f
    val pill = RectF(left, centerY - height / 2f, left + paint.measureText(label) + 56f, centerY + height / 2f)
    drawRoundRect(pill, height / 2f, height / 2f, fill(background))
    drawText(label, pill.left + 28f, pill.centerY() + paint.textSize / 3f, paint)
}

/**
 * Widget da storia: card bianca larga 1016 su sfondo trasparente, con marchio, data e le tre
 * metriche che raccontano l'allenamento (durata, volume, serie). In coda gli esercizi svolti e,
 * se ci sono, PR e striscia di giorni. Niente elenco esercizi: deve restare piccola e leggibile
 * sopra una foto.
 */
fun renderShareCard(context: Context, data: ShareCardData): Bitmap {
    val bitmapHeight = (CARD_HEIGHT + MARGIN * 2).toInt()
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardTop = MARGIN
    val cardLeft = MARGIN
    val cardRight = CARD_WIDTH - MARGIN
    canvas.drawRoundRect(
        RectF(cardLeft, cardTop, cardRight, cardTop + CARD_HEIGHT),
        CARD_RADIUS,
        CARD_RADIUS,
        fill(CARD_BG)
    )

    val left = cardLeft + PADDING
    val right = cardRight - PADDING
    val contentWidth = right - left

    // Intestazione: marchio + wordmark a sinistra, data a destra.
    val logoSize = 56f
    val headerBaseline = cardTop + 108f
    canvas.drawLogoMark(context, left, headerBaseline - logoSize + 8f, logoSize)
    canvas.drawText(
        "EINA",
        left + logoSize + 20f,
        headerBaseline - 4f,
        textPaint(32f, TEXT, bold = true, spacing = 0.16f)
    )
    canvas.drawRightAligned(
        data.dateLabel,
        right,
        headerBaseline - 6f,
        textPaint(28f, TEXT_SECONDARY)
    )

    canvas.drawRect(left, cardTop + 150f, right, cardTop + 151f, fill(HAIRLINE))

    // Le tre metriche essenziali: e' il cuore del widget, tutto il resto e' contorno.
    val metrics = listOf(
        Triple("DURATA", data.durationLabel, TEXT),
        Triple("VOLUME", "${formatVolume(data.volumeKg)} kg", ACCENT),
        Triple("SERIE", data.setCount.toString(), TEXT)
    )
    val columnWidth = contentWidth / 3f
    metrics.forEachIndexed { index, (label, value, color) ->
        val columnLeft = left + columnWidth * index
        canvas.drawText(label, columnLeft, cardTop + 232f, textPaint(24f, TEXT_SECONDARY, spacing = 0.14f))
        val valuePaint = fitted(value, columnWidth - 24f, textPaint(72f, color, bold = true), minSize = 40f)
        canvas.drawText(value, columnLeft, cardTop + 320f, valuePaint)
    }

    // Coda: esercizi a sinistra, pillola di merito a destra (PR se ci sono, altrimenti striscia).
    val footerCenterY = cardTop + 386f
    val exercises = if (data.exerciseCount == 1) "1 esercizio" else "${data.exerciseCount} esercizi"
    canvas.drawText(exercises, left, footerCenterY + 10f, textPaint(30f, TEXT_SECONDARY))

    val highlight = when {
        data.prCount > 0 ->
            if (data.prCount == 1) "1 nuovo record" else "${data.prCount} nuovi record"
        data.streakDays > 1 -> "${data.streakDays} giorni di fila"
        else -> null
    }
    if (highlight != null) {
        val paint = textPaint(28f, ACCENT, bold = true, spacing = 0.04f)
        canvas.drawPill(
            label = highlight,
            left = right - paint.measureText(highlight) - 56f,
            centerY = footerCenterY,
            background = ACCENT_SOFT,
            paint = paint
        )
    }

    return bitmap
}
