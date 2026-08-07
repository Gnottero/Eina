package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setEntry: SetEntryEntity): Long

    @Update
    suspend fun update(setEntry: SetEntryEntity)

    @Query("SELECT * FROM set_entries WHERE workoutExerciseId = :workoutExerciseId ORDER BY setIndex ASC")
    fun getForWorkoutExercise(workoutExerciseId: Long): Flow<List<SetEntryEntity>>

    // Tutte le set non-warmup completate per un esercizio, usate da isNewPR/volumeForSet.
    @Query(
        """
        SELECT se.* FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND se.isWarmup = 0 AND se.completedAt IS NOT NULL
        """
    )
    suspend fun getHistoricalSets(exerciseId: Long): List<SetEntryEntity>

    // "Ultima volta": set della piu' recente WorkoutSession (per startTime) che contiene l'esercizio.
    @Query(
        """
        SELECT se.* FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId
        AND we.sessionId = (
            SELECT we2.sessionId FROM workout_exercises we2
            INNER JOIN workout_sessions ws2 ON we2.sessionId = ws2.id
            WHERE we2.exerciseId = :exerciseId
            ORDER BY ws2.startTime DESC
            LIMIT 1
        )
        ORDER BY se.setIndex ASC
        """
    )
    fun getLastTimeSets(exerciseId: Long): Flow<List<SetEntryEntity>>
}
