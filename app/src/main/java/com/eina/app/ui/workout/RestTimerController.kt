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
 * Rest timer, kept outside the workout ViewModel.
 *
 * DECISIONE (as in [com.eina.app.ui.components.StopwatchController]): the state is the end instant
 * and not a counter decremented on each tick. Two reasons:
 * - in background the system slows the `delay` calls down, and on return the countdown showed more
 *   time than was actually left; here the remainder always comes from the clock;
 * - it lives as a singleton (see AppModule), so leaving the running workout and coming back no
 *   longer resets it, as it did when it died with the screen ViewModel.
 *
 * The tick alone cannot sound the end: with the app off screen the process is frozen and the
 * `delay` calls stall. Every rest therefore also schedules a system alarm ([RestAlarmScheduler]);
 * whichever fires first calls [finish], which acts only once.
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

    /** -15s / +15s: moves the end instant and not the remainder, keeping it tied to the clock. */
    fun adjust(deltaSeconds: Int) {
        val current = _state.value ?: return
        val endAt = current.endAtMs + deltaSeconds * 1000L
        val remaining = remainingSecondsAt(endAt)
        if (remaining <= 0) finish() else schedule(endAt, maxOf(current.totalSeconds, remaining))
    }

    /** Rest skipped by hand: everything stops without sounding. */
    fun skip() {
        stop()
    }

    /**
     * End of rest: sound and vibration, once. Called by both the in-app tick and the system alarm,
     * which can arrive milliseconds apart — whichever finds the state already cleared does nothing.
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
                // Faster than one second: the remainder comes from the clock, and a 1s tick out of
                // phase would show the same number for nearly two seconds.
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
