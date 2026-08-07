package com.eina.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.repository.StatsRepository
import com.eina.app.domain.SessionSummary
import com.eina.app.domain.currentStreak
import com.eina.app.domain.epochMillisToLocalDate
import com.eina.app.domain.summarizeSessions
import com.eina.app.domain.trainingDays
import com.eina.app.domain.volumeByDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardUiState(
    val weekSessions: Int = 0,
    val weekVolumeKg: Double = 0.0,
    val weekVolumeByDay: List<Float> = List(7) { 0f },
    val streakDays: Int = 0,
    val lastSession: SessionSummary? = null,
    val recentSessions: List<SessionSummary> = emptyList()
)

class DashboardViewModel(repository: StatsRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.observeCompletedSets()
        .map { rows ->
            val today = LocalDate.now()
            val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val sessions = summarizeSessions(rows)
            val weekSessions = sessions.filter {
                !epochMillisToLocalDate(it.startTime).isBefore(startOfWeek)
            }
            val volumePerDay = volumeByDay(rows)

            DashboardUiState(
                weekSessions = weekSessions.size,
                weekVolumeKg = weekSessions.sumOf { it.volumeKg },
                weekVolumeByDay = (0..6).map { offset ->
                    (volumePerDay[startOfWeek.plusDays(offset.toLong())] ?: 0.0).toFloat()
                },
                streakDays = currentStreak(trainingDays(rows), today),
                lastSession = sessions.firstOrNull(),
                recentSessions = sessions.take(5)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
