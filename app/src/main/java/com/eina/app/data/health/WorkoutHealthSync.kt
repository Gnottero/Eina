package com.eina.app.data.health

import com.eina.app.data.db.WorkoutSessionDao
import com.eina.app.data.db.encodeHeartRateSamples
import com.eina.app.data.prefs.SettingsRepository

/**
 * Attacca alla sessione appena chiusa quel che l'orologio ha misurato nella stessa finestra.
 *
 * Si chiama due volte: a fine allenamento, e riaprendo il riepilogo. La seconda serve perche' un
 * orologio sincronizza con comodo — i battiti dell'ultima serie possono arrivare in Health
 * Connect qualche minuto dopo il "Termina".
 */
class WorkoutHealthSync(
    private val sessionDao: WorkoutSessionDao,
    private val source: HealthConnectSource,
    private val settings: SettingsRepository
) {
    val isAvailable: Boolean get() = source.isAvailable

    val permissions: Set<String> get() = source.permissions

    suspend fun hasPermissions(): Boolean = source.hasPermissions()

    /**
     * Ritorna true se la sessione ha guadagnato qualcosa. False quando l'integrazione e' spenta,
     * la sessione e' ancora aperta, o l'orologio non ha niente per quella finestra.
     */
    suspend fun sync(sessionId: Long): Boolean {
        if (!settings.healthSyncEnabled.value) return false
        val session = sessionDao.getById(sessionId) ?: return false
        val endTime = session.endTime ?: return false
        val vitals = source.readWorkoutVitals(session.startTime, endTime)
        if (vitals == null) {
            // Niente da leggere. Se pero' la sessione porta ancora i dati di una lettura
            // precedente, quelli vanno tolti: succede quando l'allenamento viene corretto e la
            // finestra si sposta dove l'orologio non ha misurato niente. Solo col permesso in
            // mano, altrimenti si cancellerebbe per il motivo sbagliato.
            val stale = session.avgHeartRateBpm != null || session.maxHeartRateBpm != null ||
                session.caloriesKcal != null || session.heartRateSamples != null
            if (!stale || !source.hasPermissions()) return false
            sessionDao.update(
                session.copy(
                    avgHeartRateBpm = null,
                    maxHeartRateBpm = null,
                    caloriesKcal = null,
                    heartRateSamples = null
                )
            )
            return true
        }
        // Quel che si legge adesso sostituisce quel che c'era, campo per campo, invece di
        // tenere il vecchio dove il nuovo e' nullo: se l'allenamento e' stato corretto (data,
        // ora, durata) la finestra non e' piu' la stessa, e i battiti di prima erano di
        // un'altra mezz'ora. Una lettura che non trova niente non arriva fin qui.
        val updated = session.copy(
            avgHeartRateBpm = vitals.avgBpm,
            maxHeartRateBpm = vitals.maxBpm,
            caloriesKcal = vitals.kcal,
            heartRateSamples = vitals.samples.encodeHeartRateSamples()
        )
        if (updated == session) return false
        sessionDao.update(updated)
        return true
    }
}
