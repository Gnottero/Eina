package com.eina.app.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    subtitle = buildString {
        append(formatTime(summary.startTime))
        summary.durationMinutes?.let { append(" · ${formatDuration(it)}") }
    },
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

private const val CARD_WIDTH = 1080
private const val PADDING = 76f

private const val EXERCISES_TOP = 736f
private const val ROW_HEIGHT = 92f
private const val FOOTER_GAP = 96f

private const val BG = 0xFF1C1C1E.toInt()
private const val SURFACE = 0xFF2C2C2E.toInt()
private const val ACCENT = 0xFFFF6A3D.toInt()
private const val TEXT = 0xFFF7F7F8.toInt()
private const val TEXT_SECONDARY = 0xFF98989E.toInt()

// 5 righe da 92px partono a y=736 e chiudono a 1196: sotto restano la riga "+ altri N" e il piede.
private const val MAX_EXERCISE_ROWS = 5

private fun textPaint(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        letterSpacing = spacing
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

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
 * Immagine verticale larga 1080 pronta per lo share. L'altezza si adatta al numero di esercizi
 * (fra 4:5 e quasi quadrata) cosi' una sessione da un esercizio non lascia meta' card vuota.
 */
fun renderShareCard(data: ShareCardData): Bitmap {
    val rowCount = minOf(data.exercises.size, MAX_EXERCISE_ROWS)
    val hiddenCount = data.exercises.size - rowCount
    val contentEnd = EXERCISES_TOP + rowCount * ROW_HEIGHT - 12f + if (hiddenCount > 0) 46f else 0f
    val footerBaseline = contentEnd + FOOTER_GAP
    val cardHeight = (footerBaseline + 68f).toInt().coerceIn(CARD_WIDTH, 1350)

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, cardHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(BG)

    val contentWidth = CARD_WIDTH - PADDING * 2
    val right = CARD_WIDTH - PADDING

    // Wordmark + conteggio PR
    canvas.drawText("EINA", PADDING, PADDING + 40f, textPaint(40f, ACCENT, bold = true, spacing = 0.18f))
    if (data.prCount > 0) {
        val label = if (data.prCount == 1) "1 RECORD" else "${data.prCount} RECORD"
        val paint = textPaint(28f, Color.WHITE, bold = true, spacing = 0.1f)
        val pillWidth = paint.measureText(label) + 48f
        val pill = RectF(right - pillWidth, PADDING - 4f, right, PADDING + 56f)
        canvas.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT })
        canvas.drawText(label, pill.left + 24f, pill.centerY() + 10f, paint)
    }

    // Titolo + sottotitolo
    val titlePaint = fitted(data.title, contentWidth, textPaint(72f, TEXT, bold = true), minSize = 44f)
    canvas.drawText(data.title, PADDING, 280f, titlePaint)
    canvas.drawText(data.subtitle, PADDING, 336f, textPaint(34f, TEXT_SECONDARY))

    // Tile metriche
    val tileTop = 400f
    val tileHeight = 200f
    val gap = 24f
    val tileWidth = (contentWidth - gap * 2) / 3f
    val tiles = listOf(
        Triple("VOLUME", formatVolume(data.volumeKg), "kg"),
        Triple("SERIE", data.setCount.toString(), null),
        Triple("RIPETIZIONI", data.totalReps.toString(), null)
    )
    tiles.forEachIndexed { index, (label, value, unit) ->
        val left = PADDING + (tileWidth + gap) * index
        val rect = RectF(left, tileTop, left + tileWidth, tileTop + tileHeight)
        canvas.drawRoundRect(rect, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SURFACE })
        canvas.drawText(label, rect.left + 32f, rect.top + 62f, textPaint(24f, TEXT_SECONDARY, spacing = 0.1f))
        val valuePaint = fitted(value, tileWidth - 64f, textPaint(64f, if (index == 0) ACCENT else TEXT, bold = true), minSize = 36f)
        canvas.drawText(value, rect.left + 32f, rect.top + 148f, valuePaint)
        if (unit != null) {
            val offset = valuePaint.measureText(value)
            canvas.drawText(unit, rect.left + 32f + offset + 10f, rect.top + 148f, textPaint(28f, TEXT_SECONDARY))
        }
    }

    // Elenco esercizi
    canvas.drawText("ESERCIZI", PADDING, EXERCISES_TOP - 40f, textPaint(26f, TEXT_SECONDARY, spacing = 0.12f))
    var y = EXERCISES_TOP

    val shown = data.exercises.take(MAX_EXERCISE_ROWS)
    val rowHeight = ROW_HEIGHT
    shown.forEach { exercise ->
        val rect = RectF(PADDING, y, right, y + rowHeight - 12f)
        canvas.drawRoundRect(rect, 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SURFACE })

        val detail = buildString {
            append("${exercise.setCount} serie")
            exercise.bestSetLabel?.let { append(" · $it") }
        }
        val detailPaint = textPaint(28f, TEXT_SECONDARY)
        val prWidth = if (exercise.hasPr) 78f else 0f
        val detailWidth = detailPaint.measureText(detail)
        val namePaint = textPaint(34f, TEXT, bold = true)
        val baseline = rect.centerY() + 12f

        canvas.drawEllipsized(
            text = exercise.name,
            x = rect.left + 32f,
            y = baseline,
            maxWidth = rect.width() - 96f - detailWidth - prWidth,
            paint = namePaint
        )
        canvas.drawRightAligned(detail, rect.right - 32f - prWidth, baseline, detailPaint)
        if (exercise.hasPr) {
            val prPaint = textPaint(24f, Color.WHITE, bold = true, spacing = 0.08f)
            val pill = RectF(rect.right - 88f, rect.centerY() - 22f, rect.right - 24f, rect.centerY() + 22f)
            canvas.drawRoundRect(pill, 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT })
            canvas.drawText("PR", pill.centerX() - prPaint.measureText("PR") / 2f, pill.centerY() + 8f, prPaint)
        }
        y += rowHeight
    }

    if (hiddenCount > 0) {
        canvas.drawText(
            "+ altri $hiddenCount esercizi",
            PADDING + 32f,
            y + 34f,
            textPaint(28f, TEXT_SECONDARY)
        )
    }

    // Piede: ancorato al fondo reale della card, non all'ultima riga.
    val footerPaint = textPaint(28f, TEXT_SECONDARY)
    val footer = "Registrato con Eina"
    canvas.drawText(
        footer,
        (CARD_WIDTH - footerPaint.measureText(footer)) / 2f,
        cardHeight - PADDING,
        footerPaint
    )

    return bitmap
}
