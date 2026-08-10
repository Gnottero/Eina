package com.eina.app.ui.workout

import com.eina.app.ui.feedback.WorkoutFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Timer di recupero, fuori dal ViewModel dell'allenamento.
 *
 * DECISIONE (come [com.eina.app.ui.components.StopwatchController]): lo stato e' l'istante di fine,
 * non un contatore decrementato a ogni tick. Due motivi:
 * - in background i `delay` vengono rallentati dal sistema, e al rientro il conto alla rovescia
 *   mostrava piu' tempo di quanto ne fosse davvero rimasto: qui il residuo si ricava sempre
 *   dall'orologio;
 * - vive come singleton (vedi AppModule), quindi uscire dall'allenamento in corso e rientrare
 *   non lo azzera piu' — prima moriva con il ViewModel della schermata.
 *
 * Il tick pero' non basta a far suonare la fine: con l'app fuori dallo schermo il processo viene
 * congelato e i `delay` restano fermi. Per questo ogni recupero programma anche una sveglia di
 * sistema ([RestAlarmScheduler]); il primo dei due che arriva chiama [finish], che vale una
 * volta sola.
 */
class RestTimerController(
    private val feedback: WorkoutFeedback,
    private val alarms: RestAlarmScheduler
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<RestTimerState?>(null)
    val state: StateFlow<RestTimerState?> = _state.asStateFlow()

    private var job: Job? = null

    fun start(totalSeconds: Int) {
        if (totalSeconds <= 0) return
        schedule(System.currentTimeMillis() + totalSeconds * 1000L, totalSeconds)
    }

    /** -15s / +15s: sposta l'istante di fine, non il residuo, cosi' resta legato all'orologio. */
    fun adjust(deltaSeconds: Int) {
        val current = _state.value ?: return
        val endAt = current.endAtMs + deltaSeconds * 1000L
        val remaining = remainingSecondsAt(endAt)
        if (remaining <= 0) finish() else schedule(endAt, maxOf(current.totalSeconds, remaining))
    }

    /** Recupero saltato a mano: si spegne tutto senza suonare. */
    fun skip() {
        stop()
    }

    /**
     * Fine del recupero: suono e vibrazione, una volta sola. La chiamano sia il tick in-app sia
     * la sveglia di sistema, e possono arrivare a pochi millisecondi di distanza — chi trova lo
     * stato gia' spento non fa nulla.
     */
    fun finish() {
        synchronized(this) {
            if (_state.value == null) return
            stop()
        }
        feedback.restTimerFinished()
    }

    private fun stop() {
        job?.cancel()
        job = null
        alarms.cancel()
        _state.value = null
    }

    private fun schedule(endAtMs: Long, totalSeconds: Int) {
        job?.cancel()
        _state.value = RestTimerState(totalSeconds, endAtMs, remainingSecondsAt(endAtMs))
        alarms.schedule(endAtMs)
        job = scope.launch {
            while (isActive) {
                // Piu' fitto del secondo: il residuo viene dall'orologio, e un tick da 1s
                // sfasato mostrerebbe lo stesso numero per quasi due secondi.
                delay(TICK_MS)
                val current = _state.value ?: break
                val remaining = remainingSecondsAt(current.endAtMs)
                if (remaining <= 0) {
                    finish()
                    break
                }
                _state.value = current.copy(remainingSeconds = remaining)
            }
        }
    }

    private companion object {
        const val TICK_MS = 200L
    }
}

data class RestTimerState(
    val totalSeconds: Int,
    val endAtMs: Long,
    val remainingSeconds: Int
)

private fun remainingSecondsAt(endAtMs: Long): Int =
    ceil((endAtMs - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(0)
