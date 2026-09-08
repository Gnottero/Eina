package com.eina.app.domain

import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType

/**
 * One progression point: the best set of a workout, dated on that day. `weight` holds kilograms (or
 * kilometres for distance exercises) and `reps` holds repetitions, seconds or minutes depending on
 * the [WeightType] — the same two columns as the set table.
 */
data class ProgressPoint(
    val date: Long,
    val weight: Double?,
    val reps: Int?
)

/**
 * Progression of an exercise over time, oldest to newest: one point per workout, not per set.
 *
 * DECISIONE: the point is the best set of the session, not the average. An average drops as soon as
 * a light set is appended, showing a decline where more work was actually done. "Best" is the
 * highest load where a load exists (assisted exercises read as they do for PRs, highest number
 * wins) and the highest reps where no load is typed. Warmups take part: a set that turned out to be
 * the best of the day is the best of the day whatever it was called.
 */
fun exerciseProgress(rows: List<CompletedSetRow>): List<ProgressPoint> =
    rows.groupBy { it.sessionId }
        .mapNotNull { (_, sessionRows) ->
            val best = bestSet(sessionRows) ?: return@mapNotNull null
            ProgressPoint(
                date = sessionRows.first().sessionStart,
                weight = best.weight,
                reps = best.actualReps
            )
        }
        .sortedBy { it.date }

private fun bestSet(rows: List<CompletedSetRow>): CompletedSetRow? = when (rows.first().weightType) {
    WeightType.FREE_WEIGHT,
    WeightType.MACHINE_STACK,
    WeightType.ASSISTED,
    WeightType.BODYWEIGHT_PLUS_LOAD ->
        // On equal load the set with more reps wins: it went better.
        rows.maxWithOrNull(compareBy({ it.weight ?: 0.0 }, { it.actualReps ?: 0 }))

    // On equal distance the faster run wins, not the longer one: speed is compared, as for PRs.
    WeightType.DISTANCE_BASED ->
        rows.maxWithOrNull(compareBy({ it.weight ?: 0.0 }, { it.speedKmPerHour() }))

    WeightType.BODYWEIGHT, WeightType.TIME_BASED ->
        rows.maxByOrNull { it.actualReps ?: 0 }
}

/** Km/h of a distance set: `weight` holds kilometres and `actualReps` minutes. */
private fun CompletedSetRow.speedKmPerHour(): Double {
    val minutes = actualReps ?: 0
    if (minutes <= 0) return 0.0
    return (weight ?: 0.0) / (minutes / 60.0)
}
