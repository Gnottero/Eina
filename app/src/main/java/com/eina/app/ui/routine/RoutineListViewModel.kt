package com.eina.app.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.repository.RoutineRepository
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Una routine con quel tanto di contenuto che serve alla card dell'elenco. */
data class RoutineCardUi(
    val routine: RoutineEntity,
    val exerciseNames: List<String>,
    val setCount: Int
) {
    val exerciseCount: Int get() = exerciseNames.size
}

class RoutineListViewModel(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    val routines: StateFlow<List<RoutineCardUi>> = combine(
        routineRepository.observeRoutines(),
        routineRepository.observeRoutinePreviews()
    ) { routines, previews ->
        val byRoutine = previews.groupBy { it.routineId }
        routines.map { routine ->
            val rows = byRoutine[routine.id].orEmpty()
            RoutineCardUi(
                routine = routine,
                exerciseNames = rows.map { it.exerciseName },
                setCount = rows.sumOf { it.targetSets }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSession(routineId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startSessionFromRoutine(routineId))
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch { routineRepository.deleteRoutine(routine) }
    }
}
