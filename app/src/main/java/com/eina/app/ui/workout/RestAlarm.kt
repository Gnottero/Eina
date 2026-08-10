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
 * Sveglia di fine recupero.
 *
 * Il conto alla rovescia vive in [RestTimerController], che pero' e' un coroutine dentro il
 * processo: appena l'app esce dallo schermo il sistema congela il processo e i `delay` smettono
 * di scorrere, quindi suono e vibrazione arrivavano solo rimettendo l'app in primo piano — cioe'
 * quando non servono piu'. AlarmManager e' l'unico modo di farsi risvegliare a tempo senza
 * tenere in piedi un servizio in primo piano per tutta la sessione.
 *
 * L'allarme e' un doppione del tick in-app, non il suo sostituto: chi dei due arriva primo chiama
 * [RestTimerController.finish], che agisce una volta sola.
 */
class RestAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(triggerAtMs: Long) {
        val manager = alarmManager ?: return
        val intent = pendingIntent()
        // setExactAndAllowWhileIdle attraversa il doze; senza il permesso di allarme esatto
        // (revocabile su Android 12 e 13) si ripiega su un allarme inesatto, che arriva comunque
        // con lo schermo acceso — il caso normale di chi sta guardando il telefono in palestra.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, intent)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, intent)
        }
    }

    fun cancel() {
        alarmManager?.cancel(pendingIntent())
    }

    // Un solo allarme vivo alla volta: lo stesso PendingIntent (stesso request code, stessa
    // action) viene riusato, quindi programmarne un altro sostituisce il precedente.
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

/** Riceve la sveglia e chiude il recupero: e' il punto in cui suona e vibra ad app chiusa. */
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
