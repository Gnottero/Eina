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

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<WorkoutSessionEntity>>

    /**
     * Sessione ancora aperta: endTime resta null finche' l'utente non preme "Termina".
     * Uscire dalla schermata non chiude nulla, quindi puo' esistere al massimo una sessione
     * in corso e la si ritrova qui al rientro nell'app.
     */
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActive(): WorkoutSessionEntity?

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
