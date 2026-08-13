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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProgressUiState(
    val weekDates: List<LocalDate> = emptyList(),
    val selectedDayIndex: Int = 0,
    val weekVolumeByDay: List<Float> = List(7) { 0f },
    val volumeByDay: Map<LocalDate, Double> = emptyMap(),
    val personalRecords: List<PrRecord> = emptyList(),
    val latestBodyweightKg: Double? = null,
    val streakWeeks: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalSets: Int = 0,
    val totalSessions: Int = 0
)

class ProgressViewModel(repository: StatsRepository) : ViewModel() {

    private val selectedDayIndex = MutableStateFlow(LocalDate.now().dayOfWeek.value - 1)

    /**
     * Tutto quello che dipende dallo storico e non dal giorno scelto. Sta in un flusso suo perche'
     * toccare un giorno della settimana non deve far ricalcolare record, streak e volumi di tutto
     * lo storico: cambia solo quale colonna e' evidenziata.
     */
    private val history: StateFlow<ProgressUiState> = combine(
        repository.observeCompletedSets(),
        repository.observeBodyMetrics()
    ) { rows, bodyMetrics ->
        // La data si rilegge a ogni emissione: fissarla alla nascita del ViewModel lasciava la
        // settimana ferma a ieri su un'app rimasta aperta oltre la mezzanotte.
        val today = LocalDate.now()
        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val volumePerDay = volumeByDay(rows)
        val setsPerDay = setsByDay(rows)
        val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }

        ProgressUiState(
            weekDates = weekDates,
            weekVolumeByDay = weekDates.map { (volumePerDay[it] ?: 0.0).toFloat() },
            volumeByDay = volumePerDay,
            personalRecords = personalRecords(rows),
            latestBodyweightKg = bodyMetrics.firstOrNull()?.bodyweightKg,
            streakWeeks = currentStreak(trainingDays(rows), today),
            totalVolumeKg = volumePerDay.values.sum(),
            totalSets = setsPerDay.values.sum(),
            totalSessions = rows.mapTo(HashSet()) { it.sessionId }.size
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    val uiState: StateFlow<ProgressUiState> = combine(history, selectedDayIndex) { state, dayIndex ->
        state.copy(selectedDayIndex = dayIndex)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
    }
}
