package com.eina.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType
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
        // Summarising the whole history is not UI thread work.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Deletes a single workout, PR flags included (see [WorkoutRepository.deleteSession]). */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { workoutRepository.deleteSession(sessionId) }
    }
}

/**
 * One work block of the session with its completed sets, in order. Keyed by workoutExerciseId, so
 * the same exercise performed twice in a session stays two blocks.
 */
data class SessionExerciseDetail(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: ExerciseName,
    /** Superset group, as in the workout screen; letter and colour are assigned by the UI. */
    val supersetGroup: Int? = null,
    val sets: List<CompletedSetRow>
) {
    val workingSets: List<CompletedSetRow> get() = sets.filter { it.setType.countsAsWorking }

    /** Load type of the block: it is the same on every set, since it comes from the exercise. */
    val weightType: WeightType get() = sets.firstOrNull()?.weightType ?: WeightType.FREE_WEIGHT
}

data class SessionDetailUiState(
    val summary: SessionSummary? = null,
    val exercises: List<SessionExerciseDetail> = emptyList(),
    val streakWeeks: Int = 0,
    /** What the watch measured, if anything; deliberately kept off the shareable card. */
    val vitals: SessionVitals? = null
)

/** Heart rate and calories of a session, ready to render. */
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
    private val workoutRepository: WorkoutRepository,
    private val healthSync: WorkoutHealthSync,
    private val sessionId: Long
) : ViewModel() {

    /**
     * Creates a routine from this workout. [onDone] receives the id of the new routine, or null if
     * the session no longer has a single exercise to copy.
     */
    fun createRoutine(name: String, onDone: (Long?) -> Unit) {
        viewModelScope.launch {
            onDone(workoutRepository.createRoutineFromSession(sessionId, name))
        }
    }

    init {
        // Watches sync at their own pace: the samples of the last set can reach Health Connect
        // after the workout was finished, so the sync is retried when the summary opens.
        viewModelScope.launch { runCatching { healthSync.sync(sessionId) } }
    }

    val uiState: StateFlow<SessionDetailUiState> = combine(
        repository.observeSessionSets(sessionId),
        // The streak is computed over the whole history.
        repository.observeCompletedSets(),
        workoutRepository.observeSession(sessionId)
    ) { rows, allRows, session ->
        SessionDetailUiState(
            vitals = session?.toVitals()?.takeIf { it.hasData },
            summary = summarizeSessions(rows).firstOrNull(),
            exercises = rows
                // groupBy preserves input order, so sorting here leaves each block already ordered
                // by set index.
                .sortedWith(compareBy({ it.exerciseOrder }, { it.setIndex }))
                .groupBy { it.workoutExerciseId }
                .map { (workoutExerciseId, exerciseRows) ->
                    val head = exerciseRows.first()
                    SessionExerciseDetail(
                        workoutExerciseId = workoutExerciseId,
                        exerciseId = head.exerciseId,
                        exerciseName = head.exerciseName,
                        supersetGroup = head.supersetGroup,
                        sets = exerciseRows
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
