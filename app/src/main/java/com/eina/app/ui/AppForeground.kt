package com.eina.app.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Whether any screen of the app is in front of the user.
 *
 * Only the rest timer asks: its end is announced with a notification when the user is somewhere
 * else, and with nothing at all when they are looking at the running workout, where the timer
 * already says so and a banner over it would be shouting into the room it is standing in.
 *
 * Counted from the activity callbacks rather than read from ActivityManager, which describes the
 * process and not the screen, and would count a process woken by the alarm as foreground.
 */
object AppForeground {

    private var started = 0

    val isForeground: Boolean get() = started > 0

    fun track(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++
            }

            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
            }

            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
