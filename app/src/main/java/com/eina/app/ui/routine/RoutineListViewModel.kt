package com.eina.app.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.repository.RoutineRepository
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A routine with just enough content for the list card. */
data class RoutineCardUi(
    val routine: RoutineEntity,
    val exerciseNames: List<ExerciseName>,
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
        routineRepository.observeRoutinePreviews(),
        routineRepository.observeRoutineSetCounts()
    ) { routines, previews, setCounts ->
        val byRoutine = previews.groupBy { it.routineId }
        // Sets are counted from the routine_sets rows: a routine can hold exercises with different
        // set counts, so there is no single number to multiply.
        val setsByRoutine = setCounts.associate { it.routineId to it.setCount }
        routines.map { routine ->
            val rows = byRoutine[routine.id].orEmpty()
            RoutineCardUi(
                routine = routine,
                exerciseNames = rows.map { it.exerciseName },
                setCount = setsByRoutine[routine.id] ?: 0
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

    /** Builds the routine exchange file and hands it to the caller, which opens the chooser. */
    fun exportRoutine(routineId: Long, onReady: (String?) -> Unit) {
        viewModelScope.launch { onReady(routineRepository.exportRoutine(routineId)) }
    }

    /** Imports a routine from an exchange file; [onDone] receives false if the file is invalid. */
    fun importRoutine(json: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(routineRepository.importRoutine(json) != null) }
    }
}
