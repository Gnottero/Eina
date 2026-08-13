package com.eina.app.domain

import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.SetType
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.WeightType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private val zone: ZoneId = ZoneId.systemDefault()

private fun millisOf(date: LocalDate, hour: Int = 10): Long =
    date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

private fun row(
    sessionId: Long,
    date: LocalDate,
    exerciseId: Long = 1,
    exerciseName: String = "Panca piana",
    weightType: WeightType = WeightType.FREE_WEIGHT,
    setIndex: Int = 0,
    reps: Int? = 10,
    weight: Double? = 50.0,
    bodyweight: Double? = null,
    bodyweightFactor: Double = 1.0,
    setType: SetType = SetType.NORMAL,
    isPR: Boolean = false,
    endHour: Int? = 11,
    workoutExerciseId: Long = exerciseId
) = CompletedSetRow(
    sessionId = sessionId,
    sessionStart = millisOf(date),
    sessionEnd = endHour?.let { millisOf(date, it) },
    workoutExerciseId = workoutExerciseId,
    exerciseOrder = workoutExerciseId.toInt(),
    exerciseId = exerciseId,
    exerciseName = ExerciseName(exerciseName),
    weightType = weightType,
    bodyweightFactor = bodyweightFactor,
    setIndex = setIndex,
    actualReps = reps,
    weight = weight,
    bodyweightSnapshotKg = bodyweight,
    setType = setType,
    isPR = isPR,
    completedAt = millisOf(date, 10) + setIndex * 60_000L
)

class StatsTest {

    private val today: LocalDate = LocalDate.of(2026, 3, 10)

    @Test
    fun `totalVolume esclude le warmup`() {
        val rows = listOf(
            row(1, today, setIndex = 0, reps = 10, weight = 50.0),
            row(1, today, setIndex = 1, reps = 8, weight = 60.0),
            row(1, today, setIndex = 2, reps = 15, weight = 20.0, setType = SetType.WARMUP)
        )
        assertEquals(980.0, totalVolume(rows), 0.001)
    }

    @Test
    fun `cedimento e drop set contano come serie di lavoro`() {
        val rows = listOf(
            row(1, today, setIndex = 0, reps = 10, weight = 50.0, setType = SetType.FAILURE),
            row(1, today, setIndex = 1, reps = 8, weight = 30.0, setType = SetType.DROP),
            row(1, today, setIndex = 2, reps = 15, weight = 20.0, setType = SetType.WARMUP)
        )
        assertEquals(740.0, totalVolume(rows), 0.001)
        assertEquals(2, summarizeSessions(rows).first().setCount)
    }

    @Test
    fun `summarizeSessions aggrega per sessione e ordina dal piu recente`() {
        val rows = listOf(
            row(2, today, setIndex = 0, reps = 5, weight = 100.0, isPR = true),
            row(1, today.minusDays(3), setIndex = 0, reps = 10, weight = 50.0),
            row(1, today.minusDays(3), setIndex = 1, reps = 10, weight = 50.0, exerciseId = 2, exerciseName = "Squat")
        )
        val summaries = summarizeSessions(rows)

        assertEquals(listOf(2L, 1L), summaries.map { it.sessionId })
        val recent = summaries.first()
        assertEquals(500.0, recent.volumeKg, 0.001)
        assertEquals(1, recent.prCount)
        assertEquals(60L, recent.durationMinutes)

        val older = summaries.last()
        assertEquals(2, older.setCount)
        assertEquals(20, older.totalReps)
        assertEquals(listOf("Panca piana", "Squat"), older.exerciseNames.map { it.nameEn })
    }

    @Test
    fun `lo stesso esercizio ripetuto nella sessione resta due voci`() {
        val rows = listOf(
            row(1, today, exerciseId = 1, workoutExerciseId = 10, setIndex = 0),
            row(1, today, exerciseId = 2, exerciseName = "Squat", workoutExerciseId = 11, setIndex = 0),
            row(1, today, exerciseId = 1, workoutExerciseId = 12, setIndex = 0)
        )
        val summary = summarizeSessions(rows).single()

        assertEquals(
            listOf("Panca piana", "Squat", "Panca piana"),
            summary.exerciseNames.map { it.nameEn }
        )
    }

    @Test
    fun `volumeByDay usa la data locale della sessione`() {
        val rows = listOf(
            row(1, today, reps = 10, weight = 50.0),
            row(2, today.minusDays(1), reps = 10, weight = 30.0)
        )
        val byDay = volumeByDay(rows, zone)

        assertEquals(500.0, byDay.getValue(today), 0.001)
        assertEquals(300.0, byDay.getValue(today.minusDays(1)), 0.001)
    }

