package com.eina.app.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.eina.app.data.db.WeightType
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume

/** Una riga esercizio nell'immagine di riepilogo. */
data class ShareCardExercise(
    val name: String,
    val setCount: Int,
    val bestSetLabel: String?,
    val hasPr: Boolean
)

/** Dati minimi per disegnare la card: nessuna dipendenza da Compose o dal DB. */
data class ShareCardData(
    val title: String,
    val subtitle: String,
    val durationLabel: String,
    val volumeKg: Double,
    val setCount: Int,
    val totalReps: Int,
    val prCount: Int,
    val exercises: List<ShareCardExercise>
)

fun shareCardDataOf(
    summary: SessionSummary,
    exercises: List<ShareCardExercise>
): ShareCardData = ShareCardData(
    title = formatFullDate(summary.startTime),
    subtitle = formatTime(summary.startTime),
    // Senza endTime la durata non e' ricostruibile: si stampa un trattino invece di un finto zero.
    durationLabel = summary.durationMinutes?.let { formatDuration(it) } ?: "—",
    volumeKg = summary.volumeKg,
    setCount = summary.setCount,
    totalReps = summary.totalReps,
    prCount = summary.prCount,
    exercises = exercises
)

/** Etichetta della serie migliore, coerente col weightType (kg, ripetizioni o secondi). */
fun bestSetLabel(weightType: WeightType, reps: Int?, weight: Double?): String =
    when (weightType) {
        WeightType.TIME_BASED -> "${reps ?: 0} s"
        WeightType.BODYWEIGHT -> "${reps ?: 0} rip."
        WeightType.BODYWEIGHT_PLUS_LOAD -> "+${formatDecimal(weight ?: 0.0)} kg × ${reps ?: 0}"
        WeightType.ASSISTED -> "-${formatDecimal(weight ?: 0.0)} kg × ${reps ?: 0}"
        else -> "${formatDecimal(weight ?: 0.0)} kg × ${reps ?: 0}"
    }

// --- Disegno ---------------------------------------------------------------
// DECISIONE: la card e' disegnata con android.graphics invece di catturare una view Compose:
// dimensione fissa e indipendente dallo schermo, dal tema attivo e dal ciclo di vita della UI.
// La card e' sempre chiara, anche se un giorno l'app avesse un tema scuro: un'immagine
// condivisa finisce su sfondi altrui e il bianco e' l'unico che regge ovunque.

private const val CARD_WIDTH = 1080
private const val PADDING = 88f

private const val BG = 0xFFFFFFFF.toInt()
private const val ACCENT = 0xFF7A5AF8.toInt()
private const val TEXT = 0xFF16151F.toInt()
private const val TEXT_SECONDARY = 0xFF7C7A93.toInt()
private const val HAIRLINE = 0xFFE7E4F3.toInt()

private const val MAX_EXERCISE_ROWS = 5
private const val EXERCISE_ROW_HEIGHT = 62f

private fun textPaint(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        letterSpacing = spacing
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

private fun Canvas.drawEllipsized(text: String, x: Float, y: Float, maxWidth: Float, paint: TextPaint) {
    val clipped = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END)
    drawText(clipped.toString(), x, y, paint)
}

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
 * Marchio stilizzato dell'app: tessera viola con tre barre bianche di lunghezza decrescente.
 * Si legge come una "E" e come un grafico che sale, e non usa asset esterni.
 */
private fun Canvas.drawLogoMark(left: Float, top: Float, size: Float) {
    drawRoundRect(RectF(left, top, left + size, top + size), size * 0.3f, size * 0.3f, fill(ACCENT))

    val barHeight = size * 0.11f
    val barLeft = left + size * 0.24f
    val widths = listOf(size * 0.52f, size * 0.36f, size * 0.52f)
    var barTop = top + size * 0.26f
    widths.forEach { width ->
        drawRoundRect(
            RectF(barLeft, barTop, barLeft + width, barTop + barHeight),
            barHeight / 2f,
            barHeight / 2f,
            fill(BG)
        )
        barTop += barHeight + size * 0.075f
    }
}

/**
 * Immagine verticale larga 1080 pronta per lo share: intestazione col marchio, data, tre
 * metriche essenziali (durata, volume, serie) e l'elenco degli esercizi. L'altezza si adatta
 * al numero di righe, cosi' una sessione da un esercizio non lascia mezza card vuota.
 */
