package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkoutExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutExercise: WorkoutExerciseEntity): Long

    @Update
    suspend fun update(workoutExercise: WorkoutExerciseEntity)

    @Query("SELECT * FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun getById(workoutExerciseId: Long): WorkoutExerciseEntity?

    @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun deleteById(workoutExerciseId: Long)

    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY `order` ASC")
    suspend fun getForSessionOnce(sessionId: Long): List<WorkoutExerciseEntity>
}
