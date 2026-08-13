package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SetEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setEntry: SetEntryEntity): Long

    @Update
    suspend fun update(setEntry: SetEntryEntity)

    // One-shot read and not a Flow: no caller observes the sets of a single exercise. A Flow read
    // once registers and unregisters an InvalidationTracker observer for nothing, and that
    // happened inside per-exercise loops.
    @Query("SELECT * FROM set_entries WHERE workoutExerciseId = :workoutExerciseId ORDER BY setIndex ASC")
    suspend fun getForWorkoutExercise(workoutExerciseId: Long): List<SetEntryEntity>

    /** Whether the session has at least one completed set, which is what makes it a workout. */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM set_entries se
            INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
            WHERE we.sessionId = :sessionId AND se.completedAt IS NOT NULL
        )
        """
    )
    suspend fun sessionHasCompletedSets(sessionId: Long): Boolean

    // Every completed non-warmup set of an exercise, used by isNewPR and volumeForSet.
    @Query(
        """
        SELECT se.* FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND se.setType != 'WARMUP' AND se.completedAt IS NOT NULL
        """
    )
    suspend fun getHistoricalSets(exerciseId: Long): List<SetEntryEntity>

    /**
     * Every completed set of an exercise, warmups included, in completion order: the input of
     * [com.eina.app.domain.recomputePrFlags], which must be able to strip the record from a set
     * that became a warmup.
     */
    @Query(
        """
        SELECT se.* FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND se.completedAt IS NOT NULL
        ORDER BY se.completedAt ASC
        """
    )
    suspend fun getCompletedSetsForExercise(exerciseId: Long): List<SetEntryEntity>

    // "Last time": sets of the most recent session containing the exercise, by startTime and
    // excluding the current one.
    @Query(
        """
        SELECT se.* FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId
        AND we.sessionId = (
            SELECT we2.sessionId FROM workout_exercises we2
            INNER JOIN workout_sessions ws2 ON we2.sessionId = ws2.id
            WHERE we2.exerciseId = :exerciseId AND we2.sessionId != :excludeSessionId
            ORDER BY ws2.startTime DESC
            LIMIT 1
        )
        ORDER BY se.setIndex ASC
        """
    )
    suspend fun getLastTimeSets(exerciseId: Long, excludeSessionId: Long): List<SetEntryEntity>

    // Last weight and reps actually recorded for an exercise, in any session. They are the final
    // placeholder when the most recent workout with that exercise did not record the value.
    @Query(
        """
        SELECT se.weight FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND se.completedAt IS NOT NULL AND se.weight IS NOT NULL
        ORDER BY se.completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastRecordedWeight(exerciseId: Long): Double?

    @Query(
        """
        SELECT se.actualReps FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND se.completedAt IS NOT NULL AND se.actualReps IS NOT NULL
        ORDER BY se.completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastRecordedReps(exerciseId: Long): Int?

    @Query("DELETE FROM set_entries WHERE id = :setId")
    suspend fun deleteById(setId: Long)

    @Query("SELECT * FROM set_entries WHERE id = :setId")
    suspend fun getById(setId: Long): SetEntryEntity?
}
