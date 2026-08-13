package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Planned set of an exercise inside a routine.
 *
 * A plain set count ("3 sets of 10") could not express a warmup followed by two failure sets, which
 * is how routines are actually written. Each set is a row with its own type, as in a workout.
 */
@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineExerciseId")]
)
data class RoutineSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineExerciseId: Long,
    val setIndex: Int,
    /** Seconds for timed exercises, as `actualReps` is in a session. */
    val targetReps: Int? = null,
    val targetWeight: Double? = null,
    val setType: SetType = SetType.NORMAL
)
