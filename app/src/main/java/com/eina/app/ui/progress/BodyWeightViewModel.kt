package com.eina.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.BodyMetricEntity
import com.eina.app.data.repository.StatsRepository
import com.eina.app.ui.components.MAX_WEIGHT_KG
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BodyWeightUiState(
    val metrics: List<BodyMetricEntity> = emptyList(),
    val latestKg: Double? = null,
    val deltaKg: Double? = null
)

class BodyWeightViewModel(private val repository: StatsRepository) : ViewModel() {

    val uiState: StateFlow<BodyWeightUiState> = repository.observeBodyMetrics()
        .map { metrics ->
            BodyWeightUiState(
                metrics = metrics,
                latestKg = metrics.firstOrNull()?.bodyweightKg,
                // Delta against the previous measurement: the sign shows direction, not judgement.
                deltaKg = if (metrics.size >= 2) {
                    metrics[0].bodyweightKg - metrics[1].bodyweightKg
                } else {
                    null
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyWeightUiState())

    fun addMeasurement(input: String) {
        val kg = input.replace(',', '.').toDoubleOrNull() ?: return
        if (kg <= 0.0 || kg > MAX_WEIGHT_KG) return
        viewModelScope.launch { repository.addBodyMetric(kg) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteBodyMetric(id) }
    }
}
