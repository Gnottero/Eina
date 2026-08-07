package com.eina.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.repository.StatsRepository
import com.eina.app.domain.SessionSummary
import com.eina.app.domain.currentStreak
import com.eina.app.domain.summarizeSessions
import com.eina.app.domain.trainingDays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(repository: StatsRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionSummary>> = repository.observeCompletedSets()
        .map { summarizeSessions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Un blocco di lavoro della sessione con le sue set completate, in ordine. La chiave e'
 * workoutExerciseId: lo stesso esercizio svolto due volte nella stessa sessione resta due blocchi.
 */
data class SessionExerciseDetail(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<CompletedSetRow>
) {
    val workingSets: List<CompletedSetRow> get() = sets.filter { !it.isWarmup }
}

data class SessionDetailUiState(
    val summary: SessionSummary? = null,
    val exercises: List<SessionExerciseDetail> = emptyList(),
    val streakDays: Int = 0
)

class SessionDetailViewModel(
    repository: StatsRepository,
    sessionId: Long
) : ViewModel() {
    val uiState: StateFlow<SessionDetailUiState> = combine(
        repository.observeSessionSets(sessionId),
        // La striscia si calcola su tutto lo storico: e' il numero che si mostra a fine allenamento.
        repository.observeCompletedSets()
    ) { rows, allRows ->
        SessionDetailUiState(
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
            streakDays = currentStreak(trainingDays(allRows))
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())
}
