package com.eina.app.ui.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eina.app.R
import com.eina.app.ui.components.HourMinuteWheelPicker
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCalendar
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

/**
 * Conferma di fine allenamento, con data e durata correggibili: un allenamento si registra spesso
 * dopo averlo fatto ("l'ho fatto ieri, lo scrivo adesso") e il cronometro non torna se lo si e'
 * lasciato correre a vuoto.
 *
 * La data sposta solo il giorno: l'ora di inizio resta quella registrata, e la fine si ricalcola
 * dalla durata scelta.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun FinishWorkoutSheet(
    startTime: Long,
    elapsedSeconds: Int,
    onConfirm: (startTime: Long, durationSeconds: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    val locale = currentLocale()
    var start by remember { mutableLongStateOf(startTime) }
    var duration by remember { mutableIntStateOf(elapsedSeconds.coerceAtLeast(0)) }
    var datePickerOpen by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }

    val dateFormat = remember(locale) { DateFormat.getDateInstance(DateFormat.FULL, locale) }

    IslandBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.active_finish_confirm_title),
        // Col calendario aperto il foglio supera lo schermo.
        scrollable = true
    ) {
        Text(
            text = stringResource(R.string.active_finish_confirm_text),
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary
        )

        SectionHeader(title = stringResource(R.string.finish_date))
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(dateFormat.format(Date(start)), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                IslandChip(
                    text = stringResource(R.string.finish_date_today),
                    selected = daysBetween(start, System.currentTimeMillis()) == 0,
                    onClick = { start = shiftToDaysAgo(start, 0) }
                )
                IslandChip(
                    text = stringResource(R.string.finish_date_yesterday),
                    selected = daysBetween(start, System.currentTimeMillis()) == 1,
                    onClick = { start = shiftToDaysAgo(start, 1) }
                )
                IslandChip(
                    text = stringResource(R.string.finish_date_pick),
                    selected = datePickerOpen,
                    onClick = { datePickerOpen = !datePickerOpen }
                )
            }
            // Calendario nello stesso foglio e non in un secondo foglio sopra: due ModalBottomSheet
            // sovrapposti si rubano il gesto di chiusura.
            if (datePickerOpen) {
                IslandCalendar(
                    selected = Instant.ofEpochMilli(start).atZone(zone).toLocalDate(),
                    onSelect = { picked ->
                        // Mezzogiorno e non mezzanotte: withDateOf rimette comunque l'ora vera,
                        // e cosi' uno scarto di fuso non fa scivolare il giorno.
                        val millis = picked.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                        start = withDateOf(millis, start)
                    },
                    locale = locale,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionHeader(title = stringResource(R.string.finish_duration))
        HourMinuteWheelPicker(
            seconds = duration,
            onSecondsChange = { duration = it },
            modifier = Modifier.padding(vertical = Spacing.sm)
        )

        IslandButton(
            text = stringResource(R.string.active_finish_confirm_action),
            onClick = { onConfirm(start, duration); onDismiss() },
            modifier = Modifier.fillMaxWidth()
        )
    }

}

/** Riporta il giorno di `dateMillis` sull'orario di `timeMillis`: si cambia data, non ora. */
private fun withDateOf(dateMillis: Long, timeMillis: Long): Long {
    val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.YEAR, date.get(Calendar.YEAR))
        set(Calendar.MONTH, date.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun shiftToDaysAgo(timeMillis: Long, daysAgo: Int): Long {
    val target = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    return withDateOf(target.timeInMillis, timeMillis)
}

/** Giorni di calendario fra due istanti, non differenza di 24 ore: serve a evidenziare il chip. */
private fun daysBetween(from: Long, to: Long): Int {
    fun startOfDay(millis: Long) = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return ((startOfDay(to) - startOfDay(from)) / 86_400_000L).toInt()
}
