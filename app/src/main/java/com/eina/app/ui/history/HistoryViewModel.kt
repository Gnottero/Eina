package com.eina.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.HeartRateSample
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.data.db.heartRateSeries
import com.eina.app.data.health.WorkoutHealthSync
import com.eina.app.data.repository.StatsRepository
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.domain.SessionSummary
import com.eina.app.domain.currentStreak
import com.eina.app.domain.summarizeSessions
import com.eina.app.domain.trainingDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    repository: StatsRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    val sessions: StateFlow<List<SessionSummary>> = repository.observeCompletedSets()
        .map { summarizeSessions(it) }
        // Riepilogare tutto lo storico non e' lavoro da thread della UI.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Elimina un singolo allenamento. Finora si poteva solo svuotare tutto lo storico, il che
     * rendeva impossibile togliere una sessione sbagliata senza perdere anche le altre.
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { workoutRepository.deleteSession(sessionId) }
    }
}

/**
 * Un blocco di lavoro della sessione con le sue set completate, in ordine. La chiave e'
 * workoutExerciseId: lo stesso esercizio svolto due volte nella stessa sessione resta due blocchi.
 */
data class SessionExerciseDetail(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: ExerciseName,
    val sets: List<CompletedSetRow>
) {
    val workingSets: List<CompletedSetRow> get() = sets.filter { it.setType.countsAsWorking }
}

data class SessionDetailUiState(
    val summary: SessionSummary? = null,
    val exercises: List<SessionExerciseDetail> = emptyList(),
    val streakWeeks: Int = 0,
    /** Quel che l'orologio ha misurato, se c'era. Fuori dalla card condivisibile per scelta. */
    val vitals: SessionVitals? = null
)

/** Battiti e calorie di una sessione, gia' pronti da disegnare. */
data class SessionVitals(
    val avgBpm: Int?,
    val maxBpm: Int?,
    val kcal: Double?,
    val samples: List<HeartRateSample>
) {
    val hasData: Boolean get() = avgBpm != null || kcal != null || samples.isNotEmpty()
}

class SessionDetailViewModel(
    repository: StatsRepository,
    workoutRepository: WorkoutRepository,
    private val healthSync: WorkoutHealthSync,
    private val sessionId: Long
) : ViewModel() {

    init {
        // Un orologio sincronizza con comodo: i battiti dell'ultima serie possono arrivare in
        // Health Connect dopo il "Termina", quindi si riprova all'apertura del riepilogo.
        viewModelScope.launch { runCatching { healthSync.sync(sessionId) } }
    }

    val uiState: StateFlow<SessionDetailUiState> = combine(
        repository.observeSessionSets(sessionId),
        // Lo streak si calcola su tutto lo storico: e' il numero che si mostra a fine allenamento.
        repository.observeCompletedSets(),
        workoutRepository.observeSession(sessionId)
    ) { rows, allRows, session ->
        SessionDetailUiState(
            vitals = session?.toVitals()?.takeIf { it.hasData },
            summary = summarizeSessions(rows).firstOrNull(),
            exercises = rows
                .sortedWith(compareBy({ it.exerciseOrder }, { it.setIndex }))
                .groupBy { it.workoutExerciseId }
                .map { (workoutExerciseId, exerciseRows) ->
                    SessionExerciseDetail(
                        workoutExerciseId = workoutExerciseId,
                        exerciseId = exerciseRows.first().exerciseId,
                        exerciseName = exerciseRows.first().exerciseName,
                        sets = exerciseRows.sortedBy { it.setIndex }
                    )
                },
            streakWeeks = currentStreak(trainingDays(allRows))
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())
}

private fun WorkoutSessionEntity.toVitals() = SessionVitals(
    avgBpm = avgHeartRateBpm,
    maxBpm = maxHeartRateBpm,
    kcal = caloriesKcal,
    samples = heartRateSeries()
)