fun renderShareCard(data: ShareCardData): Bitmap {
    val rowCount = minOf(data.exercises.size, MAX_EXERCISE_ROWS)
    val hiddenCount = data.exercises.size - rowCount

    val exercisesTop = 690f
    var contentEnd = exercisesTop + rowCount * EXERCISE_ROW_HEIGHT
    if (hiddenCount > 0) contentEnd += 48f
    if (data.prCount > 0) contentEnd += 76f

    val cardHeight = (contentEnd + 150f).toInt().coerceAtLeast(900)

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, cardHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(BG)

    val contentWidth = CARD_WIDTH - PADDING * 2
    val right = CARD_WIDTH - PADDING

    // Intestazione: marchio + wordmark
    val logoSize = 96f
    canvas.drawLogoMark(PADDING, PADDING, logoSize)
    canvas.drawText(
        "Eina",
        PADDING + logoSize + 28f,
        PADDING + logoSize / 2f + 18f,
        textPaint(50f, TEXT, bold = true)
    )

    // Data e ora
    val titlePaint = fitted(data.title, contentWidth, textPaint(66f, TEXT, bold = true), minSize = 40f)
    canvas.drawText(data.title, PADDING, 312f, titlePaint)
    canvas.drawText(data.subtitle, PADDING, 366f, textPaint(32f, TEXT_SECONDARY))

    canvas.drawRect(PADDING, 424f, right, 425f, fill(HAIRLINE))

    // Tre metriche essenziali, senza riquadri: contano i numeri, non i contenitori.
    val metrics = listOf(
        "DURATA" to data.durationLabel,
        "VOLUME" to "${formatVolume(data.volumeKg)} kg",
        "SERIE" to data.setCount.toString()
    )
    val columnWidth = contentWidth / 3f
    metrics.forEachIndexed { index, (label, value) ->
        val left = PADDING + columnWidth * index
        canvas.drawText(label, left, 500f, textPaint(24f, TEXT_SECONDARY, spacing = 0.14f))
        val valuePaint = fitted(value, columnWidth - 24f, textPaint(60f, TEXT, bold = true), minSize = 34f)
        canvas.drawText(value, left, 570f, valuePaint)
    }

    canvas.drawRect(PADDING, 618f, right, 619f, fill(HAIRLINE))

    // Esercizi: nome a sinistra, serie migliore a destra, pallino viola sui PR.
    var y = exercisesTop
    data.exercises.take(MAX_EXERCISE_ROWS).forEach { exercise ->
        val namePaint = textPaint(34f, TEXT, bold = false)
        val detailPaint = textPaint(30f, TEXT_SECONDARY)
        val detail = exercise.bestSetLabel ?: "${exercise.setCount} serie"
        val detailWidth = detailPaint.measureText(detail)

        var nameLeft = PADDING
        if (exercise.hasPr) {
            canvas.drawCircle(PADDING + 9f, y - 11f, 9f, fill(ACCENT))
            nameLeft += 32f
        }
        canvas.drawEllipsized(
            text = exercise.name,
            x = nameLeft,
            y = y,
            maxWidth = contentWidth - (nameLeft - PADDING) - detailWidth - 32f,
            paint = namePaint
        )
        canvas.drawRightAligned(detail, right, y, detailPaint)
        y += EXERCISE_ROW_HEIGHT
    }

    if (hiddenCount > 0) {
        canvas.drawText("+ altri $hiddenCount esercizi", PADDING, y, textPaint(30f, TEXT_SECONDARY))
        y += 48f
    }

    if (data.prCount > 0) {
        val label = if (data.prCount == 1) "1 record personale" else "${data.prCount} record personali"
        val paint = textPaint(28f, ACCENT, bold = true, spacing = 0.04f)
        val pill = RectF(PADDING, y - 4f, PADDING + paint.measureText(label) + 56f, y + 56f)
        canvas.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, fill(0xFFEDE8FF.toInt()))
        canvas.drawText(label, pill.left + 28f, pill.centerY() + 10f, paint)
    }

    // Piede ancorato al fondo reale della card, non all'ultima riga.
    val footerPaint = textPaint(26f, TEXT_SECONDARY)
    val footer = "Registrato con Eina"
    canvas.drawText(
        footer,
        (CARD_WIDTH - footerPaint.measureText(footer)) / 2f,
        cardHeight - 60f,
        footerPaint
    )

    return bitmap
}
