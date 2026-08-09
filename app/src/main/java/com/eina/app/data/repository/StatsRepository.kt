package com.eina.app.data.repository

import com.eina.app.data.db.BodyMetricDao
import com.eina.app.data.db.BodyMetricEntity
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.StatsDao
import kotlinx.coroutines.flow.Flow

class StatsRepository(
    private val statsDao: StatsDao,
    private val bodyMetricDao: BodyMetricDao
) {
    fun observeCompletedSets(): Flow<List<CompletedSetRow>> = statsDao.observeCompletedSets()

    fun observeSessionSets(sessionId: Long): Flow<List<CompletedSetRow>> =
        statsDao.observeCompletedSetsForSession(sessionId)

    fun observeExerciseSets(exerciseId: Long): Flow<List<CompletedSetRow>> =
        statsDao.observeCompletedSetsForExercise(exerciseId)

    fun observeBodyMetrics(): Flow<List<BodyMetricEntity>> = bodyMetricDao.getAll()

    /** Registra il peso di oggi: alimenta bodyweightSnapshotKg delle set completate da qui in poi. */
    suspend fun addBodyMetric(bodyweightKg: Double, date: Long = System.currentTimeMillis()): Long =
        bodyMetricDao.insert(BodyMetricEntity(date = date, bodyweightKg = bodyweightKg))

    suspend fun deleteBodyMetric(id: Long) = bodyMetricDao.deleteById(id)
}
