package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"])
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val order: Int,
    /**
     * Sets and reps no longer live here: each set is a `routine_sets` row with its own type (see
     * [RoutineSetEntity]). What remains applies to the whole exercise.
     */
    val restSeconds: Int,
    /** Free note on the exercise in the routine, copied into the session at start. */
    val notes: String? = null,
    /**
     * Superset: exercises sharing a number are performed as a round, one after another, and rest
     * starts only once the round is over. Null means the exercise stands alone. Members of a group
     * are always consecutive in `order`.
     */
    val supersetGroup: Int? = null
)
