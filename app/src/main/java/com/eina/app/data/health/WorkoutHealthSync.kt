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
        val vitals = source.readWorkoutVitals(session.startTime, endTime) ?: return false
        val updated = session.copy(
            avgHeartRateBpm = vitals.avgBpm ?: session.avgHeartRateBpm,
            maxHeartRateBpm = vitals.maxBpm ?: session.maxHeartRateBpm,
            caloriesKcal = vitals.kcal ?: session.caloriesKcal,
            heartRateSamples = vitals.samples.encodeHeartRateSamples() ?: session.heartRateSamples
        )
        if (updated == session) return false
        sessionDao.update(updated)
        return true
    }
}
