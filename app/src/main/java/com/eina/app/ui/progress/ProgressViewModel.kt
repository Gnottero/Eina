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

/**
 * Span the headline number is measured over. A week says whether today went well and a year says
 * whether the training is going anywhere; the app used to answer only the first.
 */
enum class ProgressPeriod { WEEK, MONTH, YEAR }

data class ProgressUiState(
    val period: ProgressPeriod = ProgressPeriod.WEEK,
    /** Volume of the selected period, and of the one before it: together they make the delta. */
    val periodVolumeKg: Double = 0.0,
    val previousVolumeKg: Double = 0.0,
    /** Bars of the period: days in a week, weeks in a month, months in a year. */
    val chartValues: List<Float> = emptyList(),
    /** Bar the period is currently inside; -1 outside the current period. */
    val chartHighlight: Int = -1,
    val volumeByDay: Map<LocalDate, Double> = emptyMap(),
    val personalRecords: List<PrRecord> = emptyList(),
    val latestBodyweightKg: Double? = null,
    val bodyweightSeries: List<Float> = emptyList(),
    val streakWeeks: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalSets: Int = 0,
    val totalSessions: Int = 0
) {
    /**
     * Percentage change against the previous period, rounded. Null when there is nothing to compare
     * against: "+100%" over an empty first week says nothing.
     */
    val deltaPercent: Int?
        get() = if (previousVolumeKg <= 0.0) null else {
            (((periodVolumeKg - previousVolumeKg) / previousVolumeKg) * 100).toInt()
        }
}

class ProgressViewModel(repository: StatsRepository) : ViewModel() {

    private val period = MutableStateFlow(ProgressPeriod.WEEK)

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.observeCompletedSets(),
        repository.observeBodyMetrics(),
        period
    ) { rows, bodyMetrics, selected ->
        // The date is re-read on every emission: fixing it at ViewModel creation left the week
        // stuck on yesterday for an app kept open past midnight.
        val today = LocalDate.now()
        val volumePerDay = volumeByDay(rows)
        val setsPerDay = setsByDay(rows)

        ProgressUiState(
            period = selected,
            periodVolumeKg = volumeIn(volumePerDay, periodStart(selected, today), today),
            previousVolumeKg = volumeIn(
                volumePerDay,
                periodStart(selected, previousPeriodDay(selected, today)),
                periodEnd(selected, previousPeriodDay(selected, today))
            ),
            chartValues = chartValues(volumePerDay, selected, today),
            chartHighlight = chartHighlight(selected, today),
            volumeByDay = volumePerDay,
            personalRecords = personalRecords(rows),
            latestBodyweightKg = bodyMetrics.firstOrNull()?.bodyweightKg,
            // Oldest first, so the line reads left to right; the repository hands them back newest
            // first because that is what the weight screen lists.
            bodyweightSeries = bodyMetrics.takeLast(BODYWEIGHT_POINTS)
                .reversed()
                .map { it.bodyweightKg.toFloat() },
            streakWeeks = currentStreak(trainingDays(rows), today),
            totalVolumeKg = volumePerDay.values.sum(),
            totalSets = setsPerDay.values.sum(),
            totalSessions = rows.mapTo(HashSet()) { it.sessionId }.size
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectPeriod(index: Int) {
        period.value = ProgressPeriod.entries.getOrElse(index) { ProgressPeriod.WEEK }
    }

    private companion object {
        /** Last weigh-ins drawn in the hero island; the full series lives on the weight screen. */
        const val BODYWEIGHT_POINTS = 12

        fun periodStart(period: ProgressPeriod, day: LocalDate): LocalDate = when (period) {
            ProgressPeriod.WEEK -> day.minusDays((day.dayOfWeek.value - 1).toLong())
            ProgressPeriod.MONTH -> day.withDayOfMonth(1)
            ProgressPeriod.YEAR -> day.withDayOfYear(1)
        }

        fun periodEnd(period: ProgressPeriod, day: LocalDate): LocalDate = when (period) {
            ProgressPeriod.WEEK -> periodStart(period, day).plusDays(6)
            ProgressPeriod.MONTH -> day.withDayOfMonth(day.lengthOfMonth())
            ProgressPeriod.YEAR -> day.withDayOfYear(day.lengthOfYear())
        }

        /** Any day inside the previous period; the bounds are then derived from it. */
        fun previousPeriodDay(period: ProgressPeriod, day: LocalDate): LocalDate = when (period) {
            ProgressPeriod.WEEK -> day.minusWeeks(1)
            ProgressPeriod.MONTH -> day.minusMonths(1)
            ProgressPeriod.YEAR -> day.minusYears(1)
        }

        fun volumeIn(byDay: Map<LocalDate, Double>, from: LocalDate, to: LocalDate): Double =
            byDay.entries
                .filter { !it.key.isBefore(from) && !it.key.isAfter(to) }
                .sumOf { it.value }

        /**
         * One bar per unit of the period: seven days, the weeks the month is made of, twelve
         * months. A month drawn day by day would be thirty bars two pixels wide.
         */
        fun chartValues(
            byDay: Map<LocalDate, Double>,
            period: ProgressPeriod,
            today: LocalDate
        ): List<Float> {
            val start = periodStart(period, today)
            return when (period) {
                ProgressPeriod.WEEK -> (0..6).map { offset ->
                    (byDay[start.plusDays(offset.toLong())] ?: 0.0).toFloat()
                }
                ProgressPeriod.MONTH -> {
                    val weeks = ((today.lengthOfMonth() - 1) / 7) + 1
                    (0 until weeks).map { week ->
                        val from = start.plusDays((week * 7).toLong())
                        val to = minOf(from.plusDays(6), periodEnd(period, today))
                        volumeIn(byDay, from, to).toFloat()
                    }
                }
                ProgressPeriod.YEAR -> (1..12).map { month ->
                    val from = start.withMonth(month)
                    volumeIn(byDay, from, from.withDayOfMonth(from.lengthOfMonth())).toFloat()
                }
            }
        }

        fun chartHighlight(period: ProgressPeriod, today: LocalDate): Int = when (period) {
            ProgressPeriod.WEEK -> today.dayOfWeek.value - 1
            ProgressPeriod.MONTH -> (today.dayOfMonth - 1) / 7
            ProgressPeriod.YEAR -> today.monthValue - 1
        }
    }
}
