package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Riga per l'anteprima di una routine nell'elenco: nome dell'esercizio in scheda. */
data class RoutineExercisePreviewRow(
    val routineId: Long,
    @Embedded val exerciseName: ExerciseName,
    // Primary muscles: the routine row is opened by a square tinted with the group the routine
    // works most, so a list of names is scannable by colour.
    val muscleGroupsPrimary: List<String> = emptyList()
)

@Dao
interface RoutineExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routineExercise: RoutineExerciseEntity): Long

    @Update
    suspend fun update(routineExercise: RoutineExerciseEntity)

    @Delete
    suspend fun delete(routineExercise: RoutineExerciseEntity)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY `order` ASC")
    fun getForRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>

    /**
     * The same rows, read once. Callers that only read the routine (session start, export,
     * comparison with a workout) should not register an InvalidationTracker observer just to
     * unregister it immediately.
     */
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY `order` ASC")
    suspend fun getForRoutineOnce(routineId: Long): List<RoutineExerciseEntity>

    @Query(
        """
        SELECT re.routineId AS routineId,
               e.name AS nameEn, e.nameIt AS nameIt, e.nameFr AS nameFr,
               e.muscleGroupsPrimary AS muscleGroupsPrimary
        FROM routine_exercises re
        INNER JOIN exercises e ON re.exerciseId = e.id
        ORDER BY re.routineId ASC, re.`order` ASC
        """
    )
    fun observeAllPreviews(): Flow<List<RoutineExercisePreviewRow>>
}
