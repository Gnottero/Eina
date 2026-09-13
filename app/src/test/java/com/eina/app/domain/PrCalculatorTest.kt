package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrCalculatorTest {

    private fun set(
        weight: Double? = null,
        actualReps: Int? = null,
        setType: SetType = SetType.NORMAL,
        bodyweightSnapshotKg: Double? = null
    ) = SetEntryEntity(
        workoutExerciseId = 1,
        setIndex = 0,
        actualReps = actualReps,
        weight = weight,
        restSecondsPlanned = 60,
        setType = setType,
        bodyweightSnapshotKg = bodyweightSnapshotKg
    )

    @Test
    fun `a warmup holds a record like any other set`() {
        val newSet = set(weight = 100.0, actualReps = 5, setType = SetType.WARMUP)
        assertTrue(isNewPR(WeightType.FREE_WEIGHT, newSet, emptyList()))
        assertFalse(isNewPR(WeightType.FREE_WEIGHT, newSet, listOf(set(weight = 120.0, actualReps = 1))))
    }

    @Test
    fun `free weight PR beats max historical weight`() {
        val history = listOf(set(weight = 80.0, actualReps = 5), set(weight = 90.0, actualReps = 3))
        assertTrue(isNewPR(WeightType.FREE_WEIGHT, set(weight = 95.0, actualReps = 1), history))
        assertFalse(isNewPR(WeightType.FREE_WEIGHT, set(weight = 90.0, actualReps = 5), history))
    }

    @Test
    fun `machine stack and assisted use weight like free weight`() {
        val history = listOf(set(weight = 50.0, actualReps = 8))
        assertTrue(isNewPR(WeightType.MACHINE_STACK, set(weight = 55.0, actualReps = 8), history))
        assertTrue(isNewPR(WeightType.ASSISTED, set(weight = 55.0, actualReps = 8), history))
    }

    @Test
    fun `bodyweight PR beats max historical reps`() {
        val history = listOf(set(actualReps = 10), set(actualReps = 12))
        assertTrue(isNewPR(WeightType.BODYWEIGHT, set(actualReps = 13), history))
        assertFalse(isNewPR(WeightType.BODYWEIGHT, set(actualReps = 12), history))
    }

    @Test
    fun `bodyweight plus load PR compares combined load`() {
        val history = listOf(set(bodyweightSnapshotKg = 80.0, weight = 10.0, actualReps = 5)) // 90
        val newSet = set(bodyweightSnapshotKg = 82.0, weight = 10.0, actualReps = 5) // 92
        assertTrue(isNewPR(WeightType.BODYWEIGHT_PLUS_LOAD, newSet, history))
        assertFalse(
            isNewPR(
                WeightType.BODYWEIGHT_PLUS_LOAD,
                set(bodyweightSnapshotKg = 80.0, weight = 10.0, actualReps = 5),
                history
            )
        )
    }

    @Test
    fun `time based PR beats max historical duration`() {
        val history = listOf(set(actualReps = 60))
        assertTrue(isNewPR(WeightType.TIME_BASED, set(actualReps = 90), history))
        assertFalse(isNewPR(WeightType.TIME_BASED, set(actualReps = 30), history))
    }

    @Test
    fun `volume free weight and machine stack is weight times reps`() {
        val s = set(weight = 100.0, actualReps = 5)
        assertEquals(500.0, volumeForSet(WeightType.FREE_WEIGHT, s), 0.0)
        assertEquals(500.0, volumeForSet(WeightType.MACHINE_STACK, s), 0.0)
    }

    @Test
    fun `volume bodyweight uses snapshot times reps`() {
        val s = set(bodyweightSnapshotKg = 75.0, actualReps = 10)
        assertEquals(750.0, volumeForSet(WeightType.BODYWEIGHT, s), 0.0)
    }

    @Test
    fun `volume bodyweight scales with the exercise factor`() {
        val s = set(bodyweightSnapshotKg = 75.0, actualReps = 10)
        // Crunch: the body is not lifted, so no volume however many are done.
        assertEquals(0.0, volumeForSet(WeightType.BODYWEIGHT, s, bodyweightFactor = 0.0), 0.0)
        // Push-up: about 64% of the bodyweight rests on the arms.
        assertEquals(480.0, volumeForSet(WeightType.BODYWEIGHT, s, bodyweightFactor = 0.64), 0.0)
    }

    @Test
    fun `volume bodyweight plus load sums lifted bodyweight and added weight`() {
        val s = set(bodyweightSnapshotKg = 75.0, weight = 10.0, actualReps = 8)
        assertEquals(680.0, volumeForSet(WeightType.BODYWEIGHT_PLUS_LOAD, s), 0.0)
    }

    @Test
    fun `volume assisted subtracts assistance and floors at zero`() {
        val heavyAssist = set(bodyweightSnapshotKg = 70.0, weight = 80.0, actualReps = 5)
        assertEquals(0.0, volumeForSet(WeightType.ASSISTED, heavyAssist), 0.0)

        val lightAssist = set(bodyweightSnapshotKg = 70.0, weight = 20.0, actualReps = 5)
        assertEquals(250.0, volumeForSet(WeightType.ASSISTED, lightAssist), 0.0)
    }

    @Test
    fun `volume time based is always zero`() {
        val s = set(actualReps = 120)
        assertEquals(0.0, volumeForSet(WeightType.TIME_BASED, s), 0.0)
    }

    @Test
    fun `distance PR on a longer run`() {
        val history = listOf(set(weight = 5.0, actualReps = 30))
        assertTrue(isNewPR(WeightType.DISTANCE_BASED, set(weight = 6.0, actualReps = 40), history))
        assertFalse(isNewPR(WeightType.DISTANCE_BASED, set(weight = 4.0, actualReps = 30), history))
    }

    @Test
    fun `distance PR on the same run done faster`() {
        val history = listOf(set(weight = 5.0, actualReps = 30))
        assertTrue(isNewPR(WeightType.DISTANCE_BASED, set(weight = 5.0, actualReps = 25), history))
        assertFalse(isNewPR(WeightType.DISTANCE_BASED, set(weight = 5.0, actualReps = 35), history))
    }

    @Test
    fun `distance set without distance is never a PR`() {
        assertFalse(isNewPR(WeightType.DISTANCE_BASED, set(actualReps = 30), emptyList()))
    }

    @Test
    fun `distance volume stays out of the kg total`() {
        assertEquals(0.0, volumeForSet(WeightType.DISTANCE_BASED, set(weight = 5.0, actualReps = 30)), 0.0)
    }

    @Test
    fun `a weight closed with zero reps is never a PR`() {
        val history = listOf(set(weight = 100.0, actualReps = 5))
        assertFalse(isNewPR(WeightType.FREE_WEIGHT, set(weight = 200.0, actualReps = 0), history))
        assertFalse(isNewPR(WeightType.FREE_WEIGHT, set(weight = 200.0, actualReps = null), history))
        assertFalse(isNewPR(WeightType.MACHINE_STACK, set(weight = 200.0, actualReps = 0), history))
        assertFalse(isNewPR(WeightType.ASSISTED, set(weight = 200.0, actualReps = 0), history))
        assertFalse(
            isNewPR(
                WeightType.BODYWEIGHT_PLUS_LOAD,
                set(bodyweightSnapshotKg = 80.0, weight = 100.0, actualReps = 0),
                listOf(set(bodyweightSnapshotKg = 80.0, weight = 10.0, actualReps = 5))
            )
        )
    }

    @Test
    fun `a weight closed with zero reps does not raise the bar for the next set`() {
        // The 200 kg was typed in and never lifted: the next real set only has to beat the 100 kg.
        val history = listOf(set(weight = 100.0, actualReps = 5), set(weight = 200.0, actualReps = 0))
        assertTrue(isNewPR(WeightType.FREE_WEIGHT, set(weight = 110.0, actualReps = 3), history))
    }

    @Test
    fun `a hold closed with zero seconds is never a PR`() {
        assertFalse(isNewPR(WeightType.TIME_BASED, set(actualReps = 0), emptyList()))
    }
}
