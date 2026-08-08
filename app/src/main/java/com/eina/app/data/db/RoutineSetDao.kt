package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: RoutineSetEntity): Long

    @Update
    suspend fun update(set: RoutineSetEntity)

    @Delete
    suspend fun delete(set: RoutineSetEntity)

    @Query("SELECT * FROM routine_sets WHERE routineExerciseId = :routineExerciseId ORDER BY setIndex ASC")
    suspend fun getForRoutineExercise(routineExerciseId: Long): List<RoutineSetEntity>

    /** Tutte le serie della routine in un colpo solo: l'editor le raggruppa per esercizio. */
    @Query(
        """
        SELECT rs.* FROM routine_sets rs
        INNER JOIN routine_exercises re ON rs.routineExerciseId = re.id
        WHERE re.routineId = :routineId
        ORDER BY re.`order` ASC, rs.setIndex ASC
        """
    )
    fun observeForRoutine(routineId: Long): Flow<List<RoutineSetEntity>>

    /** Serie pianificate per routine: alimenta il contatore nelle card dell'elenco. */
    @Query(
        """
        SELECT re.routineId AS routineId, COUNT(rs.id) AS setCount
        FROM routine_exercises re
        LEFT JOIN routine_sets rs ON rs.routineExerciseId = re.id
        GROUP BY re.routineId
        """
    )
    fun observeSetCounts(): Flow<List<RoutineSetCountRow>>
}

data class RoutineSetCountRow(val routineId: Long, val setCount: Int)
