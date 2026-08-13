package com.eina.app.ui.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
 */
class RestAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(triggerAtMs: Long) {
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
        alarmManager?.cancel(pendingIntent())
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
    }
}

/** Receives the alarm and ends the rest: this is where sound and vibration fire in background. */
class RestAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val controller: RestTimerController by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        controller.finish()
    }

    companion object {
        const val ACTION = "com.eina.app.REST_TIMER_FINISHED"
    }
}
