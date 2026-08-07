package com.eina.app.ui.components

import com.eina.app.data.db.WeightType
import com.eina.app.domain.PrRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)

fun localDateOf(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatDayMonth(millis: Long): String = localDateOf(millis).format(dayMonthFormatter)

fun formatFullDate(millis: Long): String =
    localDateOf(millis).format(fullDateFormatter).replaceFirstChar { it.uppercase() }

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)

/** Numero compatto: sopra i 1000 kg si passa a "12,4k" per non far esplodere le tile. */
fun formatVolume(kg: Double): String = when {
    kg <= 0.0 -> "0"
    kg >= 1000.0 -> String.format(Locale.ITALIAN, "%.1fk", kg / 1000.0)
    else -> String.format(Locale.ITALIAN, "%.0f", kg)
}

fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale.ITALIAN, "%.0f", value)
    } else {
        String.format(Locale.ITALIAN, "%.1f", value)
    }

fun formatDuration(minutes: Long): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}min" else "${minutes}min"

/** Valore del PR letto secondo il weightType (kg, ripetizioni o secondi). */
fun formatPrValue(record: PrRecord): String = when (record.weightType) {
    WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK ->
        "${formatDecimal(record.weight ?: 0.0)} kg × ${record.reps ?: 0}"

    WeightType.ASSISTED ->
        "-${formatDecimal(record.weight ?: 0.0)} kg × ${record.reps ?: 0}"

    WeightType.BODYWEIGHT ->
        "${record.reps ?: 0} rip."

    WeightType.BODYWEIGHT_PLUS_LOAD ->
        "+${formatDecimal(record.weight ?: 0.0)} kg × ${record.reps ?: 0}"

    WeightType.TIME_BASED ->
        "${record.reps ?: 0} s"
}

/** Relativo e breve: "oggi", "ieri", poi la data. */
fun formatRelativeDay(millis: Long, today: LocalDate = LocalDate.now()): String {
    val date = localDateOf(millis)
    return when (date) {
        today -> "Oggi"
        today.minusDays(1) -> "Ieri"
        else -> date.format(dayMonthFormatter)
    }
}
