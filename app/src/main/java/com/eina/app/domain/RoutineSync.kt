package com.eina.app.domain

import com.eina.app.data.db.ExerciseName

/**
 * A routine or workout row, reduced to what the two are compared on. Weight and reps are left out:
 * they change almost every time, and asking to update the routine over every extra kilo would mean
 * a question after every workout. Only the structure is compared: which exercises, in what order,
 * with how many sets and what rest.
 */
data class PlanItem(
    val exerciseId: Long,
    val name: ExerciseName,
    val setCount: Int,
    val restSeconds: Int
)

/** One difference between the routine and the performed workout, ready to be listed. */
sealed interface RoutineChange {
    data class Added(val name: ExerciseName) : RoutineChange
    data class Removed(val name: ExerciseName) : RoutineChange
    data class SetsChanged(val name: ExerciseName, val from: Int, val to: Int) : RoutineChange
    data class RestChanged(val name: ExerciseName, val fromSeconds: Int, val toSeconds: Int) : RoutineChange
    data object Reordered : RoutineChange
}

/**
 * What changed between the source routine and the workout just closed.
 *
 * The comparison is per exercise and not per position, so moving a row does not read as "one
 * removed, another added". The same exercise repeated twice stays two rows, so occurrences are
 * matched one by one in the order they appear.
 *
 * An empty list means the workout followed the routine and there is nothing to ask.
 */
fun routineChanges(routine: List<PlanItem>, session: List<PlanItem>): List<RoutineChange> {
    val changes = mutableListOf<RoutineChange>()
    val routineByExercise = routine.groupBy { it.exerciseId }
    val sessionByExercise = session.groupBy { it.exerciseId }

    // Reading order: first what the workout contains, then what was left out.
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

    // Order is judged on the exercises present in both: additions and removals were already
    // reported and would otherwise count twice as a reordering.
    val common = session.map { it.exerciseId }.filter { routineByExercise.containsKey(it) }
    val plannedCommon = routine.map { it.exerciseId }.filter { sessionByExercise.containsKey(it) }
    if (common.distinct() != plannedCommon.distinct()) changes += RoutineChange.Reordered

    return changes
}
