package com.eina.app.ui.workout

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.eina.app.MainActivity
import com.eina.app.R
import java.util.Locale

/**
 * The rest timer as seen from outside the app.
 *
 * A rest is the one stretch of a workout spent looking at something else, and until now the count
 * lived only on the workout screen: leaving it left nothing behind, and the end of the rest was
 * announced by a beep that the frozen process often never got round to playing.
 *
 * Two notifications, on two channels:
 * - the countdown, ongoing and silent, carrying the time left both as text and as an instant for
 *   the system to render as a chronometer;
 * - the end, which replaces it and is the thing that survives the app being nowhere near the
 *   foreground.
 *
 * Neither channel carries a sound or a vibration of its own: those stay with [com.eina.app.ui
 * .feedback.WorkoutFeedback], which honours the two switches in the settings. A channel's sound
 * cannot be changed after it is created, and the user turning the beep off would not turn it off.
 */
class RestNotifications(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    /** Whether notifications may be posted at all; from Android 13 this is a runtime permission. */
    val allowed: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Both channels, created once. Cheap to repeat, so it runs from the application start and
     * again before every post: the alarm can be received by a process that was just created for
     * it, where nothing else has run yet.
     */
    fun ensureChannels() {
        val manager = manager ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_RUNNING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RUNNING,
                    context.getString(R.string.rest_channel_running),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.rest_channel_running_description)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        if (manager.getNotificationChannel(CHANNEL_DONE) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DONE,
                    context.getString(R.string.rest_channel_done),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.rest_channel_done_description)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    /**
     * The running countdown, said twice.
     *
     * `setWhen` plus the chronometer flags is the cheap way: the system draws the remaining time
     * itself, once a second, with this process asleep. It is also the way an OEM is free to ignore
     * — on the phone this was tested on, MagicOS renders the instant as a coarse relative stamp
     * ("in 1 min") that never moves — so the time is written into the text as well, and the caller
     * re-posts on every second that changes. Where the chronometer works, the two agree; where it
     * does not, the text is the countdown.
     *
     * The text stops moving if the system freezes the process. That is the honest failure: the
     * rest still ends on time, because ending it is the alarm's job and not this notification's.
     */
    fun showCountdown(endAtMs: Long, remainingSeconds: Int) {
        post(
            id = ID_RUNNING,
            notification = base(CHANNEL_RUNNING)
                .setContentTitle(context.getString(R.string.rest_notification_title))
                .setContentText(
                    context.getString(R.string.rest_notification_remaining, format(remainingSeconds))
                )
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(endAtMs)
                .setShowWhen(true)
                .setOngoing(true)
                .setSilent(true)
                // Re-posted once a second: without this every repost would count as a fresh alert.
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }

    /** "1:05" — the same shape the timer island shows, so the two never look like two clocks. */
    private fun format(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return String.format(Locale.getDefault(), "%d:%02d", safe / 60, safe % 60)
    }

    /** End of the rest: the countdown is replaced rather than joined by a second line. */
    fun showFinished() {
        clearCountdown()
        post(
            id = ID_DONE,
            notification = base(CHANNEL_DONE)
                .setContentTitle(context.getString(R.string.rest_done_notification_title))
                .setContentText(context.getString(R.string.rest_done_notification_text))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    /** Rest skipped, adjusted away or the workout left: nothing is left counting in the shade. */
    fun clear() {
        clearCountdown()
        manager?.cancel(ID_DONE)
    }

    private fun clearCountdown() {
        manager?.cancel(ID_RUNNING)
    }

    private fun base(channelId: String) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification_eina)
        .setContentIntent(openWorkout())
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    /** Tapping either notification comes back to the app, where the workout is still open. */
    private fun openWorkout(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun post(id: Int, notification: Notification) {
        if (!allowed) return
        ensureChannels()
        runCatching { manager?.notify(id, notification) }
    }

    private companion object {
        const val CHANNEL_RUNNING = "rest_timer"
        const val CHANNEL_DONE = "rest_timer_done"
        const val ID_RUNNING = 2001
        const val ID_DONE = 2002
    }
}
