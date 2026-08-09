package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendario island: griglia di giorni tondi su superficie bianca, senza la cornice Material.
 * Il giorno scelto e' un cerchio pieno d'accento, oggi ha solo il contorno, i giorni fuori dal
 * limite (di norma il futuro) restano spenti e non si toccano.
 *
 * La settimana parte dal primo giorno della lingua attiva (lunedi' in Italia, domenica negli
 * Stati Uniti) e i nomi di mese e giorno arrivano da java.time con lo stesso Locale.
 */
@Composable
fun IslandCalendar(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
    minDate: LocalDate = selected.minusYears(5),
    maxDate: LocalDate = LocalDate.now()
) {
    val island = EinaTheme.island
    val accent = MaterialTheme.colorScheme.primary
    val tap = LocalHapticTap.current
    val today = remember { LocalDate.now() }
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }

    val firstDayOfWeek = remember(locale) { java.time.temporal.WeekFields.of(locale).firstDayOfWeek }
    val weekDays = remember(firstDayOfWeek) {
        (0L until 7L).map { firstDayOfWeek.plus(it) }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonthArrow(
                icon = Icons.Outlined.ChevronLeft,
                contentDescription = stringResource(R.string.calendar_previous_month),
                enabled = month > YearMonth.from(minDate),
                onClick = { month = month.minusMonths(1) }
            )
            Text(
                text = monthTitle(month, locale),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            MonthArrow(
                icon = Icons.Outlined.ChevronRight,
                contentDescription = stringResource(R.string.calendar_next_month),
                enabled = month < YearMonth.from(maxDate),
                onClick = { month = month.plusMonths(1) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = island.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Griglia fissa a 6 righe: cambiando mese le celle non ballano in verticale.
        val leading = leadingBlanks(month, firstDayOfWeek)
        val length = month.lengthOfMonth()
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(6) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { index ->
                        val dayNumber = week * 7 + index - leading + 1
                        val date = if (dayNumber in 1..length) month.atDay(dayNumber) else null
                        DayCell(
                            date = date,
                            isSelected = date == selected,
                            isToday = date == today,
                            enabled = date != null && date >= minDate && date <= maxDate,
                            accent = accent,
                            onClick = { picked -> tap(); onSelect(picked) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val island = EinaTheme.island
    val tap = LocalHapticTap.current
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (enabled) MaterialTheme.colorScheme.onSurface else island.outlineSubtle,
        modifier = Modifier
            .clip(PillShape)
            .clickable(enabled = enabled) { tap(); onClick() }
            .padding(Spacing.sm)
    )
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (date == null) return@Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(PillShape)
                .background(if (isSelected) accent else Color.Transparent)
                .then(
                    if (isToday && !isSelected) {
                        Modifier.border(1.5.dp, accent, PillShape)
                    } else {
                        Modifier
                    }
                )
                .clickable(enabled = enabled) { onClick(date) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !enabled -> island.outlineSubtle
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private fun leadingBlanks(month: YearMonth, firstDayOfWeek: DayOfWeek): Int {
    val firstDay = month.atDay(1).dayOfWeek.value
    return (firstDay - firstDayOfWeek.value + 7) % 7
}

private fun monthTitle(month: YearMonth, locale: Locale): String {
    val name = month.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    return "$name ${month.year}"
}
