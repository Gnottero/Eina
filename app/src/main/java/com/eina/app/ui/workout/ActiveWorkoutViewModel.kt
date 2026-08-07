package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(sessionId = sessionId))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        repository.observeExercises()
            .onEach { list -> _uiState.update { it.copy(availableExercises = list) } }
            .launchIn(viewModelScope)
        loadExistingExercises()
    }

    private fun loadExistingExercises() {
        viewModelScope.launch {
            val workoutExercises = repository.getSessionExercises(sessionId)
            workoutExercises.forEach { we ->
                val exercise = repository.getExercise(we.exerciseId) ?: return@forEach
                upsertExerciseUi(we, exercise)
            }
        }
    }

    private suspend fun upsertExerciseUi(workoutExercise: WorkoutExerciseEntity, exercise: ExerciseEntity) {
        val sets = repository.getSetsForWorkoutExercise(workoutExercise.id).map { it.toUi() }
        val lastTimeSets = repository.getLastTimeSets(exercise.id, sessionId)
        val exerciseUi = SessionExerciseUi(
            workoutExerciseId = workoutExercise.id,
            exerciseId = exercise.id,
            name = exercise.name,
            weightType = exercise.weightType,
            order = workoutExercise.order,
            sets = sets,
            lastTimeSets = lastTimeSets
        )
        _uiState.update { state ->
            val others = state.exercises.filterNot { it.workoutExerciseId == workoutExercise.id }
            state.copy(exercises = (others + exerciseUi).sortedBy { it.order })
        }
    }

    private suspend fun refreshSets(workoutExerciseId: Long) {
        val sets = repository.getSetsForWorkoutExercise(workoutExerciseId).map { it.toUi() }
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId == workoutExerciseId) ex.copy(sets = sets) else ex
                }
            )
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val order = _uiState.value.exercises.size
            val workoutExerciseId = repository.addExercise(sessionId, exercise.id, order)
            val workoutExercise = WorkoutExerciseEntity(id = workoutExerciseId, sessionId = sessionId, exerciseId = exercise.id, order = order)
            upsertExerciseUi(workoutExercise, exercise)
        }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            repository.removeExercise(workoutExerciseId)
            val remaining = _uiState.value.exercises.filterNot { it.workoutExerciseId == workoutExerciseId }
            _uiState.update { it.copy(exercises = remaining) }
            persistExerciseOrder(remaining)
        }
    }

    fun moveExercise(workoutExerciseId: Long, delta: Int) {
        viewModelScope.launch {
            val current = _uiState.value.exercises
            val index = current.indexOfFirst { it.workoutExerciseId == workoutExerciseId }
            val targetIndex = index + delta
            if (index < 0 || targetIndex !in current.indices) return@launch
            val reordered = current.toMutableList().apply { add(targetIndex, removeAt(index)) }
            _uiState.update { it.copy(exercises = reordered) }
            persistExerciseOrder(reordered)
        }
    }

    private suspend fun persistExerciseOrder(exercises: List<SessionExerciseUi>) {
        val entities = exercises.mapIndexed { index, ex ->
            WorkoutExerciseEntity(id = ex.workoutExerciseId, sessionId = sessionId, exerciseId = ex.exerciseId, order = index)
        }
        repository.reorderExercises(entities)
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { index, ex -> ex.copy(order = index) })
        }
    }

    fun addSet(workoutExerciseId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val nextIndex = exercise.sets.size
            val restSeconds = exercise.sets.lastOrNull()?.restSecondsPlanned ?: 90
            repository.addSet(workoutExerciseId, nextIndex, restSeconds)
            refreshSets(workoutExerciseId)
        }
    }

    fun removeSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            repository.removeSet(setId)
            refreshSets(workoutExerciseId)
        }
    }

    fun updateSetValues(workoutExerciseId: Long, setId: Long, actualReps: Int?, weight: Double?) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val set = exercise.sets.find { it.id == setId } ?: return
        val updated = set.copy(actualReps = actualReps, weight = weight)
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId != workoutExerciseId) ex
                    else ex.copy(sets = ex.sets.map { if (it.id == setId) updated else it })
                }
            )
        }
        viewModelScope.launch {
            repository.updateSet(updated.toEntity(workoutExerciseId))
        }
    }

    fun completeSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val set = exercise.sets.find { it.id == setId } ?: return@launch
            val completed = repository.completeSet(set.toEntity(workoutExerciseId), exercise.exerciseId, exercise.weightType)
            refreshSets(workoutExerciseId)
            if (!completed.isWarmup) startRestTimer(completed.restSecondsPlanned)
        }
    }

    private fun startRestTimer(totalSeconds: Int) {
        if (totalSeconds <= 0) return
        timerJob?.cancel()
        _uiState.update { it.copy(timer = TimerUi(totalSeconds, totalSeconds)) }
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _uiState.value.timer ?: break
                val next = current.remainingSeconds - 1
                if (next <= 0) {
                    _uiState.update { it.copy(timer = null) }
                    break
                }
                _uiState.update { it.copy(timer = current.copy(remainingSeconds = next)) }
            }
        }
    }

    fun adjustTimer(deltaSeconds: Int) {
        val current = _uiState.value.timer ?: return
        val next = current.remainingSeconds + deltaSeconds
        if (next <= 0) {
            skipTimer()
        } else {
            _uiState.update { it.copy(timer = current.copy(remainingSeconds = next, totalSeconds = maxOf(current.totalSeconds, next))) }
        }
    }

    fun skipTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timer = null) }
    }

    fun finishWorkout(onFinished: () -> Unit) {
        viewModelScope.launch {
            skipTimer()
            repository.finishSession(sessionId)
            _uiState.update { it.copy(isFinished = true) }
            onFinished()
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

private fun SetEntryEntity.toUi() = SessionSetUi(
    id = id,
    setIndex = setIndex,
    targetReps = targetReps,
    actualReps = actualReps,
    weight = weight,
    restSecondsPlanned = restSecondsPlanned,
    isWarmup = isWarmup,
    completedAt = completedAt,
    isPR = isPR,
    bodyweightSnapshotKg = bodyweightSnapshotKg
)

private fun SessionSetUi.toEntity(workoutExerciseId: Long) = SetEntryEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setIndex = setIndex,
    targetReps = targetReps,
    actualReps = actualReps,
    weight = weight,
    restSecondsPlanned = restSecondsPlanned,
    isWarmup = isWarmup,
    completedAt = completedAt,
    isPR = isPR,
    bodyweightSnapshotKg = bodyweightSnapshotKg
)
