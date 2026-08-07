package com.eina.app.data.repository

import com.eina.app.data.db.BodyMetricDao
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.SetEntryDao
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.WorkoutExerciseDao
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.db.WorkoutSessionDao
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.domain.isNewPR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WorkoutRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val bodyMetricDao: BodyMetricDao
) {
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    suspend fun startSession(): Long =
        workoutSessionDao.insert(WorkoutSessionEntity(startTime = System.currentTimeMillis()))

    suspend fun finishSession(sessionId: Long) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(session.copy(endTime = System.currentTimeMillis()))
    }

    suspend fun getSessionExercises(sessionId: Long): List<WorkoutExerciseEntity> =
        workoutExerciseDao.getForSessionOnce(sessionId)

    suspend fun getSetsForWorkoutExercise(workoutExerciseId: Long): List<SetEntryEntity> =
        setEntryDao.getForWorkoutExercise(workoutExerciseId).first()

    suspend fun getLastTimeSets(exerciseId: Long, currentSessionId: Long): List<SetEntryEntity> =
        setEntryDao.getLastTimeSets(exerciseId, currentSessionId)

    suspend fun addExercise(sessionId: Long, exerciseId: Long, order: Int, defaultRestSeconds: Int = 90): Long {
        val workoutExerciseId = workoutExerciseDao.insert(
            WorkoutExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, order = order)
        )
        setEntryDao.insert(
            SetEntryEntity(
                workoutExerciseId = workoutExerciseId,
                setIndex = 0,
                restSecondsPlanned = defaultRestSeconds
            )
        )
        return workoutExerciseId
    }

    suspend fun removeExercise(workoutExerciseId: Long) {
        workoutExerciseDao.deleteById(workoutExerciseId)
    }

    suspend fun reorderExercises(orderedWorkoutExercises: List<WorkoutExerciseEntity>) {
        orderedWorkoutExercises.forEachIndexed { index, workoutExercise ->
            if (workoutExercise.order != index) {
                workoutExerciseDao.update(workoutExercise.copy(order = index))
            }
        }
    }

    suspend fun addSet(workoutExerciseId: Long, setIndex: Int, restSecondsPlanned: Int): Long =
        setEntryDao.insert(
            SetEntryEntity(
                workoutExerciseId = workoutExerciseId,
                setIndex = setIndex,
                restSecondsPlanned = restSecondsPlanned
            )
        )

    suspend fun removeSet(setId: Long) {
        setEntryDao.deleteById(setId)
    }

    suspend fun updateSet(set: SetEntryEntity) {
        setEntryDao.update(set)
    }

    /**
     * Completa una set: applica bodyweight snapshot per i weightType che lo richiedono, calcola isPR
     * confrontando con lo storico non-warmup, persiste. Ritorna la set aggiornata.
     */
    suspend fun completeSet(set: SetEntryEntity, exerciseId: Long, weightType: WeightType): SetEntryEntity {
        val needsBodyweightSnapshot = weightType == WeightType.BODYWEIGHT ||
            weightType == WeightType.BODYWEIGHT_PLUS_LOAD ||
            weightType == WeightType.ASSISTED

        val bodyweightSnapshotKg = if (needsBodyweightSnapshot) {
            set.bodyweightSnapshotKg ?: bodyMetricDao.getLatest()?.bodyweightKg
        } else {
            null
        }

        val historicalSets = setEntryDao.getHistoricalSets(exerciseId)
        val candidate = set.copy(
            completedAt = System.currentTimeMillis(),
            bodyweightSnapshotKg = bodyweightSnapshotKg
        )
        val isPR = isNewPR(weightType, candidate, historicalSets)
        val finalSet = candidate.copy(isPR = isPR)
        setEntryDao.update(finalSet)
        return finalSet
    }
}
