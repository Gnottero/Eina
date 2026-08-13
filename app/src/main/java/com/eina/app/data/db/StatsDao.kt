package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Denormalised row: a completed set with its session and exercise context.
 * DECISIONE: statistics (volume, PRs, heatmap, history) are computed in Kotlin over this single
 * query instead of N aggregate SQL queries — the local dataset is small and the volume logic
 * depends on weightType, already written in domain/PrCalculator.
 */
data class CompletedSetRow(
    val sessionId: Long,
    val sessionStart: Long,
    val sessionEnd: Long?,
    // Identifies the session row and not the library exercise, so the same exercise performed
    // twice in one workout stays two distinct blocks.
    val workoutExerciseId: Long = 0,
    val exerciseOrder: Int = 0,
    // Superset group: the summary uses it to colour the rounds as the workout screen does.
    val supersetGroup: Int? = null,
    // Name of the routine the session started from; null for a free workout.
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
               we.supersetGroup AS supersetGroup,
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
               we.supersetGroup AS supersetGroup,
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

    /** History of a single exercise, feeding the progression chart on its detail screen. */
    @Query(
        """
        SELECT ws.id AS sessionId, ws.startTime AS sessionStart, ws.endTime AS sessionEnd,
               we.id AS workoutExerciseId, we.`order` AS exerciseOrder,
               we.supersetGroup AS supersetGroup,
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
