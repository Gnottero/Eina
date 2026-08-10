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

    /**
     * Elimina le sessioni chiuse senza nemmeno una serie svolta: righe che lo storico non disegna
     * (si basa sulle serie completate) ma che restavano nel database, invisibili e non
     * cancellabili da nessuna schermata.
     *
     * `endTime IS NOT NULL` tiene fuori l'allenamento in corso, che di serie svolte non ne ha
     * finche' non se ne chiude la prima. Ritorna quante ne ha tolte.
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
     * Svuota lo storico. Esercizi e serie spariscono con le sessioni (ON DELETE CASCADE),
     * routine, libreria e peso corporeo restano.
     */
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()
}
