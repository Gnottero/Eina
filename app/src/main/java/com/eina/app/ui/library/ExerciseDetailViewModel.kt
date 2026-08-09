package com.eina.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.repository.StatsRepository
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.domain.ProgressPoint
import com.eina.app.domain.exerciseProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    private val repository: WorkoutRepository,
    private val statsRepository: StatsRepository,
    private val exerciseId: Long
) : ViewModel() {
    private val _exercise = MutableStateFlow<ExerciseEntity?>(null)
    val exercise: StateFlow<ExerciseEntity?> = _exercise.asStateFlow()

    /** Progressione dell'esercizio: un punto per allenamento, dal piu' vecchio al piu' recente. */
    private val _progress = MutableStateFlow<List<ProgressPoint>>(emptyList())
    val progress: StateFlow<List<ProgressPoint>> = _progress.asStateFlow()

    init {
        viewModelScope.launch {
            _exercise.value = repository.getExercise(exerciseId)
        }
        statsRepository.observeExerciseSets(exerciseId)
            .onEach { rows -> _progress.value = exerciseProgress(rows) }
            .launchIn(viewModelScope)
    }
}
