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
        WHERE we.exerciseId = :exerciseId AND se.setType != 'WARMUP' AND se.completedAt IS NOT NULL
        """
    )
    suspend fun getHistoricalSets(exerciseId: Long): List<SetEntryEntity>

    /**
     * Tutte le serie completate di un esercizio, riscaldamenti compresi, in ordine di
     * completamento: e' l'ingresso di [com.eina.app.domain.recomputePrFlags], che deve poter
     * togliere il record anche a una serie diventata riscaldamento.
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

    // "Ultima volta": set della piu' recente WorkoutSession (per startTime, escludendo la sessione corrente) che contiene l'esercizio.
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

    // Ultimo peso e ultime ripetizioni effettivamente registrati per un esercizio, in qualunque
    // sessione. Servono come ultimo segnaposto quando l'allenamento piu' recente con quell'esercizio
    // non aveva il dato (es. serie chiusa senza peso): meglio proporre l'ultimo valore noto che
    // lasciare il campo vuoto.
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
