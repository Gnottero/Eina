package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_entries",
    foreignKeys = [ForeignKey(entity = WorkoutExerciseEntity::class, parentColumns = ["id"], childColumns = ["workoutExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("workoutExerciseId")]
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setIndex: Int,
    val targetReps: Int? = null,
    /** Per-set routine load, kept separate from the value actually recorded by the user. */
    val targetWeight: Double? = null,
    val actualReps: Int? = null,        // TIME_BASED: duration in seconds; DISTANCE_BASED: minutes
    val weight: Double? = null,
    val restSecondsPlanned: Int,
    val setType: SetType = SetType.NORMAL,
    val completedAt: Long? = null,
    val isPR: Boolean = false,
    // Stored when the set is completed, only for BODYWEIGHT / BODYWEIGHT_PLUS_LOAD / ASSISTED:
    // avoids joining BodyMetric over time windows to reconstruct the historical bodyweight.
    val bodyweightSnapshotKg: Double? = null
)
