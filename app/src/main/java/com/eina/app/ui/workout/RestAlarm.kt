package com.eina.app.ui.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Rest-timer alarm.
 *
 * The countdown lives in [RestTimerController], but that is a coroutine inside the process: once
 * the app leaves the screen the system freezes it and the `delay` calls stop advancing, so sound
 * and vibration only arrived when the app came back to the foreground. AlarmManager is the only way
 * to be woken on time without keeping a foreground service alive for the whole session.
 *
 * The alarm duplicates the in-app tick rather than replacing it: whichever fires first calls
 * [RestTimerController.finish], which acts only once.
 *
 * The instant is also written down. A broadcast can reach a process that no longer holds the rest
 * it was scheduled for — Android is free to kill the app the moment it leaves the screen, which is
 * exactly when the rest matters — and the controller rebuilt from nothing would have found no rest
 * running and stayed silent, which is what the end of a rest sounded like from inside another app.
 */
class RestAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun schedule(triggerAtMs: Long) {
        prefs.edit().putLong(KEY_END_AT, triggerAtMs).apply()
        val manager = alarmManager ?: return
        val intent = pendingIntent()
        // setExactAndAllowWhileIdle survives doze; without the exact-alarm permission (revocable on
        // Android 12 and 13) it falls back to an inexact alarm, which still arrives with the screen
        // on — the common case in a gym.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, intent)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, intent)
        }
    }

    fun cancel() {
        prefs.edit().remove(KEY_END_AT).apply()
        alarmManager?.cancel(pendingIntent())
    }

    /**
     * Whether a rest was still pending, clearing the record. Answers true once and only for an
     * alarm whose instant has actually come: a stale record left by a process killed mid-rest must
     * not beep hours later, when the alarm it belonged to fires late.
     */
    fun consumeDue(now: Long = System.currentTimeMillis()): Boolean {
        val endAt = prefs.getLong(KEY_END_AT, 0L)
        prefs.edit().remove(KEY_END_AT).apply()
        return endAt > 0L && now >= endAt - TOLERANCE_MS && now <= endAt + STALE_AFTER_MS
    }

    // One live alarm at a time: the same PendingIntent (same request code, same action) is reused,
    // so scheduling another replaces the previous one.
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, RestAlarmReceiver::class.java).setAction(RestAlarmReceiver.ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val REQUEST_CODE = 1001
        const val PREFS_NAME = "eina_rest_timer"
        const val KEY_END_AT = "rest_end_at"

        /** An inexact alarm may arrive slightly early; it is still the end of that rest. */
        const val TOLERANCE_MS = 5_000L

        /** Past this, the record belongs to a rest nobody is waiting for any more. */
        const val STALE_AFTER_MS = 10 * 60 * 1000L
    }
}

/** Receives the alarm and ends the rest: this is where sound and vibration fire in background. */
class RestAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val controller: RestTimerController by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        // The beep lasts most of a second and the vibration nearly as long, and a receiver whose
        // onReceive has returned takes its process with it: without holding the broadcast open,
        // the process could be killed mid-tone, which is the silence this alarm exists to prevent.
        val pending = goAsync()
        controller.finish()
        CoroutineScope(Dispatchers.Default).launch {
            delay(FEEDBACK_MS)
            runCatching { pending.finish() }
        }
    }

    companion object {
        const val ACTION = "com.eina.app.REST_TIMER_FINISHED"

        /** Long enough for the tone and the double buzz to play out; see WorkoutFeedback. */
        private const val FEEDBACK_MS = 1_500L
    }
}
