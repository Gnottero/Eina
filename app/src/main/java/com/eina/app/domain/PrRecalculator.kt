package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking

/**
 * Recomputes the `isPR` flag of every set of an exercise.
 *
 * `isPR` is the only domain value not derived from a query: it is written on the row when the set
 * is completed, comparing it with what came before. That was enough while the past was immutable;
 * now that a recorded workout can be corrected it is not — lowering the load of a set from last
 * month would leave its record standing, and raising it would not create one.
 *
 * So the flags are rebuilt from scratch: sets are walked in completion order and each is a record
 * if it beats everything before it, exactly as [isNewPR] does live. Warmups never hold a record and
 * stay out of the comparison.
 *
 * [sets] must be complete (every completed set of that exercise, in any session); the order does
 * not matter, the function sorts them. Only the rows whose flag changes are returned, so the caller
 * writes just those.
 */
fun recomputePrFlags(weightType: WeightType, sets: List<SetEntryEntity>): List<SetEntryEntity> {
    // On equal instants the set index breaks the tie: sets completed in the same millisecond do
    // occur, and without a second criterion the outcome would depend on the database row order.
    val ordered = sets.sortedWith(compareBy({ it.completedAt ?: 0L }, { it.setIndex }, { it.id }))
    val history = mutableListOf<SetEntryEntity>()
    val changed = mutableListOf<SetEntryEntity>()

    ordered.forEach { set ->
        val isPR = set.completedAt != null && isNewPR(weightType, set, history)
        if (isPR != set.isPR) changed += set.copy(isPR = isPR)
        if (set.completedAt != null && set.setType.countsAsWorking) history += set
    }
    return changed
}
