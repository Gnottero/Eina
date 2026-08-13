package com.eina.app.ui.workout

import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType

data class SessionSetUi(
    val id: Long,
    val setIndex: Int,
    val targetReps: Int? = null,
    val actualReps: Int? = null,
    val weight: Double? = null,
    val restSecondsPlanned: Int,
    val setType: SetType = SetType.NORMAL,
    val completedAt: Long? = null,
    val isPR: Boolean = false,
    val bodyweightSnapshotKg: Double? = null,
    /** Routine target weight: a UI placeholder only, never a recorded value. */
    val targetWeight: Double? = null,
    /** Matching set of the last workout, shown in the "previous" column. */
    val previous: SetEntryEntity? = null,
    /**
     * Suggested values: printed in grey in the fields and recorded if the set is completed without
     * typing anything. See ActiveWorkoutViewModel.withSuggestions for the fallback chain.
     */
    val suggestedWeight: Double? = null,
    val suggestedReps: Int? = null
)

data class SessionExerciseUi(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val name: ExerciseName,
    val weightType: WeightType,
    /** See ExerciseEntity.bodyweightFactor; used by the volume shown in the header. */
    val bodyweightFactor: Double = 1.0,
    val order: Int,
    val restSeconds: Int = 90,
    /** Note for this session, inherited from the routine and editable here. */
    val notes: String? = null,
    /** Superset group; see [com.eina.app.domain.Superset]. Null means the exercise stands alone. */
    val supersetGroup: Int? = null,
    val sets: List<SessionSetUi> = emptyList(),
    val lastTimeSets: List<SetEntryEntity> = emptyList(),
    /** Last values recorded for this exercise anywhere: the final link of the placeholder chain. */
    val lastRecordedWeight: Double? = null,
    val lastRecordedReps: Int? = null
) {
    val completedSets: Int get() = sets.count { it.completedAt != null }
}

data class TimerUi(
    val totalSeconds: Int,
    val remainingSeconds: Int
)

data class ActiveWorkoutUiState(
    val sessionId: Long,
    /** Recorded workout opened from the history for correction. */
    val isPast: Boolean = false,
    val startTime: Long = System.currentTimeMillis(),
    val elapsedSeconds: Int = 0,
    val exercises: List<SessionExerciseUi> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val timer: TimerUi? = null,
    val isFinished: Boolean = false,
    /** Source routine, if any; needed to offer updating it at the end. */
    val routineId: Long? = null,
    val routineName: String? = null,
    /** Playlist of the source routine; played from here, not from the editor. */
    val playlistUri: String? = null,
    val playlistType: PlaylistType? = null,
    /** Volume in kg of the completed sets only, recomputed on every refresh. */
    val volumeKg: Double = 0.0
) {
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val progress: Float get() = if (totalSets == 0) 0f else completedSets.toFloat() / totalSets
}
