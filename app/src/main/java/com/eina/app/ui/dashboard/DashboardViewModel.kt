package com.eina.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.prefs.SettingsRepository
import com.eina.app.data.repository.StatsRepository
import com.eina.app.domain.SessionSummary
import com.eina.app.domain.epochMillisToLocalDate
import com.eina.app.domain.summarizeSessions
import com.eina.app.domain.volumeByDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardUiState(
    val weekSessions: Int = 0,
    /** Distinct days trained in the current week, which is what fills the ring. */
    val weekDaysTrained: Int = 0,
    val weekVolumeKg: Double = 0.0,
    val weekVolumeByDay: List<Float> = List(7) { 0f },
    /** Records set this week: the second number of the hero, next to the volume. */
    val weekPrCount: Int = 0,
    /** Days per week the ring is drawn against; see SettingsRepository.weeklyGoalDays. */
    val weekGoalDays: Int = 4,
    val recentSessions: List<SessionSummary> = emptyList()
) {
    /** How many more days would close the ring; zero once the goal is met. */
    val goalDaysLeft: Int get() = (weekGoalDays - weekDaysTrained).coerceAtLeast(0)
}

class DashboardViewModel(
    repository: StatsRepository,
    settings: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeCompletedSets(),
        settings.weeklyGoalDays
    ) { rows, goalDays ->
            val today = LocalDate.now()
            val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val sessions = summarizeSessions(rows)
            val weekSessions = sessions.filter {
                !epochMillisToLocalDate(it.startTime).isBefore(startOfWeek)
            }
            val volumePerDay = volumeByDay(rows)

            DashboardUiState(
                weekSessions = weekSessions.size,
                // Two sessions on the same day fill one segment: the ring counts days, not
                // workouts, or it could close in a single Sunday.
                weekDaysTrained = weekSessions
                    .map { epochMillisToLocalDate(it.startTime) }
                    .distinct()
                    .size,
                weekVolumeKg = weekSessions.sumOf { it.volumeKg },
                weekPrCount = weekSessions.sumOf { it.prCount },
                weekGoalDays = goalDays,
                weekVolumeByDay = (0..6).map { offset ->
                    (volumePerDay[startOfWeek.plusDays(offset.toLong())] ?: 0.0).toFloat()
                },
                recentSessions = sessions.take(RECENT_SESSIONS)
            )
        }
        // The summary is recomputed over the whole history on every emission, so it runs off the
        // UI thread: with many sessions the update would otherwise be noticeable.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private companion object {
        /** How many recent sessions reach the Dashboard; the rest live in the History. */
        const val RECENT_SESSIONS = 3
    }
}
