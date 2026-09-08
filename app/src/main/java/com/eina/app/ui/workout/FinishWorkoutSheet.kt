package com.eina.app.ui.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
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
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.HourMinuteWheelPicker
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCalendar
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.MetricTile
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

/**
 * Finish confirmation, with editable date and duration: a workout is often recorded after the fact,
 * and the clock is wrong if it was left running.
 *
 * The date moves the day only: the start time stays as recorded and the end is derived from the
 * chosen duration.
 *
 * A session without completed sets is deleted rather than saved: the sheet says so and hides date
 * and duration, which would not be stored anywhere.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun FinishWorkoutSheet(
    startTime: Long,
    elapsedSeconds: Int,
    isEmpty: Boolean,
    volumeKg: Double,
    setCount: Int,
    onConfirm: (startTime: Long, durationSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
    /** Discards the whole session; live workouts only, and the confirmation is the caller's. */
    onDelete: (() -> Unit)? = null,
    /** Workout already in the history: nothing is closed, only its date and duration move. */
    editing: Boolean = false
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
        title = stringResource(
            if (editing) R.string.edit_session_sheet_title else R.string.active_finish_confirm_title
        ),
        // With the calendar open the sheet exceeds the screen height.
        scrollable = true
    ) {
        Text(
            // Says which of the two questions this is: the second one, about the routine, follows
            // straight after and a sheet that reopens with different buttons reads as a mistake.
            text = if (editing) {
                stringResource(R.string.edit_session_sheet_text)
            } else {
                stringResource(
                    R.string.finish_step_one,
                    stringResource(
                        if (isEmpty) R.string.active_finish_confirm_text_empty
                        else R.string.active_finish_confirm_text
                    )
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary
        )

        // The three numbers of the session, before deciding what to do with it: an empty workout
        // is recognisable here rather than after it has been saved.
        if (!isEmpty) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                MetricTile(
                    icon = Icons.Outlined.Timer,
                    label = stringResource(R.string.stat_duration),
                    value = formatDuration(duration),
                    tint = MetricColors.Duration,
                    centered = true,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Outlined.FitnessCenter,
                    label = stringResource(R.string.stat_volume),
                    value = formatVolume(volumeKg),
                    unit = stringResource(R.string.unit_kg),
                    tint = MetricColors.Volume,
                    centered = true,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Outlined.Repeat,
                    label = stringResource(R.string.stat_sets),
                    value = setCount.toString(),
                    tint = MetricColors.Sets,
                    centered = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Leaving the sheet is a choice of its own, so it is a row and not just a tap outside:
        // the session stays open and is picked up again from the Workout tab.
        if (!editing) {
            SheetActionRow(
                icon = Icons.Outlined.Pause,
                label = stringResource(R.string.active_finish_later),
                description = stringResource(R.string.active_finish_later_description),
                onClick = onDismiss
            )
        }

        if (!isEmpty) {
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
                // Calendar inside the same sheet and not in a second one on top: two stacked
                // ModalBottomSheets steal each other's dismiss gesture.
                if (datePickerOpen) {
                    IslandCalendar(
                        selected = Instant.ofEpochMilli(start).atZone(zone).toLocalDate(),
                        onSelect = { picked ->
                            // Noon and not midnight: withDateOf restores the real time anyway, and
                            // this keeps a time zone offset from shifting the day.
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
                onSecondsChange = { duration = it }
            )
        }

        // Two ways out on one line: discard on the left, go on with the closing on the right. The
        // destructive one keeps the sunken surface and only reddens its label — it still opens a
        // confirmation, so it is not the point of no return.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (onDelete != null) {
                IslandSecondaryButton(
                    text = stringResource(R.string.active_cancel_confirm_action),
                    icon = Icons.Outlined.Delete,
                    onClick = { onDismiss(); onDelete() },
                    contentColor = DestructiveRed,
                    modifier = Modifier.weight(1f)
                )
            }
            IslandButton(
                text = stringResource(
                    if (editing) R.string.edit_session_save else R.string.active_finish_confirm_action
                ),
                onClick = { onConfirm(start, duration); onDismiss() },
                modifier = Modifier.weight(1f)
            )
        }
    }

}

/** Applies the day of [dateMillis] to the time of [timeMillis]: the date changes, the clock does not. */
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

/** Calendar days between two instants, not 24-hour spans; used to highlight the chips. */
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
