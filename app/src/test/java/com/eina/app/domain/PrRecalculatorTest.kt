package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrRecalculatorTest {

    private fun set(
        id: Long,
        weight: Double?,
        completedAt: Long?,
        isPR: Boolean = false,
        setType: SetType = SetType.NORMAL,
        reps: Int? = 5
    ) = SetEntryEntity(
        id = id,
        workoutExerciseId = 1,
        setIndex = id.toInt(),
        actualReps = reps,
        weight = weight,
        restSecondsPlanned = 90,
        setType = setType,
        completedAt = completedAt,
        isPR = isPR
    )

    @Test
    fun `il record segue il massimo progressivo`() {
        val sets = listOf(
            set(1, 60.0, 1_000),
            set(2, 80.0, 2_000),
            set(3, 70.0, 3_000),
            set(4, 90.0, 4_000)
        )

        val changed = recomputePrFlags(WeightType.FREE_WEIGHT, sets).associateBy { it.id }

        assertEquals(setOf(1L, 2L, 4L), changed.keys)
        assertTrue(changed.values.all { it.isPR })
    }

    @Test
    fun `abbassare il carico di una serie passata sposta il record`() {
        // La serie 2 aveva il record; corretta a 65 kg, il massimo torna alla 3.
        val sets = listOf(
            set(1, 60.0, 1_000, isPR = true),
            set(2, 65.0, 2_000, isPR = true),
            set(3, 70.0, 3_000, isPR = false)
        )

        val changed = recomputePrFlags(WeightType.FREE_WEIGHT, sets)

        assertEquals(listOf(3L to true), changed.map { it.id to it.isPR })
    }

    @Test
    fun `un riscaldamento non tiene ne' assegna record`() {
        val sets = listOf(
            set(1, 100.0, 1_000, isPR = true, setType = SetType.WARMUP),
            set(2, 80.0, 2_000)
        )

        val changed = recomputePrFlags(WeightType.FREE_WEIGHT, sets).associateBy { it.id }

        assertEquals(false, changed.getValue(1L).isPR)
        // Il carico del riscaldamento non conta nel confronto: 80 kg resta un record.
        assertEquals(true, changed.getValue(2L).isPR)
    }

    @Test
    fun `una serie non completata non e' un record`() {
        val sets = listOf(set(1, 200.0, null, isPR = true))

        val changed = recomputePrFlags(WeightType.FREE_WEIGHT, sets)

        assertEquals(listOf(1L to false), changed.map { it.id to it.isPR })
    }

    @Test
    fun `senza cambiamenti non si riscrive niente`() {
        val sets = listOf(
            set(1, 60.0, 1_000, isPR = true),
            set(2, 55.0, 2_000, isPR = false)
        )

        assertTrue(recomputePrFlags(WeightType.FREE_WEIGHT, sets).isEmpty())
    }
}
