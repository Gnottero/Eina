package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Riga per l'anteprima di una routine nell'elenco: nome esercizio e serie pianificate. */
data class RoutineExercisePreviewRow(
    val routineId: Long,
    val exerciseName: String,
    val targetSets: Int
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

    @Query(
        """
        SELECT re.routineId AS routineId, e.name AS exerciseName, re.targetSets AS targetSets
        FROM routine_exercises re
        INNER JOIN exercises e ON re.exerciseId = e.id
        ORDER BY re.routineId ASC, re.`order` ASC
        """
    )
    fun observeAllPreviews(): Flow<List<RoutineExercisePreviewRow>>
}
