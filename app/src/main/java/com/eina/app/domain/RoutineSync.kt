package com.eina.app.domain

import com.eina.app.data.db.ExerciseName

/**
 * Una voce di scheda o di allenamento, ridotta a quel che si confronta fra le due. I valori di
 * peso e ripetizioni non ci sono: cambiano quasi sempre (e' il senso di allenarsi) e chiedere di
 * aggiornare la scheda a ogni chilo in piu' sarebbe una domanda a ogni allenamento. Qui si
 * guarda solo l'impianto: quali esercizi, in che ordine, con quante serie e che recupero.
 */
data class PlanItem(
    val exerciseId: Long,
    val name: ExerciseName,
    val setCount: Int,
    val restSeconds: Int
)

/** Una differenza fra la scheda e come e' andato l'allenamento, gia' pronta da elencare. */
sealed interface RoutineChange {
    data class Added(val name: ExerciseName) : RoutineChange
    data class Removed(val name: ExerciseName) : RoutineChange
    data class SetsChanged(val name: ExerciseName, val from: Int, val to: Int) : RoutineChange
    data class RestChanged(val name: ExerciseName, val fromSeconds: Int, val toSeconds: Int) : RoutineChange
    data object Reordered : RoutineChange
}

/**
 * Cosa e' cambiato fra la scheda di partenza e l'allenamento appena chiuso.
 *
 * Il confronto e' per esercizio e non per posizione: spostare una voce non deve leggersi come
 * "tolto uno, aggiunto un altro". Lo stesso esercizio ripetuto due volte nella stessa scheda
 * resta due voci, quindi si confrontano le occorrenze una a una nell'ordine in cui compaiono.
 *
 * Lista vuota = l'allenamento ha seguito la scheda, e non c'e' niente da chiedere.
 */
fun routineChanges(routine: List<PlanItem>, session: List<PlanItem>): List<RoutineChange> {
    val changes = mutableListOf<RoutineChange>()
    val routineByExercise = routine.groupBy { it.exerciseId }
    val sessionByExercise = session.groupBy { it.exerciseId }

    // Ordine di lettura: prima quel che c'e' in allenamento, poi quel che e' rimasto fuori.
    val exerciseIds = session.map { it.exerciseId }.distinct() +
        routine.map { it.exerciseId }.distinct().filterNot { sessionByExercise.containsKey(it) }

    exerciseIds.forEach { exerciseId ->
        val planned = routineByExercise[exerciseId].orEmpty()
        val done = sessionByExercise[exerciseId].orEmpty()
        repeat(done.size - planned.size) { changes += RoutineChange.Added(done[planned.size + it].name) }
        repeat(planned.size - done.size) { changes += RoutineChange.Removed(planned[done.size + it].name) }
        planned.zip(done).forEach { (before, after) ->
            if (before.setCount != after.setCount) {
                changes += RoutineChange.SetsChanged(after.name, before.setCount, after.setCount)
            }
            if (before.restSeconds != after.restSeconds) {
                changes += RoutineChange.RestChanged(after.name, before.restSeconds, after.restSeconds)
            }
        }
    }

    // L'ordine si giudica sui soli esercizi rimasti in entrambe: aggiunte ed eliminazioni sono
    // gia' state dette, e conterebbero due volte come "ordine diverso".
    val common = session.map { it.exerciseId }.filter { routineByExercise.containsKey(it) }
    val plannedCommon = routine.map { it.exerciseId }.filter { sessionByExercise.containsKey(it) }
    if (common.distinct() != plannedCommon.distinct()) changes += RoutineChange.Reordered

    return changes
}
