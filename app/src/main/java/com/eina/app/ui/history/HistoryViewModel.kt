package com.eina.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.repository.StatsRepository
import com.eina.app.domain.SessionSummary
import com.eina.app.domain.summarizeSessions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(repository: StatsRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionSummary>> = repository.observeCompletedSets()
        .map { summarizeSessions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Un esercizio della sessione con le sue set completate, in ordine. */
data class SessionExerciseDetail(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<CompletedSetRow>
)

data class SessionDetailUiState(
    val summary: SessionSummary? = null,
    val exercises: List<SessionExerciseDetail> = emptyList()
)

class SessionDetailViewModel(
    repository: StatsRepository,
    sessionId: Long
) : ViewModel() {
    val uiState: StateFlow<SessionDetailUiState> = repository.observeSessionSets(sessionId)
        .map { rows ->
            SessionDetailUiState(
                summary = summarizeSessions(rows).firstOrNull(),
                exercises = rows.groupBy { it.exerciseId }.map { (exerciseId, exerciseRows) ->
                    SessionExerciseDetail(
                        exerciseId = exerciseId,
                        exerciseName = exerciseRows.first().exerciseName,
                        sets = exerciseRows.sortedBy { it.setIndex }
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())
}
