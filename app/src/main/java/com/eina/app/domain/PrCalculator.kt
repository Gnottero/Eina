package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType

/**
 * Whether the set beats everything recorded before it for the same exercise.
 *
 * The set type does not matter: a heavier bar is a heavier bar, and calling it a warmup does not
 * make it lighter. Marking a set as a warmup after lifting a record used to erase the record.
 *
 * A set with no repetitions is not a lift, though: loading 200 kg and racking the bar without
 * moving it is not a record, so a set whose [SetEntryEntity.actualReps] is null or zero can never
 * be a PR, and it is not part of the history the next set has to beat either.
 */
fun isNewPR(
    weightType: WeightType,
    newSet: SetEntryEntity,
    historicalSets: List<SetEntryEntity> // every completed set of the same exerciseId
): Boolean {
    if (!isPerformed(weightType, newSet)) return false
    val history = historicalSets.filter { isPerformed(weightType, it) }

    return when (weightType) {
        WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK, WeightType.ASSISTED ->
            (newSet.weight ?: 0.0) > (history.maxOfOrNull { it.weight ?: 0.0 } ?: 0.0)

        WeightType.BODYWEIGHT ->
            (newSet.actualReps ?: 0) > (history.maxOfOrNull { it.actualReps ?: 0 } ?: 0)

        WeightType.BODYWEIGHT_PLUS_LOAD -> {
            val newLoad = (newSet.bodyweightSnapshotKg ?: 0.0) + (newSet.weight ?: 0.0)
            val maxHistLoad = history.maxOfOrNull { (it.bodyweightSnapshotKg ?: 0.0) + (it.weight ?: 0.0) } ?: 0.0
            newLoad > maxHistLoad
        }

        WeightType.TIME_BASED ->
            (newSet.actualReps ?: 0) > (history.maxOfOrNull { it.actualReps ?: 0 } ?: 0) // actualReps holds the duration

        // Distance and time together: running further is a record, and so is running the same
        // distance faster, so beating either the maximum distance or the maximum average speed
        // makes the set a PR.
        WeightType.DISTANCE_BASED -> {
            val newDistance = newSet.weight ?: 0.0
            val bestDistance = history.maxOfOrNull { it.weight ?: 0.0 } ?: 0.0
            val bestSpeed = history.maxOfOrNull { speedKmPerHour(it) } ?: 0.0
            newDistance > bestDistance || speedKmPerHour(newSet) > bestSpeed
        }
    }
}

/**
 * Whether the set records work actually done, and so can hold a record and cap the next one.
 *
 * What makes a set real depends on what it measures: repetitions for a load, seconds for a hold,
 * kilometres for a machine that covers distance. Zero of that measure means the set was closed
 * without being performed - a weight typed in and never lifted - and it must stay out of the
 * records in both directions.
 */
private fun isPerformed(weightType: WeightType, set: SetEntryEntity): Boolean = when (weightType) {
    WeightType.DISTANCE_BASED -> (set.weight ?: 0.0) > 0.0
    else -> (set.actualReps ?: 0) > 0
}

/**
 * Average speed of a distance set, in km/h: `weight` holds kilometres and `actualReps` minutes
 * (see [WeightType.DISTANCE_BASED]). With no time recorded there is no speed to compare and the
 * result is 0.
 */
fun speedKmPerHour(set: SetEntryEntity): Double {
    val minutes = set.actualReps ?: 0
    if (minutes <= 0) return 0.0
    return (set.weight ?: 0.0) / (minutes / 60.0)
}

/**
 * Volume in kg of a set.
 *
 * Bodyweight only counts for the share the exercise actually lifts
 * ([com.eina.app.data.db.ExerciseEntity.bodyweightFactor]): pull-ups raise the whole body and enter
 * the total, crunches raise nothing against gravity and count as 0 — otherwise sit-ups alone would
 * inflate the session volume in proportion to the user's weight.
 */
fun volumeForSet(weightType: WeightType, set: SetEntryEntity, bodyweightFactor: Double = 1.0): Double {
    val reps = set.actualReps ?: 0
    val liftedBodyweight = (set.bodyweightSnapshotKg ?: 0.0) * bodyweightFactor
    return when (weightType) {
        WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK ->
            (set.weight ?: 0.0) * reps
        WeightType.BODYWEIGHT ->
            liftedBodyweight * reps
        WeightType.BODYWEIGHT_PLUS_LOAD ->
            (liftedBodyweight + (set.weight ?: 0.0)) * reps
        WeightType.ASSISTED ->
            (liftedBodyweight - (set.weight ?: 0.0)).coerceAtLeast(0.0) * reps
        WeightType.TIME_BASED, WeightType.DISTANCE_BASED ->
            0.0 // time and distance are not measured in kg: excluded from the lifted total
    }
}
