package com.eina.app.domain

import com.eina.app.data.db.ExerciseName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineSyncTest {

    private fun item(id: Long, sets: Int = 3, rest: Int = 90) =
        PlanItem(exerciseId = id, name = ExerciseName("Esercizio $id"), setCount = sets, restSeconds = rest)

    @Test
    fun `un allenamento uguale alla scheda non ha niente da chiedere`() {
        val plan = listOf(item(1), item(2))

        assertEquals(emptyList<RoutineChange>(), routineChanges(plan, plan))
    }

    @Test
    fun `esercizio aggiunto e esercizio tolto`() {
        val changes = routineChanges(listOf(item(1), item(2)), listOf(item(1), item(3)))

        assertTrue(changes.any { it is RoutineChange.Added && it.name.nameEn == "Esercizio 3" })
        assertTrue(changes.any { it is RoutineChange.Removed && it.name.nameEn == "Esercizio 2" })
    }

    @Test
    fun `serie e recupero cambiati si leggono col valore di prima e di dopo`() {
        val changes = routineChanges(listOf(item(1, sets = 3, rest = 90)), listOf(item(1, sets = 4, rest = 120)))

        assertEquals(
            listOf(
                RoutineChange.SetsChanged(ExerciseName("Esercizio 1"), 3, 4),
                RoutineChange.RestChanged(ExerciseName("Esercizio 1"), 90, 120)
            ),
            changes
        )
    }

    @Test
    fun `spostare un esercizio si legge come ordine cambiato e non come aggiunta`() {
        val changes = routineChanges(listOf(item(1), item(2)), listOf(item(2), item(1)))

        assertEquals(listOf(RoutineChange.Reordered), changes)
    }

    @Test
    fun `aggiungere in coda non conta come ordine cambiato`() {
        val changes = routineChanges(listOf(item(1)), listOf(item(1), item(2)))

        assertEquals(listOf(RoutineChange.Added(ExerciseName("Esercizio 2"))), changes)
    }
}
