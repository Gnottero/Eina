package com.eina.app.domain

import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Session summary, rebuilt from the completed sets. */
data class SessionSummary(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    /** Name of the routine followed, unless the session was a free workout. */
    val routineName: String? = null,
    val exerciseNames: List<ExerciseName>,
    val setCount: Int,
    val totalReps: Int,
    val volumeKg: Double,
    val prCount: Int,
    /**
     * Primary muscle worked by most of the session's exercises, used only to colour the dot on the
     * history card. Null when the session says nothing about it.
     */
    val dominantMuscle: String? = null
) {
    val durationMinutes: Long?
        get() = endTime?.let { (it - startTime) / 60_000L }
}

/** A personal record, to be rendered according to its weight type. */
data class PrRecord(
    val exerciseId: Long,
    val exerciseName: ExerciseName,
    val weightType: WeightType,
    val achievedAt: Long,
    val weight: Double?,
    val reps: Int?,
    val bodyweightSnapshotKg: Double?
)

fun epochMillisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

private fun volumeOf(row: CompletedSetRow): Double = volumeForSet(
    row.weightType,
    SetEntryEntity(
        workoutExerciseId = 0,
        setIndex = row.setIndex,
        actualReps = row.actualReps,
        weight = row.weight,
        restSecondsPlanned = 0,
        setType = row.setType,
        bodyweightSnapshotKg = row.bodyweightSnapshotKg
    ),
    row.bodyweightFactor
)

/** Volume in kg of a list of sets; warmups do not count towards the total lifted. */
fun totalVolume(rows: List<CompletedSetRow>): Double =
    rows.filter { it.setType.countsAsWorking }.sumOf { volumeOf(it) }

/** Session summaries, most recent first. */
fun summarizeSessions(rows: List<CompletedSetRow>): List<SessionSummary> =
    rows.groupBy { it.sessionId }
        .map { (sessionId, sessionRows) ->
            val working = sessionRows.filter { it.setType.countsAsWorking }
            SessionSummary(
                sessionId = sessionId,
                startTime = sessionRows.first().sessionStart,
                endTime = sessionRows.first().sessionEnd,
                routineName = sessionRows.first().routineName,
                // An exercise repeated in the same session appears twice: they are two distinct
                // work blocks, not a duplicate to collapse.
                exerciseNames = sessionRows
                    .sortedBy { it.exerciseOrder }
                    .groupBy { it.workoutExerciseId }
                    .values
                    .map { it.first().exerciseName },
                setCount = working.size,
                totalReps = working.sumOf { it.actualReps ?: 0 },
                volumeKg = working.sumOf { volumeOf(it) },
                prCount = working.count { it.isPR },
                // Counted per exercise and not per set: an exercise carried through eight sets
                // would otherwise outweigh three different movements.
                dominantMuscle = sessionRows
                    .groupBy { it.workoutExerciseId }
                    .values
                    .mapNotNull { it.first().muscleGroupsPrimary.firstOrNull() }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
            )
        }
        .sortedByDescending { it.startTime }

/** Volume per calendar day, keyed by the local date of the session. */
fun volumeByDay(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, Double> =
    rows.filter { it.setType.countsAsWorking }
        .groupBy { epochMillisToLocalDate(it.sessionStart, zone) }
        .mapValues { (_, dayRows) -> dayRows.sumOf { volumeOf(it) } }

/** Number of completed working sets per day. */
fun setsByDay(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, Int> =
    rows.filter { it.setType.countsAsWorking }
        .groupBy { epochMillisToLocalDate(it.sessionStart, zone) }
        .mapValues { (_, dayRows) -> dayRows.size }

/** Days with at least one completed set. */
fun trainingDays(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Set<LocalDate> = rows.map { epochMillisToLocalDate(it.sessionStart, zone) }.toSet()

/**
 * Streak: consecutive weeks with at least one workout, up to the current week (or the previous
 * one, so the streak does not reset before the week is over).
 * DECISIONE: weekly rather than daily — training every day is not the goal, consistency is
 * measured over the week.
 */
fun currentStreak(days: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    if (days.isEmpty()) return 0
    val weeks = days.map { startOfWeek(it) }.toSet()
    val thisWeek = startOfWeek(today)
    var cursor = when {
        weeks.contains(thisWeek) -> thisWeek
        weeks.contains(thisWeek.minusWeeks(1)) -> thisWeek.minusWeeks(1)
        else -> return 0
    }
    var streak = 0
    while (weeks.contains(cursor)) {
        streak++
        cursor = cursor.minusWeeks(1)
    }
    return streak
}

/** Monday of the week the date belongs to. */
private fun startOfWeek(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())

/**
 * Latest PR per exercise, most recent first. Sets flagged isPR were already validated by
 * [isNewPR] when saved, so only the most recent one per exercise is kept.
 */
fun personalRecords(rows: List<CompletedSetRow>): List<PrRecord> =
    rows.filter { it.isPR && it.setType.countsAsWorking }
        .groupBy { it.exerciseId }
        .map { (_, prRows) ->
            val best = prRows.maxBy { it.completedAt }
            PrRecord(
                exerciseId = best.exerciseId,
                exerciseName = best.exerciseName,
                weightType = best.weightType,
                achievedAt = best.completedAt,
                weight = best.weight,
                reps = best.actualReps,
                bodyweightSnapshotKg = best.bodyweightSnapshotKg
            )
        }
        .sortedByDescending { it.achievedAt }
