package com.eina.app.ui.components

import android.content.Context
import com.eina.app.R
import com.eina.app.data.db.WeightType
import com.eina.app.domain.PrRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// I pattern di data restano gli stessi, cambia la lingua con cui java.time li riempie:
// "12 mar" in italiano, "12 Mar" in inglese, "12 mars" in francese. Locale.getDefault()
// segue la lingua scelta in Impostazioni, che MainActivity applica anche al processo.
// Un formatter viene ricostruito solo quando la lingua cambia davvero.
// I tre formatter viaggiano insieme in un oggetto solo, sostituito con una scrittura sola: il
// riepilogo si formatta anche fuori dal thread della UI (vedi renderShareCard), e tre campi
// riscritti uno alla volta si possono leggere a meta' da un altro thread.
private class Formatters(val locale: Locale) {
    val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
}

@Volatile
private var cachedFormatters: Formatters? = null

private fun formatters(): Formatters {
    val locale = Locale.getDefault()
    val cached = cachedFormatters
    if (cached != null && cached.locale == locale) return cached
    return Formatters(locale).also { cachedFormatters = it }
}

fun localDateOf(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatDayMonth(millis: Long): String = localDateOf(millis).format(formatters().dayMonth)

fun formatFullDate(millis: Long): String = formatFullDate(localDateOf(millis))

fun formatFullDate(date: LocalDate): String =
    date.format(formatters().fullDate).replaceFirstChar { it.uppercase() }

/**
 * Iniziali dei giorni, da lunedi': "L M M G V S D" in italiano, "M T W T F S S" in inglese.
 * Le fornisce java.time a partire dalla lingua attiva, non una lista scritta a mano.
 */
fun weekDayInitials(): List<String> {
    val locale = Locale.getDefault()
    return (0..6).map { offset ->
        DayOfWeek.MONDAY.plus(offset.toLong())
            .getDisplayName(TextStyle.NARROW, locale)
            .uppercase(locale)
    }
}

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(formatters().time)

/** Numero compatto: sopra i 1000 kg si passa a "12,4k" per non far esplodere le tile. */
fun formatVolume(kg: Double): String = when {
    kg <= 0.0 -> "0"
    kg >= 1000.0 -> String.format(Locale.getDefault(), "%.1fk", kg / 1000.0)
    else -> String.format(Locale.getDefault(), "%.0f", kg)
}

fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }

// "h" e "min" si scrivono uguali nelle tre lingue: nessuna stringa da tradurre.
fun formatDuration(minutes: Long): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}min" else "${minutes}min"

/** Valore del PR letto secondo il weightType (kg, ripetizioni o secondi). */
fun Context.formatPrValue(record: PrRecord): String = when (record.weightType) {
    WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK ->
        "${formatDecimal(record.weight ?: 0.0)} kg × ${record.reps ?: 0}"

    WeightType.ASSISTED ->
        "-${formatDecimal(record.weight ?: 0.0)} kg × ${record.reps ?: 0}"

    WeightType.BODYWEIGHT ->
        getString(R.string.unit_reps_value, record.reps ?: 0)

    WeightType.BODYWEIGHT_PLUS_LOAD ->
        "+${formatDecimal(record.weight ?: 0.0)} kg × ${record.reps ?: 0}"

    WeightType.TIME_BASED ->
        "${record.reps ?: 0} s"

    WeightType.DISTANCE_BASED -> formatDistanceAndTime(record.weight, record.reps)
}

/**
 * Serie a distanza: chilometri e minuti insieme, perche' ne' l'uno ne' l'altro da solo dice
 * com'e' andata (vedi [WeightType.DISTANCE_BASED]).
 */
fun formatDistanceAndTime(distanceKm: Double?, minutes: Int?): String {
    val distance = distanceKm?.let { "${formatDecimal(it)} km" }
    val time = minutes?.takeIf { it > 0 }?.let { "$it min" }
    return listOfNotNull(distance, time).joinToString(" · ").ifBlank { "–" }
}

/** Relativo e breve: "oggi", "ieri", poi la data. */
fun Context.formatRelativeDay(millis: Long, today: LocalDate = LocalDate.now()): String {
    val date = localDateOf(millis)
    return when (date) {
        today -> getString(R.string.date_today)
        today.minusDays(1) -> getString(R.string.date_yesterday)
        else -> date.format(formatters().dayMonth)
    }
}
