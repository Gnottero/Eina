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
import com.eina.app.R
import com.eina.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * Cronometro libero, separato dal timer di recupero: serve per una plank, un giro di corsa, una
 * pausa cronometrata a mano.
 *
 * DECISIONE: nessun job che gira per conto suo. Lo stato e' l'istante di partenza piu' il tempo
 * gia' accumulato, quindi il conteggio resta giusto anche mentre nessuna schermata lo guarda —
 * e' la UI aperta a ridisegnarlo dieci volte al secondo. Vive come singleton (vedi AppModule),
 * cosi' avviarlo in Dashboard e ritrovarlo durante l'allenamento e' lo stesso cronometro.
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

    /** Fermo e a zero: il tasto "Azzera" non ha niente da azzerare. */
    val isIdle: Boolean get() = !running && accumulatedMs == 0L
}

/** "12:34.5" — decimi di secondo perche' un cronometro fermo al secondo sembra rotto. */
fun formatStopwatch(elapsedMs: Long): String {
    val safe = elapsedMs.coerceAtLeast(0L)
    val minutes = safe / 60_000
    val seconds = (safe % 60_000) / 1000
    val tenths = (safe % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}

/**
 * Foglio del cronometro: un numero grande, avvio/pausa e azzeramento. Il conteggio a schermo
 * si aggiorna qui, non nel controller (vedi [StopwatchController]).
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
 * Tasto tondo che apre il cronometro. Acceso quando sta girando, cosi' si vede da fuori che c'e'
 * un conteggio in corso senza aprire il foglio.
 */
@Composable
fun StopwatchIconButton(
    running: Boolean,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    IslandIconButton(
        icon = Icons.Outlined.Timer,
        contentDescription = stringResource(R.string.stopwatch_open_cd),
        onClick = onClick,
        containerColor = if (running) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else containerColor,
        contentColor = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
}
