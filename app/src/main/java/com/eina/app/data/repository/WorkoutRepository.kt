package com.eina.app.data.repository

import com.eina.app.data.db.BodyMetricDao
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineExerciseEntity
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
    private val bodyMetricDao: BodyMetricDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineDao: RoutineDao
) {
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    suspend fun insertExercise(exercise: ExerciseEntity): Long = exerciseDao.insert(exercise)

    suspend fun updateExercise(exercise: ExerciseEntity) = exerciseDao.update(exercise)

    suspend fun deleteExercise(exercise: ExerciseEntity) = exerciseDao.delete(exercise)

    /** Sessione ancora aperta, se esiste: alimenta il banner "Riprendi" e blocca nuovi avvii. */
    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeActive()

    /**
     * Avvia un allenamento libero. Se ce n'e' gia' uno in corso non ne crea un secondo:
     * ritorna quello aperto, cosi' il chiamante ci rientra invece di duplicarlo.
     */
    suspend fun startSession(): Long {
        workoutSessionDao.getActive()?.let { return it.id }
        return workoutSessionDao.insert(WorkoutSessionEntity(startTime = System.currentTimeMillis()))
    }

    /**
     * Crea sessione legata alla routine e prepara esercizi/set vuoti.
     * I valori della routine restano *target* (targetReps, recupero pianificato): non vengono
     * scritti in `weight`/`actualReps`, cosi' una serie appena creata non risulta gia' svolta.
     */
    suspend fun startSessionFromRoutine(routineId: Long): Long {
        workoutSessionDao.getActive()?.let { return it.id }
        val sessionId = workoutSessionDao.insert(
            WorkoutSessionEntity(routineId = routineId, startTime = System.currentTimeMillis())
        )
        val routineExercises = routineExerciseDao.getForRoutine(routineId).first().sortedBy { it.order }
        routineExercises.forEachIndexed { index, routineExercise ->
            val workoutExerciseId = workoutExerciseDao.insert(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = routineExercise.exerciseId,
                    order = index,
                    // La nota della routine parte come nota della sessione: modificarla durante
                    // l'allenamento non deve riscrivere il template.
                    notes = routineExercise.notes
                )
            )
            repeat(routineExercise.targetSets) { setIndex ->
                setEntryDao.insert(
                    SetEntryEntity(
                        workoutExerciseId = workoutExerciseId,
                        setIndex = setIndex,
                        targetReps = routineExercise.targetReps,
                        restSecondsPlanned = routineExercise.restSeconds
                    )
                )
            }
        }
        return sessionId
    }

    suspend fun finishSession(sessionId: Long) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(session.copy(endTime = System.currentTimeMillis()))
    }

    /**
     * Annulla la sessione: la riga sparisce e con lei, per cascade, esercizi e serie registrate.
     * Serve per l'allenamento aperto per sbaglio, che altrimenti resterebbe nello storico.
     */
    suspend fun cancelSession(sessionId: Long) {
        workoutSessionDao.deleteById(sessionId)
    }

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = workoutSessionDao.getById(sessionId)

    /** Routine di partenza della sessione: serve il link playlist durante l'allenamento. */
    suspend fun getRoutine(routineId: Long): RoutineEntity? = routineDao.getById(routineId)

    /** Target della routine per exerciseId: servono alla UI come segnaposto, non come valori registrati. */
    suspend fun getRoutineTargets(routineId: Long): Map<Long, RoutineExerciseEntity> =
        routineExerciseDao.getForRoutine(routineId).first().associateBy { it.exerciseId }

    suspend fun getSessionExercises(sessionId: Long): List<WorkoutExerciseEntity> =
        workoutExerciseDao.getForSessionOnce(sessionId)

    suspend fun getSetsForWorkoutExercise(workoutExerciseId: Long): List<SetEntryEntity> =
        setEntryDao.getForWorkoutExercise(workoutExerciseId).first()

    suspend fun getLastTimeSets(exerciseId: Long, currentSessionId: Long): List<SetEntryEntity> =
        setEntryDao.getLastTimeSets(exerciseId, currentSessionId)

    /** Ultimi peso/ripetizioni registrati per l'esercizio, in qualunque sessione. */
    suspend fun getLastRecordedValues(exerciseId: Long): Pair<Double?, Int?> =
        setEntryDao.getLastRecordedWeight(exerciseId) to setEntryDao.getLastRecordedReps(exerciseId)

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

    suspend fun setExerciseNotes(workoutExerciseId: Long, notes: String?) {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return
        workoutExerciseDao.update(current.copy(notes = notes))
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
