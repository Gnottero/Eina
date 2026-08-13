package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val order: Int,
    /**
     * Rest of the exercise in this session. It lives here and not only on the sets: rest is changed
     * in the gym, and reading it from the first set made it fall back to the routine value as soon
     * as that set was completed, since a completed set is never rewritten.
     */
    val restSeconds: Int = 90,
    /** Session note, inherited from the routine and editable here. */
    val notes: String? = null,
    /**
     * Session superset, inherited from the routine and editable here.
     * See [RoutineExerciseEntity.supersetGroup].
     */
    val supersetGroup: Int? = null
)
