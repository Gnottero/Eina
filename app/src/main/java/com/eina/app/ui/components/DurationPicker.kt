package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import kotlinx.coroutines.flow.distinctUntilChanged

private val ITEM_HEIGHT = 46.dp
private const val VISIBLE_ITEMS = 5

/**
 * Wheel duration picker, like a system alarm clock: minutes and seconds scroll and snap to the
 * highlighted centre row. It replaces numeric fields and preset grids, since setting "1:30" by
 * scrolling is one gesture instead of two taps and a keyboard.
 */
@Composable
fun DurationWheelPicker(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxMinutes: Int = 10,
    secondStep: Int = 5
) {
    val island = EinaTheme.island
    val minuteValues = remember(maxMinutes) { (0..maxMinutes).toList() }
    val secondValues = remember(secondStep) { (0 until 60 step secondStep).toList() }

    // The initial value is rounded to the wheel step: otherwise a 47s rest would have no row to
    // snap to and the wheel would start misaligned.
    var minutes by remember {
        mutableIntStateOf((seconds / 60).coerceIn(0, maxMinutes))
    }
    var restSeconds by remember {
        val nearest = secondValues.minByOrNull { kotlin.math.abs(it - seconds % 60) } ?: 0
        mutableIntStateOf(nearest)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Selection band, drawn under the wheels so the centre number stays readable.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .clip(TileShape)
                .background(island.sunken)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                values = minuteValues,
                selected = minutes,
                onSelected = { minutes = it; onSecondsChange(it * 60 + restSeconds) },
                modifier = Modifier.width(84.dp)
            )
            WheelLabel(stringResource(R.string.wheel_min))
            WheelColumn(
                values = secondValues,
                selected = restSeconds,
                onSelected = { restSeconds = it; onSecondsChange(minutes * 60 + it) },
                modifier = Modifier.width(84.dp)
            )
            WheelLabel(stringResource(R.string.wheel_sec))
        }
    }
}

/**
 * Hour and minute wheels for the duration of a whole workout: [DurationWheelPicker] tops out at ten
 * minutes and counts seconds, while here hours and exact minutes are needed.
 */
@Composable
fun HourMinuteWheelPicker(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxHours: Int = 12
) {
    val island = EinaTheme.island
    val hourValues = remember(maxHours) { (0..maxHours).toList() }
    val minuteValues = remember { (0..59).toList() }

    var hours by remember { mutableIntStateOf((seconds / 3600).coerceIn(0, maxHours)) }
    var minutes by remember { mutableIntStateOf((seconds % 3600) / 60) }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .clip(TileShape)
                .background(island.sunken)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                values = hourValues,
                selected = hours,
                onSelected = { hours = it; onSecondsChange(it * 3600 + minutes * 60) },
                modifier = Modifier.width(84.dp)
            )
            WheelLabel(stringResource(R.string.wheel_hour))
            WheelColumn(
                values = minuteValues,
                selected = minutes,
                onSelected = { minutes = it; onSecondsChange(hours * 3600 + it * 60) },
                modifier = Modifier.width(84.dp)
            )
            WheelLabel(stringResource(R.string.wheel_min))
        }
    }
}

/**
 * Rest time sheet, shared by the routine editor and the running workout. The value is applied only
 * on confirmation, since scrolling passes through dozens of intermediate values that must not be
 * written to the database one by one.
 */
@Composable
fun RestTimeSheet(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.rest_time_title),
    description: String? = null
) {
    val island = EinaTheme.island
    var pending by remember { mutableIntStateOf(currentSeconds) }

    IslandBottomSheet(onDismiss = onDismiss, title = title) {
        DurationWheelPicker(
            seconds = currentSeconds,
            onSecondsChange = { pending = it },
            modifier = Modifier.padding(vertical = Spacing.sm)
        )

        Text(
            text = if (pending == 0) {
                stringResource(R.string.rest_none)
            } else {
                description ?: stringResource(R.string.rest_set_to, formatClock(pending))
            },
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary,
            modifier = Modifier.padding(vertical = Spacing.sm)
        )

        IslandButton(
            text = stringResource(R.string.action_done),
            onClick = { onConfirm(pending); onDismiss() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** mm:ss, the format rest is shown with everywhere in the app. */
fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

@Composable
private fun WheelLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = EinaTheme.island.textSecondary,
        modifier = Modifier.padding(end = Spacing.md)
    )
}

/**
 * One wheel. The chosen value is the one framed by the centre band: with two padding items above
 * and below, it coincides with the first visible item once the snap settles.
 */
@Composable
private fun WheelColumn(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val initialIndex = remember { values.indexOf(selected).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val edgeItems = VISIBLE_ITEMS / 2
    // Read directly, firstVisibleItemIndex would recompose every row on each scroll frame; this
    // way rows recompose only when the framed value changes.
    val selectedIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val value = values.getOrNull(index) ?: return@collect
                if (value != selected) {
                    hapticTap()
                    onSelected(value)
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT * edgeItems),
        modifier = modifier.height(ITEM_HEIGHT * VISIBLE_ITEMS)
    ) {
        itemsIndexed(values) { index, value ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(value),
                    style = if (isSelected) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else island.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
