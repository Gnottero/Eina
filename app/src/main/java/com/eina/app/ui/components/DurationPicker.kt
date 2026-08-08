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
 * Selettore di durata a rulli, come la sveglia di sistema: minuti e secondi scorrono e si
 * agganciano alla riga centrale evidenziata. Sostituisce i campi numerici e le griglie di preset,
 * perche' impostare "1:30" scorrendo e' un gesto solo invece di due tocchi e una tastiera.
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

    // Il valore iniziale si arrotonda al passo del rullo: senza, un recupero di 47s non avrebbe
    // nessuna riga su cui agganciarsi e il rullo partirebbe disallineato.
    var minutes by remember {
        mutableIntStateOf((seconds / 60).coerceIn(0, maxMinutes))
    }
    var restSeconds by remember {
        val nearest = secondValues.minByOrNull { kotlin.math.abs(it - seconds % 60) } ?: 0
        mutableIntStateOf(nearest)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Banda della selezione: sta sotto ai rulli, cosi' il numero al centro resta leggibile.
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
 * Rulli ore/minuti, per la durata di un allenamento intero: [DurationWheelPicker] arriva a dieci
 * minuti e conta i secondi, qui servono le ore e il minuto esatto.
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
 * Foglio del tempo di recupero, unico per routine e allenamento in corso: rulli stile sveglia e
 * una conferma. Il valore si applica solo su "Fatto", perche' scorrendo si passa per decine di
 * valori intermedi che non vanno scritti nel database uno per uno.
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

/** mm:ss, il formato con cui il recupero viene mostrato ovunque nell'app. */
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
 * Un rullo. Il valore scelto e' quello incorniciato dalla banda centrale: con due elementi di
 * padding sopra e sotto, coincide con il primo elemento visibile una volta agganciato lo snap.
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
    // Letto direttamente, firstVisibleItemIndex ricomporrebbe ogni riga a ogni fotogramma di
    // scorrimento: cosi' invece le righe si ricompongono solo quando il valore incorniciato cambia.
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
