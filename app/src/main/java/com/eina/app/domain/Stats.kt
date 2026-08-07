package com.eina.app.domain

import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Riepilogo di una sessione, ricostruito dalle set completate. */
data class SessionSummary(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    val exerciseNames: List<String>,
    val setCount: Int,
    val totalReps: Int,
    val volumeKg: Double,
    val prCount: Int
) {
    val durationMinutes: Long?
        get() = endTime?.let { (it - startTime) / 60_000L }
}

/** Un record personale, con il valore gia' formattato secondo il weightType. */
data class PrRecord(
    val exerciseId: Long,
    val exerciseName: String,
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
        isWarmup = row.isWarmup,
        bodyweightSnapshotKg = row.bodyweightSnapshotKg
    )
)

/** Volume in kg di una lista di set (le warmup non contano nel totale sollevato). */
fun totalVolume(rows: List<CompletedSetRow>): Double =
    rows.filter { !it.isWarmup }.sumOf { volumeOf(it) }

/** Riepiloghi di sessione ordinati dal piu' recente. */
fun summarizeSessions(rows: List<CompletedSetRow>): List<SessionSummary> =
    rows.groupBy { it.sessionId }
        .map { (sessionId, sessionRows) ->
            val working = sessionRows.filter { !it.isWarmup }
            SessionSummary(
                sessionId = sessionId,
                startTime = sessionRows.first().sessionStart,
                endTime = sessionRows.first().sessionEnd,
                // Un esercizio ripetuto nella stessa sessione compare due volte: sono due blocchi
                // di lavoro distinti, non un duplicato da collassare.
                exerciseNames = sessionRows
                    .sortedBy { it.exerciseOrder }
                    .groupBy { it.workoutExerciseId }
                    .values
                    .map { it.first().exerciseName },
                setCount = working.size,
                totalReps = working.sumOf { it.actualReps ?: 0 },
                volumeKg = working.sumOf { volumeOf(it) },
                prCount = working.count { it.isPR }
            )
        }
        .sortedByDescending { it.startTime }

/** Volume per giorno di calendario (chiave = data locale della sessione). */
fun volumeByDay(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, Double> =
    rows.filter { !it.isWarmup }
        .groupBy { epochMillisToLocalDate(it.sessionStart, zone) }
        .mapValues { (_, dayRows) -> dayRows.sumOf { volumeOf(it) } }

/** Numero di set completate (non warmup) per giorno. */
fun setsByDay(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, Int> =
    rows.filter { !it.isWarmup }
        .groupBy { epochMillisToLocalDate(it.sessionStart, zone) }
        .mapValues { (_, dayRows) -> dayRows.size }

/** Giorni in cui almeno una set e' stata completata. */
fun trainingDays(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Set<LocalDate> = rows.map { epochMillisToLocalDate(it.sessionStart, zone) }.toSet()

/**
 * Streak = giorni consecutivi di allenamento che arrivano fino a oggi (o a ieri, cosi' la
 * striscia non si azzera prima che la giornata sia finita).
 */
fun currentStreak(days: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    if (days.isEmpty()) return 0
    var cursor = when {
        days.contains(today) -> today
        days.contains(today.minusDays(1)) -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    while (days.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * Ultimo PR per esercizio, dal piu' recente. Le set marcate isPR sono gia' state validate da
 * [isNewPR] al momento del salvataggio: qui si tiene solo la piu' recente per esercizio.
 */
fun personalRecords(rows: List<CompletedSetRow>): List<PrRecord> =
    rows.filter { it.isPR && !it.isWarmup }
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
