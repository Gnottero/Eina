package com.eina.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * Free stopwatch, separate from the rest timer: for a plank, a lap, a manually timed pause.
 *
 * DECISIONE: no job running on its own. The state is the start instant plus the accumulated time,
 * so the count stays correct while no screen is watching — the open UI redraws it ten times a
 * second. It lives as a singleton (see AppModule), so starting it on the Dashboard and finding it
 * during a workout is the same stopwatch.
 */
class StopwatchController {
    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    fun toggle(now: Long = System.currentTimeMillis()) {
        _state.update { current ->
            if (current.running) {
                current.copy(running = false, startedAt = null, accumulatedMs = current.elapsedMs(now))
            } else {
                current.copy(running = true, startedAt = now)
            }
        }
    }

    fun reset() {
        _state.value = StopwatchState()
    }
}

data class StopwatchState(
    val running: Boolean = false,
    val startedAt: Long? = null,
    val accumulatedMs: Long = 0L
) {
    fun elapsedMs(now: Long = System.currentTimeMillis()): Long =
        accumulatedMs + if (running && startedAt != null) (now - startedAt).coerceAtLeast(0L) else 0L

    /** Stopped and at zero: the reset button has nothing to reset. */
    val isIdle: Boolean get() = !running && accumulatedMs == 0L
}

/** "12:34.5" — tenths of a second, because a stopwatch ticking whole seconds looks broken. */
fun formatStopwatch(elapsedMs: Long): String {
    val safe = elapsedMs.coerceAtLeast(0L)
    val minutes = safe / 60_000
    val seconds = (safe % 60_000) / 1000
    val tenths = (safe % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}

/**
 * Stopwatch sheet: one large number, start/pause and reset. The on-screen count is refreshed here
 * and not in the controller (see [StopwatchController]).
 */
@Composable
fun StopwatchSheet(controller: StopwatchController, onDismiss: () -> Unit) {
    val state by controller.state.collectAsState()
    var elapsed by remember { mutableLongStateOf(state.elapsedMs()) }

    LaunchedEffect(state) {
        elapsed = state.elapsedMs()
        while (state.running) {
            delay(100)
            elapsed = state.elapsedMs()
        }
    }

    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.stopwatch_title)) {
        Text(
            text = formatStopwatch(elapsed),
            style = MaterialTheme.typography.displayMedium,
            color = if (state.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            IslandSecondaryButton(
                text = stringResource(R.string.stopwatch_reset),
                onClick = { controller.reset() },
                enabled = !state.isIdle,
                modifier = Modifier.weight(1f)
            )
            IslandButton(
                text = stringResource(
                    when {
                        state.running -> R.string.stopwatch_pause
                        state.accumulatedMs > 0L -> R.string.stopwatch_resume
                        else -> R.string.stopwatch_start
                    }
                ),
                icon = if (state.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                onClick = { controller.toggle() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Round button opening the stopwatch. Highlighted while it runs, so a count in progress is visible
 * without opening the sheet.
 */
@Composable
fun StopwatchIconButton(
    running: Boolean,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    size: Dp = 44.dp
) {
    IslandIconButton(
        icon = Icons.Outlined.Timer,
        contentDescription = stringResource(R.string.stopwatch_open_cd),
        onClick = onClick,
        size = size,
        containerColor = if (running) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else containerColor,
        contentColor = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
}
