package com.eina.app.ui.workout

import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType

data class SessionSetUi(
    val id: Long,
    val setIndex: Int,
    val targetReps: Int? = null,
    val actualReps: Int? = null,
    val weight: Double? = null,
    val restSecondsPlanned: Int,
    val isWarmup: Boolean = false,
    val completedAt: Long? = null,
    val isPR: Boolean = false,
    val bodyweightSnapshotKg: Double? = null
)

data class SessionExerciseUi(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val name: String,
    val weightType: WeightType,
    val order: Int,
    val sets: List<SessionSetUi> = emptyList(),
    val lastTimeSets: List<SetEntryEntity> = emptyList()
)

data class TimerUi(
    val totalSeconds: Int,
    val remainingSeconds: Int
)

data class ActiveWorkoutUiState(
    val sessionId: Long,
    val exercises: List<SessionExerciseUi> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val timer: TimerUi? = null,
    val isFinished: Boolean = false
)
