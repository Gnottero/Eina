package com.eina.app.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.repository.RoutineRepository
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineListViewModel(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    val routines: StateFlow<List<RoutineEntity>> = routineRepository.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSession(routineId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startSessionFromRoutine(routineId))
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch { routineRepository.deleteRoutine(routine) }
    }
}
