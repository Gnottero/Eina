package com.eina.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetEntryDaoTest {

    private lateinit var db: EinaDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, EinaDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getLastTimeSets_returnsMostRecentSessionOnly() = runBlocking {
        val exerciseId = db.exerciseDao().insert(
            ExerciseEntity(
                name = "Bench Press",
                description = "",
                loggingInstructions = "",
                weightType = WeightType.FREE_WEIGHT,
                muscleGroupsPrimary = listOf("Chest"),
                muscleGroupsSecondary = emptyList()
            )
        )

        val olderSessionId = db.workoutSessionDao().insert(WorkoutSessionEntity(startTime = 1_000L))
        val newerSessionId = db.workoutSessionDao().insert(WorkoutSessionEntity(startTime = 2_000L))
        val currentSessionId = db.workoutSessionDao().insert(WorkoutSessionEntity(startTime = 3_000L))

        val olderWorkoutExerciseId = db.workoutExerciseDao().insert(
            WorkoutExerciseEntity(sessionId = olderSessionId, exerciseId = exerciseId, order = 0)
        )
        val newerWorkoutExerciseId = db.workoutExerciseDao().insert(
            WorkoutExerciseEntity(sessionId = newerSessionId, exerciseId = exerciseId, order = 0)
        )
        val currentWorkoutExerciseId = db.workoutExerciseDao().insert(
            WorkoutExerciseEntity(sessionId = currentSessionId, exerciseId = exerciseId, order = 0)
        )

        db.setEntryDao().insert(
            SetEntryEntity(workoutExerciseId = olderWorkoutExerciseId, setIndex = 0, weight = 80.0, actualReps = 5, restSecondsPlanned = 90)
        )
        db.setEntryDao().insert(
            SetEntryEntity(workoutExerciseId = newerWorkoutExerciseId, setIndex = 0, weight = 90.0, actualReps = 5, restSecondsPlanned = 90)
        )
        db.setEntryDao().insert(
            SetEntryEntity(workoutExerciseId = newerWorkoutExerciseId, setIndex = 1, weight = 95.0, actualReps = 3, restSecondsPlanned = 90)
        )
        db.setEntryDao().insert(
            SetEntryEntity(workoutExerciseId = currentWorkoutExerciseId, setIndex = 0, weight = 999.0, actualReps = 1, restSecondsPlanned = 90)
        )

        val lastTime = db.setEntryDao().getLastTimeSets(exerciseId, excludeSessionId = currentSessionId)

        assertEquals(2, lastTime.size)
        assertEquals(90.0, lastTime[0].weight)
        assertEquals(95.0, lastTime[1].weight)
    }
}
