package com.eina.app.data.health

import com.eina.app.data.db.WorkoutSessionDao
import com.eina.app.data.db.encodeHeartRateSamples
import com.eina.app.data.prefs.SettingsRepository

/**
 * Attaches to a closed session what the watch measured over the same window.
 *
 * Called twice: when the workout ends and again when the summary is opened, because a watch syncs
 * at its own pace and the last samples can reach Health Connect minutes later.
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
     * Returns true if the session gained something. False when the integration is off, the session
     * is still open, or the watch has nothing for that window.
     */
    suspend fun sync(sessionId: Long): Boolean {
        if (!settings.healthSyncEnabled.value) return false
        val session = sessionDao.getById(sessionId) ?: return false
        val endTime = session.endTime ?: return false
        val vitals = source.readWorkoutVitals(session.startTime, endTime)
        if (vitals == null) {
            // Nothing to read. Data from an earlier read must be dropped, which happens when the
            // workout is corrected and its window moves where the watch measured nothing. Only
            // with the permission granted, or the deletion would happen for the wrong reason.
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
        // The new reading replaces the old one field by field instead of keeping previous values
        // where the new ones are null: if the workout was corrected, the window is no longer the
        // same and the old samples belonged to another half hour.
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