    @Test
    fun `currentStreak conta le settimane consecutive fino a questa`() {
        // today = Tuesday 10/3/2026: one session per week is enough to keep the streak.
        val days = setOf(today, today.minusWeeks(1), today.minusWeeks(2), today.minusWeeks(6))
        assertEquals(3, currentStreak(days, today))
    }

    @Test
    fun `piu allenamenti nella stessa settimana valgono una settimana sola`() {
        // today = Tuesday: Monday the 9th and Tuesday the 10th are in the same week.
        val days = setOf(today, today.minusDays(1))
        assertEquals(1, currentStreak(days, today))
    }

    @Test
    fun `currentStreak parte dalla settimana scorsa se questa e ancora vuota`() {
        val days = setOf(today.minusWeeks(1), today.minusWeeks(2))
        assertEquals(2, currentStreak(days, today))
    }

    @Test
    fun `currentStreak azzerato se l ultimo allenamento e piu vecchio della settimana scorsa`() {
        assertEquals(0, currentStreak(setOf(today.minusWeeks(2)), today))
        assertEquals(0, currentStreak(emptySet(), today))
    }

    @Test
    fun `personalRecords tiene solo il PR piu recente per esercizio`() {
        val rows = listOf(
            row(3, today, exerciseId = 1, weight = 100.0, isPR = true),
            row(2, today.minusDays(7), exerciseId = 1, weight = 90.0, isPR = true),
            row(1, today.minusDays(9), exerciseId = 2, exerciseName = "Trazioni",
                weightType = WeightType.BODYWEIGHT, reps = 12, weight = null, bodyweight = 70.0, isPR = true),
            row(1, today.minusDays(9), exerciseId = 3, exerciseName = "Rematore", isPR = false)
        )
        val records = personalRecords(rows)

        assertEquals(2, records.size)
        assertEquals(1L, records.first().exerciseId)
        assertEquals(100.0, records.first().weight!!, 0.001)
        assertEquals(WeightType.BODYWEIGHT, records.last().weightType)
        assertEquals(12, records.last().reps)
    }

    @Test
    fun `volume a corpo libero scala col fattore dell'esercizio`() {
        val rows = listOf(
            // Pull-up: the whole body rises, so it counts.
            row(1, today, weightType = WeightType.BODYWEIGHT, reps = 10, weight = null, bodyweight = 72.0),
            // Crunch: the body does not rise, factor 0, so it never inflates the total.
            row(1, today, weightType = WeightType.BODYWEIGHT, setIndex = 1, reps = 30,
                weight = null, bodyweight = 72.0, bodyweightFactor = 0.0),
            row(1, today, weightType = WeightType.BODYWEIGHT_PLUS_LOAD, setIndex = 2,
                reps = 5, weight = 10.0, bodyweight = 72.0)
        )
        assertEquals(720.0 + 0.0 + 410.0, totalVolume(rows), 0.001)
    }
}

class ExerciseProgressTest {

    @Test
    fun `one point per session, taken from the best set`() {
        val rows = listOf(
            row(sessionId = 1, date = LocalDate.of(2026, 1, 5), weight = 60.0, reps = 8),
            row(sessionId = 1, date = LocalDate.of(2026, 1, 5), setIndex = 1, weight = 70.0, reps = 5),
            row(sessionId = 2, date = LocalDate.of(2026, 1, 12), weight = 75.0, reps = 5)
        )
        val points = exerciseProgress(rows)
        assertEquals(2, points.size)
        assertEquals(70.0, points[0].weight!!, 0.0)
        assertEquals(5, points[0].reps)
        assertEquals(75.0, points[1].weight!!, 0.0)
    }

    @Test
    fun `warmup sets stay out of the progression`() {
        val rows = listOf(
            row(sessionId = 1, date = LocalDate.of(2026, 1, 5), weight = 100.0, setType = SetType.WARMUP),
            row(sessionId = 1, date = LocalDate.of(2026, 1, 5), setIndex = 1, weight = 60.0)
        )
        assertEquals(60.0, exerciseProgress(rows).single().weight!!, 0.0)
    }

    @Test
    fun `on distance the faster run wins over the slower one`() {
        val rows = listOf(
            row(sessionId = 1, date = LocalDate.of(2026, 1, 5), weightType = WeightType.DISTANCE_BASED, weight = 5.0, reps = 40),
            row(sessionId = 1, date = LocalDate.of(2026, 1, 5), setIndex = 1, weightType = WeightType.DISTANCE_BASED, weight = 5.0, reps = 28)
        )
        assertEquals(28, exerciseProgress(rows).single().reps)
    }
}
