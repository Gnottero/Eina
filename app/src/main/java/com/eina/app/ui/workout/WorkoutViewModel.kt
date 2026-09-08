package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.repository.StatsRepository
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.domain.totalVolume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Numbers of the open session, shown on the banner that resumes it. They are the same three the
 * workout screen carries in its header: leaving the session should not mean losing sight of it.
 */
data class OpenSessionUi(
    val volumeKg: Double = 0.0,
    val setCount: Int = 0,
    val prCount: Int = 0,
    /** Exercises with at least one completed set, over the total in the session. */
    val exercisesDone: Int = 0,
    val exerciseCount: Int = 0
)

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    stats: StatsRepository
) : ViewModel() {

    /** Allenamento ancora aperto: finche' c'e', la schermata offre "Riprendi" invece di "Inizia". */
    val activeSession: StateFlow<WorkoutSessionEntity?> = repository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val openSession: StateFlow<OpenSessionUi?> = repository.observeActiveSession()
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                stats.observeSessionSets(session.id).map { rows ->
                    val working = rows.filter { it.setType.countsAsWorking }
                    OpenSessionUi(
                        volumeKg = totalVolume(rows),
                        setCount = working.size,
                        prCount = rows.count { it.isPR },
                        // Completed sets only name the exercises already touched, so the total
                        // comes from the session itself.
                        exercisesDone = rows.mapTo(HashSet()) { it.workoutExerciseId }.size,
                        exerciseCount = repository.getSessionExercises(session.id).size
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startNewSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = repository.startSession()
            onCreated(sessionId)
        }
    }
}
