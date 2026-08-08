package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Update
    suspend fun updateAll(exercises: List<ExerciseEntity>)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    /** Ricerca per nome inglese: e' la chiave con cui viaggiano le routine esportate. */
    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    /** Esercizi di libreria (non custom): quelli che il seeder puo' riscrivere. */
    @Query("SELECT * FROM exercises WHERE isCustom = 0")
    suspend fun getLibraryExercises(): List<ExerciseEntity>

    /**
     * Toglie dalla libreria gli esercizi usciti dal catalogo curato, ma solo se non li
     * usa nessuno: uno rimasto dentro una routine o dentro un allenamento gia' registrato
     * resta al suo posto, altrimenti la foreign key salterebbe e lo storico perderebbe
     * il nome dell'esercizio.
     */
    @Query(
        """
        DELETE FROM exercises
        WHERE isCustom = 0
          AND name IN (:names)
          AND id NOT IN (SELECT exerciseId FROM routine_exercises)
          AND id NOT IN (SELECT exerciseId FROM workout_exercises)
        """
    )
    suspend fun deleteUnusedLibraryExercises(names: List<String>): Int
}
