package com.eina.app.domain

import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Riepilogo di una sessione, ricostruito dalle set completate. */
data class SessionSummary(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    /** Nome della routine seguita, se la sessione non era un allenamento libero. */
    val routineName: String? = null,
    val exerciseNames: List<ExerciseName>,
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

/** Volume in kg di una lista di set (le warmup non contano nel totale sollevato). */
fun totalVolume(rows: List<CompletedSetRow>): Double =
    rows.filter { it.setType.countsAsWorking }.sumOf { volumeOf(it) }

/** Riepiloghi di sessione ordinati dal piu' recente. */
fun summarizeSessions(rows: List<CompletedSetRow>): List<SessionSummary> =
    rows.groupBy { it.sessionId }
        .map { (sessionId, sessionRows) ->
            val working = sessionRows.filter { it.setType.countsAsWorking }
            SessionSummary(
                sessionId = sessionId,
                startTime = sessionRows.first().sessionStart,
                endTime = sessionRows.first().sessionEnd,
                routineName = sessionRows.first().routineName,
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
    rows.filter { it.setType.countsAsWorking }
        .groupBy { epochMillisToLocalDate(it.sessionStart, zone) }
        .mapValues { (_, dayRows) -> dayRows.sumOf { volumeOf(it) } }

/** Numero di set completate (non warmup) per giorno. */
fun setsByDay(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, Int> =
    rows.filter { it.setType.countsAsWorking }
        .groupBy { epochMillisToLocalDate(it.sessionStart, zone) }
        .mapValues { (_, dayRows) -> dayRows.size }

/** Giorni in cui almeno una set e' stata completata. */
fun trainingDays(
    rows: List<CompletedSetRow>,
    zone: ZoneId = ZoneId.systemDefault()
): Set<LocalDate> = rows.map { epochMillisToLocalDate(it.sessionStart, zone) }.toSet()

/**
 * Streak = settimane consecutive con almeno un allenamento, fino alla settimana in corso (o a
 * quella scorsa, cosi' lo streak non si azzera prima che la settimana sia finita).
 * DECISIONE: unita' settimanale invece che giornaliera — allenarsi ogni giorno non e' l'obiettivo,
 * la costanza si misura sulla settimana.
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

/** Lunedi' della settimana a cui appartiene la data. */
private fun startOfWeek(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())

/**
 * Ultimo PR per esercizio, dal piu' recente. Le set marcate isPR sono gia' state validate da
 * [isNewPR] al momento del salvataggio: qui si tiene solo la piu' recente per esercizio.
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
