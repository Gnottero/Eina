package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.domain.volumeForSet
import com.eina.app.ui.feedback.WorkoutFeedback
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
    private val feedback: WorkoutFeedback,
    private val sessionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(sessionId = sessionId))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /** Target della routine di partenza, per exerciseId: alimentano i segnaposto dei campi. */
    private var routineTargets: Map<Long, RoutineExerciseEntity> = emptyMap()

    init {
        repository.observeExercises()
            .onEach { list -> _uiState.update { it.copy(availableExercises = list) } }
            .launchIn(viewModelScope)
        loadSession()
        startElapsedTicker()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session != null) {
                _uiState.update { it.copy(startTime = session.startTime) }
                session.routineId?.let { routineTargets = repository.getRoutineTargets(it) }
            }
            repository.getSessionExercises(sessionId).forEach { we ->
                val exercise = repository.getExercise(we.exerciseId) ?: return@forEach
                upsertExerciseUi(we, exercise)
            }
        }
    }

    private fun startElapsedTicker() {
        viewModelScope.launch {
            while (isActive) {
                val start = _uiState.value.startTime
                val elapsed = ((System.currentTimeMillis() - start) / 1000).toInt().coerceAtLeast(0)
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
                delay(1000)
            }
        }
    }

    private suspend fun upsertExerciseUi(workoutExercise: WorkoutExerciseEntity, exercise: ExerciseEntity) {
        val lastTimeSets = repository.getLastTimeSets(exercise.id, sessionId)
        val sets = repository.getSetsForWorkoutExercise(workoutExercise.id)
            .map { it.toUi(exercise.id, lastTimeSets) }
        val exerciseUi = SessionExerciseUi(
            workoutExerciseId = workoutExercise.id,
            exerciseId = exercise.id,
            name = exercise.name,
            weightType = exercise.weightType,
            order = workoutExercise.order,
            restSeconds = sets.firstOrNull()?.restSecondsPlanned
                ?: routineTargets[exercise.id]?.restSeconds
                ?: DEFAULT_REST_SECONDS,
            sets = sets,
            lastTimeSets = lastTimeSets
        )
        _uiState.update { state ->
            val others = state.exercises.filterNot { it.workoutExerciseId == workoutExercise.id }
            state.copy(exercises = (others + exerciseUi).sortedBy { it.order })
        }
        recomputeVolume()
    }

    private suspend fun refreshSets(workoutExerciseId: Long) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val sets = repository.getSetsForWorkoutExercise(workoutExerciseId)
            .map { it.toUi(exercise.exerciseId, exercise.lastTimeSets) }
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId == workoutExerciseId) ex.copy(sets = sets) else ex
                }
            )
        }
        recomputeVolume()
    }

    private fun recomputeVolume() {
        _uiState.update { state ->
            val volume = state.exercises.sumOf { ex ->
                ex.sets.filter { it.completedAt != null && !it.isWarmup }
                    .sumOf { volumeForSet(ex.weightType, it.toEntity(ex.workoutExerciseId)) }
            }
            state.copy(volumeKg = volume)
        }
    }

    private fun SetEntryEntity.toUi(exerciseId: Long, lastTimeSets: List<SetEntryEntity>) = SessionSetUi(
        id = id,
        setIndex = setIndex,
        targetReps = targetReps ?: routineTargets[exerciseId]?.targetReps,
        actualReps = actualReps,
        weight = weight,
        restSecondsPlanned = restSecondsPlanned,
        isWarmup = isWarmup,
        completedAt = completedAt,
        isPR = isPR,
        bodyweightSnapshotKg = bodyweightSnapshotKg,
        targetWeight = routineTargets[exerciseId]?.targetWeight,
        previous = lastTimeSets.getOrNull(setIndex)
    )

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val order = _uiState.value.exercises.size
            val rest = routineTargets[exercise.id]?.restSeconds ?: DEFAULT_REST_SECONDS
            val workoutExerciseId = repository.addExercise(sessionId, exercise.id, order, rest)
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
            recomputeVolume()
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
            repository.addSet(workoutExerciseId, exercise.sets.size, exercise.restSeconds)
            refreshSets(workoutExerciseId)
        }
    }

    fun removeSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            repository.removeSet(setId)
            refreshSets(workoutExerciseId)
        }
    }

    /** Il recupero si imposta per esercizio e si propaga a tutte le sue serie non ancora svolte. */
    fun setRestSeconds(workoutExerciseId: Long, seconds: Int) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val safeSeconds = seconds.coerceIn(0, 600)
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId != workoutExerciseId) ex
                    else ex.copy(
                        restSeconds = safeSeconds,
                        sets = ex.sets.map { if (it.completedAt == null) it.copy(restSecondsPlanned = safeSeconds) else it }
                    )
                }
            )
        }
        viewModelScope.launch {
            exercise.sets.filter { it.completedAt == null }.forEach { set ->
                repository.updateSet(set.copy(restSecondsPlanned = safeSeconds).toEntity(workoutExerciseId))
            }
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
        recomputeVolume()
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
            feedback.haptic()
            if (!completed.isWarmup) startRestTimer(completed.restSecondsPlanned)
        }
    }

    /** Annulla il completamento: la serie torna modificabile e esce dal volume. */
    fun uncompleteSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val set = exercise.sets.find { it.id == setId } ?: return@launch
            repository.updateSet(set.copy(completedAt = null, isPR = false).toEntity(workoutExerciseId))
            refreshSets(workoutExerciseId)
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
                    feedback.restTimerFinished()
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

    private companion object {
        const val DEFAULT_REST_SECONDS = 90
    }
}

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
