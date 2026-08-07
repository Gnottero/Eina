package com.eina.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.repository.StatsRepository
import com.eina.app.domain.PrRecord
import com.eina.app.domain.currentStreak
import com.eina.app.domain.personalRecords
import com.eina.app.domain.setsByDay
import com.eina.app.domain.trainingDays
import com.eina.app.domain.volumeByDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProgressUiState(
    val weekDates: List<LocalDate> = emptyList(),
    val selectedDayIndex: Int = 0,
    val selectedDayVolumeKg: Double = 0.0,
    val selectedDaySets: Int = 0,
    val weekVolumeByDay: List<Float> = List(7) { 0f },
    val volumeByDay: Map<LocalDate, Double> = emptyMap(),
    val personalRecords: List<PrRecord> = emptyList(),
    val latestBodyweightKg: Double? = null,
    val streakWeeks: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalSessions: Int = 0
)

class ProgressViewModel(repository: StatsRepository) : ViewModel() {

    private val today = LocalDate.now()
    private val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
    private val selectedDayIndex = MutableStateFlow(today.dayOfWeek.value - 1)

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.observeCompletedSets(),
        repository.observeBodyMetrics(),
        selectedDayIndex
    ) { rows, bodyMetrics, dayIndex ->
        val volumePerDay = volumeByDay(rows)
        val setsPerDay = setsByDay(rows)
        val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
        val selectedDate = weekDates[dayIndex]

        ProgressUiState(
            weekDates = weekDates,
            selectedDayIndex = dayIndex,
            selectedDayVolumeKg = volumePerDay[selectedDate] ?: 0.0,
            selectedDaySets = setsPerDay[selectedDate] ?: 0,
            weekVolumeByDay = weekDates.map { (volumePerDay[it] ?: 0.0).toFloat() },
            volumeByDay = volumePerDay,
            personalRecords = personalRecords(rows),
            latestBodyweightKg = bodyMetrics.firstOrNull()?.bodyweightKg,
            streakWeeks = currentStreak(trainingDays(rows), today),
            totalVolumeKg = volumePerDay.values.sum(),
            totalSessions = rows.map { it.sessionId }.distinct().size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
    }
}
