package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Riga denormalizzata: una set completata con il contesto di sessione ed esercizio.
 * DECISIONE: statistiche (volume, PR, heatmap, storico) si calcolano in Kotlin su questa
 * singola query invece che con N query SQL aggregate — dataset locale piccolo, e la logica
 * di volume dipende dal weightType, gia' scritta in domain/PrCalculator.
 */
data class CompletedSetRow(
    val sessionId: Long,
    val sessionStart: Long,
    val sessionEnd: Long?,
    // Identifica la voce di sessione, non l'esercizio di libreria: lo stesso esercizio ripetuto
    // due volte nello stesso allenamento resta cosi' due blocchi distinti, non uno solo.
    val workoutExerciseId: Long = 0,
    val exerciseOrder: Int = 0,
    // Nome della routine da cui e' partita la sessione: null per un allenamento libero.
    val routineName: String? = null,
    val exerciseId: Long,
    @Embedded val exerciseName: ExerciseName,
    val weightType: WeightType,
    val bodyweightFactor: Double,
    val setIndex: Int,
    val actualReps: Int?,
    val weight: Double?,
    val bodyweightSnapshotKg: Double?,
    val setType: SetType,
    val isPR: Boolean,
    val completedAt: Long
)

@Dao
interface StatsDao {
    @Query(
        """
        SELECT ws.id AS sessionId, ws.startTime AS sessionStart, ws.endTime AS sessionEnd,
               we.id AS workoutExerciseId, we.`order` AS exerciseOrder,
               r.name AS routineName,
               e.id AS exerciseId, e.weightType AS weightType,
               e.bodyweightFactor AS bodyweightFactor,
               e.name AS nameEn, e.nameIt AS nameIt, e.nameFr AS nameFr,
               se.setIndex AS setIndex, se.actualReps AS actualReps, se.weight AS weight,
               se.bodyweightSnapshotKg AS bodyweightSnapshotKg, se.setType AS setType,
               se.isPR AS isPR, se.completedAt AS completedAt
        FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        INNER JOIN workout_sessions ws ON we.sessionId = ws.id
        INNER JOIN exercises e ON we.exerciseId = e.id
        LEFT JOIN routines r ON ws.routineId = r.id
        WHERE se.completedAt IS NOT NULL
        ORDER BY ws.startTime DESC, we.`order` ASC, se.setIndex ASC
        """
    )
    fun observeCompletedSets(): Flow<List<CompletedSetRow>>

    @Query(
        """
        SELECT ws.id AS sessionId, ws.startTime AS sessionStart, ws.endTime AS sessionEnd,
               we.id AS workoutExerciseId, we.`order` AS exerciseOrder,
               r.name AS routineName,
               e.id AS exerciseId, e.weightType AS weightType,
               e.bodyweightFactor AS bodyweightFactor,
               e.name AS nameEn, e.nameIt AS nameIt, e.nameFr AS nameFr,
               se.setIndex AS setIndex, se.actualReps AS actualReps, se.weight AS weight,
               se.bodyweightSnapshotKg AS bodyweightSnapshotKg, se.setType AS setType,
               se.isPR AS isPR, se.completedAt AS completedAt
        FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        INNER JOIN workout_sessions ws ON we.sessionId = ws.id
        INNER JOIN exercises e ON we.exerciseId = e.id
        LEFT JOIN routines r ON ws.routineId = r.id
        WHERE se.completedAt IS NOT NULL AND ws.id = :sessionId
        ORDER BY we.`order` ASC, se.setIndex ASC
        """
    )
    fun observeCompletedSetsForSession(sessionId: Long): Flow<List<CompletedSetRow>>

    /** Storico di un solo esercizio: alimenta il grafico di progressione nella sua scheda. */
    @Query(
        """
        SELECT ws.id AS sessionId, ws.startTime AS sessionStart, ws.endTime AS sessionEnd,
               we.id AS workoutExerciseId, we.`order` AS exerciseOrder,
               r.name AS routineName,
               e.id AS exerciseId, e.weightType AS weightType,
               e.bodyweightFactor AS bodyweightFactor,
               e.name AS nameEn, e.nameIt AS nameIt, e.nameFr AS nameFr,
               se.setIndex AS setIndex, se.actualReps AS actualReps, se.weight AS weight,
               se.bodyweightSnapshotKg AS bodyweightSnapshotKg, se.setType AS setType,
               se.isPR AS isPR, se.completedAt AS completedAt
        FROM set_entries se
        INNER JOIN workout_exercises we ON se.workoutExerciseId = we.id
        INNER JOIN workout_sessions ws ON we.sessionId = ws.id
        INNER JOIN exercises e ON we.exerciseId = e.id
        LEFT JOIN routines r ON ws.routineId = r.id
        WHERE se.completedAt IS NOT NULL AND e.id = :exerciseId
        ORDER BY ws.startTime ASC, se.setIndex ASC
        """
    )
    fun observeCompletedSetsForExercise(exerciseId: Long): Flow<List<CompletedSetRow>>
}
