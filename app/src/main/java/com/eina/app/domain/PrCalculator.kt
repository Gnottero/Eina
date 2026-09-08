package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType

/**
 * Whether the set beats everything recorded before it for the same exercise.
 *
 * The set type does not matter: a heavier bar is a heavier bar, and calling it a warmup does not
 * make it lighter. Marking a set as a warmup after lifting a record used to erase the record.
 */
fun isNewPR(
    weightType: WeightType,
    newSet: SetEntryEntity,
    historicalSets: List<SetEntryEntity> // every completed set of the same exerciseId
): Boolean {
    return when (weightType) {
        WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK, WeightType.ASSISTED ->
            (newSet.weight ?: 0.0) > (historicalSets.maxOfOrNull { it.weight ?: 0.0 } ?: 0.0)

        WeightType.BODYWEIGHT ->
            (newSet.actualReps ?: 0) > (historicalSets.maxOfOrNull { it.actualReps ?: 0 } ?: 0)

        WeightType.BODYWEIGHT_PLUS_LOAD -> {
            val newLoad = (newSet.bodyweightSnapshotKg ?: 0.0) + (newSet.weight ?: 0.0)
            val maxHistLoad = historicalSets.maxOfOrNull { (it.bodyweightSnapshotKg ?: 0.0) + (it.weight ?: 0.0) } ?: 0.0
            newLoad > maxHistLoad
        }

        WeightType.TIME_BASED ->
            (newSet.actualReps ?: 0) > (historicalSets.maxOfOrNull { it.actualReps ?: 0 } ?: 0) // actualReps holds the duration

        // Distance and time together: running further is a record, and so is running the same
        // distance faster, so beating either the maximum distance or the maximum average speed
        // makes the set a PR.
        WeightType.DISTANCE_BASED -> {
            val newDistance = newSet.weight ?: 0.0
            if (newDistance <= 0.0) false
            else {
                val bestDistance = historicalSets.maxOfOrNull { it.weight ?: 0.0 } ?: 0.0
                val bestSpeed = historicalSets.maxOfOrNull { speedKmPerHour(it) } ?: 0.0
                newDistance > bestDistance || speedKmPerHour(newSet) > bestSpeed
            }
        }
    }
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
