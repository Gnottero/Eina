package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking

fun isNewPR(
    weightType: WeightType,
    newSet: SetEntryEntity,
    historicalSets: List<SetEntryEntity> // tutte le set non-warmup gia' completate per lo stesso exerciseId
): Boolean {
    if (!newSet.setType.countsAsWorking) return false
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
            (newSet.actualReps ?: 0) > (historicalSets.maxOfOrNull { it.actualReps ?: 0 } ?: 0) // riusa actualReps come durata

        // Distanza e tempo insieme: correre piu' lontano e' un record, ma anche correre la
        // stessa distanza piu' in fretta lo e'. Quindi basta superare uno dei due — la
        // distanza massima o la velocita' media massima — perche' la serie sia un PR.
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
 * Velocita' media di una serie a distanza, in km/h: `weight` sono chilometri e `actualReps`
 * minuti (vedi [WeightType.DISTANCE_BASED]). Senza tempo registrato non c'e' velocita' da
 * confrontare e vale 0.
 */
fun speedKmPerHour(set: SetEntryEntity): Double {
    val minutes = set.actualReps ?: 0
    if (minutes <= 0) return 0.0
    return (set.weight ?: 0.0) / (minutes / 60.0)
}

/**
 * Volume in kg di una set.
 *
 * Il peso corporeo conta solo per la quota che l'esercizio solleva davvero
 * ([ExerciseEntity.bodyweightFactor]): le trazioni tirano su tutto il corpo e vanno nel
 * totale, i crunch non lo alzano contro gravita' e valgono 0, altrimenti bastava fare
 * addominali per gonfiare il volume della sessione in proporzione a quanto si pesa.
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
            0.0 // tempo e distanza non si misurano in kg: fuori dal totale sollevato
    }
}
