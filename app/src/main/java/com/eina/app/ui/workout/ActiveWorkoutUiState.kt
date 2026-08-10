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
    /** Peso target della routine: solo segnaposto in UI, mai un valore registrato. */
    val targetWeight: Double? = null,
    /** Serie corrispondente dell'ultima volta, mostrata in colonna "Precedente". */
    val previous: SetEntryEntity? = null,
    /**
     * Valori proposti per la serie: sono quelli stampati in grigio nei campi e quelli che
     * vengono registrati se si chiude la serie senza digitare nulla. Vedi
     * ActiveWorkoutViewModel.withSuggestions per la catena di ripiego.
     */
    val suggestedWeight: Double? = null,
    val suggestedReps: Int? = null
)

data class SessionExerciseUi(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val name: ExerciseName,
    val weightType: WeightType,
    /** Vedi ExerciseEntity.bodyweightFactor: serve al volume mostrato nell'header. */
    val bodyweightFactor: Double = 1.0,
    val order: Int,
    val restSeconds: Int = 90,
    /** Nota dell'esercizio in questa sessione: ereditata dalla routine, modificabile qui. */
    val notes: String? = null,
    /** Superset di appartenenza: vedi [com.eina.app.domain.Superset]. null = esercizio a se'. */
    val supersetGroup: Int? = null,
    val sets: List<SessionSetUi> = emptyList(),
    val lastTimeSets: List<SetEntryEntity> = emptyList(),
    /** Ultimi valori registrati per questo esercizio, ovunque: ultimo anello dei segnaposto. */
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
    val startTime: Long = System.currentTimeMillis(),
    val elapsedSeconds: Int = 0,
    val exercises: List<SessionExerciseUi> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val timer: TimerUi? = null,
    val isFinished: Boolean = false,
    /** Routine di partenza, se la sessione ne aveva una: serve alla proposta di aggiornarla. */
    val routineId: Long? = null,
    val routineName: String? = null,
    /** Playlist della routine di partenza: si riproduce da qui, non dall'editor della routine. */
    val playlistUri: String? = null,
    val playlistType: PlaylistType? = null,
    /** Volume in kg delle sole serie completate, ricalcolato a ogni refresh. */
    val volumeKg: Double = 0.0
) {
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val progress: Float get() = if (totalSets == 0) 0f else completedSets.toFloat() / totalSets
}
