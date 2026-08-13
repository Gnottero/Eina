package com.eina.app.data.repository

import com.eina.app.data.db.BodyMetricDao
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineSetDao
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.SetEntryDao
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.WorkoutExerciseDao
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.db.WorkoutSessionDao
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.domain.PlanItem
import com.eina.app.domain.RoutineChange
import com.eina.app.domain.routineChanges
import com.eina.app.domain.isNewPR
import com.eina.app.domain.recomputePrFlags
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val bodyMetricDao: BodyMetricDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineSetDao: RoutineSetDao,
    private val routineDao: RoutineDao
) {
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    suspend fun insertExercise(exercise: ExerciseEntity): Long = exerciseDao.insert(exercise)

    /**
     * Rewrites a custom exercise. Custom only: the seeder would overwrite a library one on the
     * next launch. The id is preserved, so routines and history keep pointing at the same row.
     */
    suspend fun updateCustomExercise(exercise: ExerciseEntity): Boolean {
        if (!exercise.isCustom) return false
        exerciseDao.update(exercise)
        return true
    }

    /**
     * Deletes a custom exercise if nothing uses it; returns false when it still appears in a
     * routine or in a recorded workout. Library exercises are never deleted: the seeder would
     * bring them back anyway.
     */
    suspend fun deleteCustomExercise(exercise: ExerciseEntity): Boolean {
        if (!exercise.isCustom) return false
        if (exerciseDao.countUsages(exercise.id) > 0) return false
        exerciseDao.delete(exercise)
        return true
    }

    /** Still-open session, if any: feeds the resume banner and blocks starting a second one. */
    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeActive()

    /** Starts a free workout, or returns the session already open instead of creating a second. */
    suspend fun startSession(): Long {
        workoutSessionDao.getActive()?.let { return it.id }
        return workoutSessionDao.insert(WorkoutSessionEntity(startTime = System.currentTimeMillis()))
    }

    /**
     * Creates a session bound to a routine, with empty exercises and sets. Routine values stay
     * targets (targetReps, planned rest) and are not written to `weight`/`actualReps`, so a fresh
     * set is not reported as already done.
     */
    suspend fun startSessionFromRoutine(routineId: Long): Long {
        workoutSessionDao.getActive()?.let { return it.id }
        val sessionId = workoutSessionDao.insert(
            WorkoutSessionEntity(routineId = routineId, startTime = System.currentTimeMillis())
        )
        val routineExercises = routineExerciseDao.getForRoutineOnce(routineId)
        routineExercises.forEachIndexed { index, routineExercise ->
            val workoutExerciseId = workoutExerciseDao.insert(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = routineExercise.exerciseId,
                    order = index,
                    restSeconds = routineExercise.restSeconds,
                    // Note and superset are copied from the routine: editing them during the
                    // workout must not rewrite the template.
                    notes = routineExercise.notes,
                    supersetGroup = routineExercise.supersetGroup
                )
            )
            // One session set per routine set, type included.
            routineSetDao.getForRoutineExercise(routineExercise.id).forEachIndexed { setIndex, routineSet ->
                setEntryDao.insert(
                    SetEntryEntity(
                        workoutExerciseId = workoutExerciseId,
                        setIndex = setIndex,
                        targetReps = routineSet.targetReps,
                        restSecondsPlanned = routineExercise.restSeconds,
                        setType = routineSet.setType
                    )
                )
            }
        }
        return sessionId
    }

    /**
     * Closes the session. [startTime] and [endTime] come from the finish sheet, where they can be
     * corrected, so a workout done yesterday lands in yesterday's history.
     *
     * A session without a single completed set is deleted instead of saved, and `false` is
     * returned. The criterion is the completed set and not the listed exercise, because the
     * history is drawn from completed sets: otherwise a session filled with exercises but never
     * performed would linger as an invisible row.
     */
    suspend fun finishSession(
        sessionId: Long,
        startTime: Long? = null,
        endTime: Long? = null
    ): Boolean {
        val session = workoutSessionDao.getById(sessionId) ?: return false
        if (!hasCompletedSets(sessionId)) {
            workoutSessionDao.deleteById(sessionId)
            return false
        }
        val start = startTime ?: session.startTime
        workoutSessionDao.update(
            session.copy(
                startTime = start,
                endTime = endTime ?: System.currentTimeMillis()
            )
        )
        return true
    }

    /** Cancels the session: the row is deleted and, by cascade, its exercises and sets. */
    suspend fun cancelSession(sessionId: Long) {
        workoutSessionDao.deleteById(sessionId)
    }

    /**
     * Deletes a recorded workout and its sets. PR flags of the exercises it contained are
     * recomputed: if the all-time maximum was in that session, the record goes back to the
     * previous holder.
     */
    suspend fun deleteSession(sessionId: Long) {
        val exerciseIds = exerciseIdsOfSession(sessionId)
        workoutSessionDao.deleteById(sessionId)
        recomputePrs(exerciseIds)
    }

    /** Corrects date and duration of a recorded workout, as chosen in the finish sheet. */
    suspend fun updateSessionTimes(sessionId: Long, startTime: Long, durationSeconds: Int) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(
            session.copy(
                startTime = startTime,
                endTime = startTime + durationSeconds.coerceAtLeast(0) * 1000L
            )
        )
    }

    /**
     * Recomputes PR flags of the given exercises across the whole history: editing a past workout
     * can create or void records in sessions other than the one touched
     * (see [com.eina.app.domain.recomputePrFlags]).
     */
    suspend fun recomputePrs(exerciseIds: Collection<Long>) {
        exerciseIds.distinct().forEach { exerciseId ->
            val weightType = exerciseDao.getById(exerciseId)?.weightType ?: return@forEach
            recomputePrFlags(weightType, setEntryDao.getCompletedSetsForExercise(exerciseId))
                .forEach { setEntryDao.update(it) }
        }
    }

    /** Exercises touched by a session; the list to pass to [recomputePrs] after editing it. */
    suspend fun exerciseIdsOfSession(sessionId: Long): List<Long> =
        workoutExerciseDao.getForSessionOnce(sessionId).map { it.exerciseId }

    /** Whether the session has at least one completed set, in a single EXISTS query. */
    suspend fun hasCompletedSets(sessionId: Long): Boolean =
        setEntryDao.sessionHasCompletedSets(sessionId)

    /**
     * Removes closed sessions without a single completed set: the history is drawn from completed
     * sets, so these are invisible rows no screen can delete. Runs at app start and after editing
     * a past workout, the two points where such a row can appear.
     *
     * The live workout is untouched: its `endTime` stays null until it is finished. Returns how
     * many rows were deleted.
     */
    suspend fun purgeEmptySessions(): Int = workoutSessionDao.deleteEmptySessions()

    /** Deletes the whole history, live session included. Irreversible. */
    suspend fun deleteAllSessions() {
        workoutSessionDao.deleteAll()
    }

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = workoutSessionDao.getById(sessionId)

    /** Observed session: the summary refreshes when the watch data arrives. */
    fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeById(sessionId)

    /** Source routine of the session; the playlist link is read from it during the workout. */
    suspend fun getRoutine(routineId: Long): RoutineEntity? = routineDao.getById(routineId)

    /**
     * Routine targets by exerciseId, used as placeholders and never as recorded values. Weight and
     * reps come from the first working set of the routine: sets carry their own values, but a set
     * added by hand in the gym needs a single number to suggest.
     */
    suspend fun getRoutineTargets(routineId: Long): Map<Long, RoutineTarget> =
        routineExerciseDao.getForRoutineOnce(routineId).associate { routineExercise ->
            val sets = routineSetDao.getForRoutineExercise(routineExercise.id)
            val reference = sets.firstOrNull { it.setType.countsAsWorking } ?: sets.firstOrNull()
            routineExercise.exerciseId to RoutineTarget(
                restSeconds = routineExercise.restSeconds,
                targetReps = reference?.targetReps,
                targetWeight = reference?.targetWeight
            )
        }

    /**
     * The routine reduced to what is compared against the performed workout; see
     * [com.eina.app.domain.routineChanges].
     */
    suspend fun routinePlan(routineId: Long): List<PlanItem> =
        routineExerciseDao.getForRoutineOnce(routineId).mapNotNull { routineExercise ->
            val exercise = exerciseDao.getById(routineExercise.exerciseId) ?: return@mapNotNull null
            PlanItem(
                exerciseId = routineExercise.exerciseId,
                name = exercise.exerciseName(),
                setCount = routineSetDao.getForRoutineExercise(routineExercise.id).size,
                restSeconds = routineExercise.restSeconds
            )
        }

    /** What changed between routine and workout; empty when the routine was followed exactly. */
    suspend fun routineChangesFor(sessionId: Long, routineId: Long): List<RoutineChange> =
        routineChanges(routinePlan(routineId), sessionPlan(sessionId))

    /** The performed workout in the same shape as the routine, so the two can be compared. */
    suspend fun sessionPlan(sessionId: Long): List<PlanItem> =
        workoutExerciseDao.getForSessionOnce(sessionId).mapNotNull { workoutExercise ->
            val exercise = exerciseDao.getById(workoutExercise.exerciseId) ?: return@mapNotNull null
            val sets = setEntryDao.getForWorkoutExercise(workoutExercise.id)
            PlanItem(
                exerciseId = workoutExercise.exerciseId,
                name = exercise.exerciseName(),
                setCount = sets.size,
                restSeconds = workoutExercise.restSeconds
            )
        }

    /**
     * Rewrites the routine from the workout: exercises, order, supersets, notes, rest and sets
     * (count and type). Targets become the recorded values, so the next workout starts from there.
     *
     * Rows are rebuilt from scratch rather than merged: the differences can be of any kind and an
     * incremental merge would have more cases than rows.
     */
    suspend fun applySessionToRoutine(sessionId: Long, routineId: Long) {
        val sessionExercises = workoutExerciseDao.getForSessionOnce(sessionId)
        // The routine_exercises cascade takes its routine_sets with it.
        routineExerciseDao.getForRoutineOnce(routineId).forEach { routineExerciseDao.delete(it) }
        sessionExercises.forEachIndexed { index, workoutExercise ->
            val sets = setEntryDao.getForWorkoutExercise(workoutExercise.id)
            val routineExerciseId = routineExerciseDao.insert(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = workoutExercise.exerciseId,
                    order = index,
                    restSeconds = workoutExercise.restSeconds,
                    notes = workoutExercise.notes,
                    supersetGroup = workoutExercise.supersetGroup
                )
            )
            sets.forEachIndexed { setIndex, set ->
                routineSetDao.insert(
                    RoutineSetEntity(
                        routineExerciseId = routineExerciseId,
                        setIndex = setIndex,
                        // An unfinished set must not wipe the target it had.
                        targetReps = set.actualReps ?: set.targetReps,
                        targetWeight = set.weight,
                        setType = set.setType
                    )
                )
            }
        }
    }

    /**
     * Turns a performed workout into a new routine. The work is done by [applySessionToRoutine],
     * which here starts from a freshly created routine, so its initial delete finds nothing.
     */
    suspend fun createRoutineFromSession(sessionId: Long, name: String): Long? {
        if (workoutExerciseDao.getForSessionOnce(sessionId).isEmpty()) return null
        val routineId = routineDao.insert(RoutineEntity(name = name))
        applySessionToRoutine(sessionId, routineId)
        return routineId
    }

    suspend fun getSessionExercises(sessionId: Long): List<WorkoutExerciseEntity> =
        workoutExerciseDao.getForSessionOnce(sessionId)

    suspend fun getSetsForWorkoutExercise(workoutExerciseId: Long): List<SetEntryEntity> =
        setEntryDao.getForWorkoutExercise(workoutExerciseId)

    suspend fun getLastTimeSets(exerciseId: Long, currentSessionId: Long): List<SetEntryEntity> =
        setEntryDao.getLastTimeSets(exerciseId, currentSessionId)

    /** Last recorded weight and reps for the exercise, in any session. */
    suspend fun getLastRecordedValues(exerciseId: Long): Pair<Double?, Int?> =
        setEntryDao.getLastRecordedWeight(exerciseId) to setEntryDao.getLastRecordedReps(exerciseId)

    suspend fun addExercise(sessionId: Long, exerciseId: Long, order: Int, defaultRestSeconds: Int = 90): Long {
        val workoutExerciseId = workoutExerciseDao.insert(
            WorkoutExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                order = order,
                restSeconds = defaultRestSeconds
            )
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

    /**
     * Swaps the movement of a workout row in place, keeping order, superset and notes.
     *
     * The sets survive as a structure (how many, of which type, with which rest) but the recorded
     * values are cleared: weight and reps belonged to another exercise and would otherwise count
     * towards the volume and progression of the new one. Any assigned PR is dropped for the same
     * reason.
     *
     * Returns false if the row does not exist or already holds that exercise.
     */
    suspend fun replaceExercise(workoutExerciseId: Long, newExerciseId: Long): Boolean {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return false
        if (current.exerciseId == newExerciseId) return false
        workoutExerciseDao.update(current.copy(exerciseId = newExerciseId))
        // Sets are recreated instead of cleared in place: the table fields keep the typed text as
        // long as a set has the same id, so clearing only the database would leave the numbers of
        // the old exercise on screen, ready to be confirmed.
        setEntryDao.getForWorkoutExercise(workoutExerciseId).forEach { set ->
            setEntryDao.deleteById(set.id)
            setEntryDao.insert(
                set.copy(
                    id = 0,
                    actualReps = null,
                    weight = null,
                    completedAt = null,
                    isPR = false,
                    bodyweightSnapshotKg = null
                )
            )
        }
        return true
    }

    suspend fun setExerciseNotes(workoutExerciseId: Long, notes: String?) {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return
        workoutExerciseDao.update(current.copy(notes = notes))
    }

    /** Rest of a session exercise; see [WorkoutExerciseEntity.restSeconds]. */
    suspend fun setExerciseRestSeconds(workoutExerciseId: Long, restSeconds: Int) {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return
        if (current.restSeconds == restSeconds) return
        workoutExerciseDao.update(current.copy(restSeconds = restSeconds))
    }

    suspend fun removeExercise(workoutExerciseId: Long) {
        workoutExerciseDao.deleteById(workoutExerciseId)
    }

    /**
     * Rewrites the session exercise list: the position in the list is the `order` field, and
     * supersets and notes travel with it.
     *
     * The comparison must be against the stored row and not the incoming `order`: the caller
     * builds the rows already numbered, so comparing them with their own index always matched and
     * nothing was ever written.
     */
    suspend fun reorderExercises(orderedWorkoutExercises: List<WorkoutExerciseEntity>) {
        // Stored rows are read in one go: a getById per row meant N queries to find out, almost
        // always, that there was nothing to rewrite.
        val sessionId = orderedWorkoutExercises.firstOrNull()?.sessionId ?: return
        val stored = workoutExerciseDao.getForSessionOnce(sessionId).associateBy { it.id }
        orderedWorkoutExercises.forEachIndexed { index, workoutExercise ->
            val updated = workoutExercise.copy(order = index)
            if (stored[workoutExercise.id] != updated) {
                workoutExerciseDao.update(updated)
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
     * Completes a set: stores the bodyweight snapshot for the weight types that need it, computes
     * `isPR` against the non-warmup history and persists the row, returning the updated set.
     *
     * [completedAt] is passed when the set is not being closed now: while editing a workout from
     * last month, "now" would push it to the top of the history and make it the last known value
     * for that exercise.
     */
    suspend fun completeSet(
        set: SetEntryEntity,
        exerciseId: Long,
        weightType: WeightType,
        completedAt: Long = System.currentTimeMillis()
    ): SetEntryEntity {
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
            completedAt = completedAt,
            bodyweightSnapshotKg = bodyweightSnapshotKg
        )
        val isPR = isNewPR(weightType, candidate, historicalSets)
        val finalSet = candidate.copy(isPR = isPR)
        setEntryDao.update(finalSet)
        return finalSet
    }
}

/** Routine values suggested in a session: exercise rest and targets of the first working set. */
data class RoutineTarget(
    val restSeconds: Int,
    val targetReps: Int?,
    val targetWeight: Double?
)
