package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    /** Una sessione osservata: il riepilogo aggiorna i dati dell'orologio appena arrivano. */
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<WorkoutSessionEntity>>

    /**
     * Still-open session: endTime stays null until the workout is finished. Leaving the screen
     * closes nothing, so at most one session can be running and it is found here on return.
     */
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActive(): WorkoutSessionEntity?

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Deletes closed sessions without a single completed set: rows the history never draws (it is
     * built from completed sets) but which stayed in the database, invisible and undeletable.
     *
     * `endTime IS NOT NULL` keeps the running workout out, since it has no completed set until the
     * first one is closed. Returns how many rows were removed.
     */
    @Query(
        """
        DELETE FROM workout_sessions
        WHERE endTime IS NOT NULL AND id NOT IN (
            SELECT we.sessionId FROM set_entries se
            INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
            WHERE se.completedAt IS NOT NULL
        )
        """
    )
    suspend fun deleteEmptySessions(): Int

    /**
     * Empties the history. Exercises and sets go with the sessions (ON DELETE CASCADE); routines,
     * library and bodyweight stay.
     */
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()
}
