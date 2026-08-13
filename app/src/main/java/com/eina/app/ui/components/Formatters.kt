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

// The date patterns stay the same; only the language java.time fills them with changes.
// Locale.getDefault() follows the language chosen in Settings, which MainActivity also applies to
// the process, and the formatters are rebuilt only when it actually changes.
// The three of them live in one object replaced by a single write: the summary is also formatted
// off the UI thread (see renderShareCard), where three separately updated fields could be read
// half-way through.
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
 * Weekday initials starting on Monday ("M T W T F S S" in English), provided by java.time for the
 * active language rather than a hand-written list.
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

/** Compact number: above 1000 kg it switches to "12.4k" so the tiles keep their size. */
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

// "h" and "min" are written the same in all three languages: nothing to translate.
fun formatDuration(minutes: Long): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}min" else "${minutes}min"

/** PR value rendered according to the weight type (kg, reps, seconds or distance). */
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
 * Distance set: kilometres and minutes together, since neither alone describes the effort
 * (see [WeightType.DISTANCE_BASED]).
 */
fun formatDistanceAndTime(distanceKm: Double?, minutes: Int?): String {
    val distance = distanceKm?.let { "${formatDecimal(it)} km" }
    val time = minutes?.takeIf { it > 0 }?.let { "$it min" }
    return listOfNotNull(distance, time).joinToString(" · ").ifBlank { "–" }
}

/** Short relative day: "today", "yesterday", then the date. */
fun Context.formatRelativeDay(millis: Long, today: LocalDate = LocalDate.now()): String {
    val date = localDateOf(millis)
    return when (date) {
        today -> getString(R.string.date_today)
        today.minusDays(1) -> getString(R.string.date_yesterday)
        else -> date.format(formatters().dayMonth)
    }
}
