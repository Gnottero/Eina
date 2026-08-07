package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutExercise: WorkoutExerciseEntity): Long

    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getForSession(sessionId: Long): Flow<List<WorkoutExerciseEntity>>
}
