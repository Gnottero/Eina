package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.usesDecimalField
import com.eina.app.data.health.WorkoutHealthSync
import com.eina.app.data.repository.RoutineTarget
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.domain.RoutineChange
import com.eina.app.domain.Superset
import com.eina.app.domain.volumeForSet
import com.eina.app.ui.components.MAX_WEIGHT_KG
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
    private val restTimer: RestTimerController,
    private val healthSync: WorkoutHealthSync,
    private val sessionId: Long,
    /**
     * Recorded workout opened from the history for correction. The clock does not run (duration is
     * the saved one), completing a set does not start the rest timer, and the completion instant
     * stays inside the day of the workout instead of being "now".
     */
    private val isPast: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(sessionId = sessionId, isPast = isPast))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    /** Targets of the source routine, by exerciseId; they feed the field placeholders. */
    private var routineTargets: Map<Long, RoutineTarget> = emptyMap()

    /** Finish request waiting for the routine-sync answer; see [finishWorkout]. */
    private var pendingFinish: PendingFinish? = null

    /**
     * Exercises removed from the session while editing. They are no longer part of it when saving,
     * but a removed set may have held their PR, so their flags must be recomputed too.
     */
    private val touchedExerciseIds = mutableSetOf<Long>()

    /** Pending PR recomputation after editing a completed set; see [schedulePrRecompute]. */
    private var prRecomputeJob: Job? = null

    init {
        repository.observeExercises()
            .onEach { list -> _uiState.update { it.copy(availableExercises = list) } }
            .launchIn(viewModelScope)
        // The rest countdown lives in the shared controller and is only mirrored here, so
        // re-entering the screen finds it still running.
        restTimer.state
            .onEach { timer ->
                _uiState.update {
                    it.copy(timer = timer?.let { t -> TimerUi(t.totalSeconds, t.remainingSeconds) })
                }
            }
            .launchIn(viewModelScope)
        loadSession()
        if (!isPast) startElapsedTicker()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session != null) {
                _uiState.update {
                    it.copy(
                        startTime = session.startTime,
                        // For a past workout the duration is the recorded one, not the time since
                        // then: the ticker does not run, so it is written here.
                        elapsedSeconds = if (!isPast) it.elapsedSeconds else {
                            (((session.endTime ?: session.startTime) - session.startTime) / 1000).toInt()
                        }
                    )
                }
                session.routineId?.let { routineId ->
                    routineTargets = repository.getRoutineTargets(routineId)
                    val routine = repository.getRoutine(routineId)
                    _uiState.update {
                        it.copy(
                            routineId = routineId,
                            routineName = routine?.name,
                            playlistUri = routine?.linkedPlaylistUri,
                            playlistType = routine?.linkedPlaylistType
                        )
                    }
                }
            }
            // Published in a single update: one update per exercise rebuilt and re-sorted the whole
            // list on every iteration, and computed volume on a half-loaded workout.
            val loaded = repository.getSessionExercises(sessionId).mapNotNull { we ->
                repository.getExercise(we.exerciseId)?.let { buildExerciseUi(we, it) }
            }
            _uiState.update { it.copy(exercises = loaded.sortedBy { ex -> ex.order }) }
            recomputeVolume()
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
        val exerciseUi = buildExerciseUi(workoutExercise, exercise)
        _uiState.update { state ->
            val others = state.exercises.filterNot { it.workoutExerciseId == workoutExercise.id }
            state.copy(exercises = (others + exerciseUi).sortedBy { it.order })
        }
        recomputeVolume()
    }

    /** Session row ready to render, with placeholders and "last time" values already resolved. */
    private suspend fun buildExerciseUi(
        workoutExercise: WorkoutExerciseEntity,
        exercise: ExerciseEntity
    ): SessionExerciseUi {
        val lastTimeSets = repository.getLastTimeSets(exercise.id, sessionId)
        val (lastWeight, lastReps) = repository.getLastRecordedValues(exercise.id)
        val sets = withSuggestions(
            repository.getSetsForWorkoutExercise(workoutExercise.id).map { it.toUi(exercise.id, lastTimeSets) },
            lastWeight,
            lastReps,
            exercise.weightType
        )
        return SessionExerciseUi(
            workoutExerciseId = workoutExercise.id,
            exerciseId = exercise.id,
            name = exercise.exerciseName(),
            weightType = exercise.weightType,
            bodyweightFactor = exercise.bodyweightFactor,
            order = workoutExercise.order,
            notes = workoutExercise.notes,
            supersetGroup = workoutExercise.supersetGroup,
            // Rest belongs to the session row: reading it from the first set made it fall back to
            // the routine value as soon as that set was completed.
            restSeconds = workoutExercise.restSeconds,
            sets = sets,
            lastTimeSets = lastTimeSets,
            lastRecordedWeight = lastWeight,
            lastRecordedReps = lastReps
        )
    }

    private suspend fun refreshSets(workoutExerciseId: Long) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val sets = withSuggestions(
            repository.getSetsForWorkoutExercise(workoutExerciseId)
                .map { it.toUi(exercise.exerciseId, exercise.lastTimeSets) },
            exercise.lastRecordedWeight,
            exercise.lastRecordedReps,
            exercise.weightType
        )
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
                ex.sets.filter { it.completedAt != null && it.setType.countsAsWorking }
                    .sumOf { volumeForSet(ex.weightType, it.toEntity(ex.workoutExerciseId), ex.bodyweightFactor) }
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
        setType = setType,
        completedAt = completedAt,
        isPR = isPR,
        bodyweightSnapshotKg = bodyweightSnapshotKg,
        targetWeight = routineTargets[exerciseId]?.targetWeight,
        previous = lastTimeSets.getOrNull(setIndex)
    )

    /**
     * Fills the suggested values set by set. Fallback order: the same set of the last workout, then
     * the routine target, then the last value seen (in this session or in the exercise history), so
     * a weight is proposed even when the previous workout did not record one.
     */
    private fun withSuggestions(
        sets: List<SessionSetUi>,
        fallbackWeight: Double?,
        fallbackReps: Int?,
        weightType: WeightType
    ): List<SessionSetUi> {
        var lastWeight: Double? = if (weightType.usesDecimalField) fallbackWeight else null
        var lastReps: Int? = fallbackReps
        return sets.map { set ->
            // Without a decimal field no load is suggested: completing the set would record a
            // value the user never saw. For distance the field exists and holds kilometres.
            val suggestedWeight = if (!weightType.usesDecimalField) null
            else set.previous?.weight ?: set.targetWeight ?: lastWeight
            val suggestedReps = set.previous?.actualReps ?: set.targetReps ?: lastReps
            lastWeight = set.weight ?: suggestedWeight ?: lastWeight
            lastReps = set.actualReps ?: suggestedReps ?: lastReps
            set.copy(suggestedWeight = suggestedWeight, suggestedReps = suggestedReps)
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val order = _uiState.value.exercises.size
            val rest = routineTargets[exercise.id]?.restSeconds ?: DEFAULT_REST_SECONDS
            val workoutExerciseId = repository.addExercise(sessionId, exercise.id, order, rest)
            val workoutExercise = WorkoutExerciseEntity(
                id = workoutExerciseId,
                sessionId = sessionId,
                exerciseId = exercise.id,
                order = order,
                restSeconds = rest
            )
            upsertExerciseUi(workoutExercise, exercise)
        }
    }

    /**
     * Swaps the movement of a row without moving it: superset, rest and note are kept. Recorded
     * values are cleared, since they belonged to another exercise
     * (see [WorkoutRepository.replaceExercise]).
     */
    fun replaceExercise(workoutExerciseId: Long, exercise: ExerciseEntity) {
        viewModelScope.launch {
            _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId }
                ?.let { touchedExerciseIds += it.exerciseId }
            if (!repository.replaceExercise(workoutExerciseId, exercise.id)) return@launch
            val entity = repository.getSessionExercises(sessionId)
                .find { it.id == workoutExerciseId } ?: return@launch
            upsertExerciseUi(entity, exercise)
        }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId }
                ?.let { touchedExerciseIds += it.exerciseId }
            repository.removeExercise(workoutExerciseId)
            // A superset left with a single member is no longer a superset.
            val remaining = dissolveOrphanSupersets(
                _uiState.value.exercises.filterNot { it.workoutExerciseId == workoutExerciseId }
            )
            _uiState.update { it.copy(exercises = remaining) }
            persistExerciseOrder(remaining)
            recomputeVolume()
        }
    }

    /**
     * Order chosen by dragging in the reorder sheet. It arrives complete, so it is written in one
     * go instead of swapping neighbours; the ids come from the flattened blocks, which keeps
     * superset members adjacent.
     */
    fun applyOrder(orderedWorkoutExerciseIds: List<Long>) {
        viewModelScope.launch {
            val byId = _uiState.value.exercises.associateBy { it.workoutExerciseId }
            val reordered = orderedWorkoutExerciseIds.mapNotNull { byId[it] }
            // A partial order would rewrite the list dropping rows: do nothing instead.
            if (reordered.size != byId.size) return@launch
            if (reordered.map { it.workoutExerciseId } == _uiState.value.exercises.map { it.workoutExerciseId }) return@launch
            _uiState.update { it.copy(exercises = reordered) }
            persistExerciseOrder(reordered)
        }
    }

    private suspend fun persistExerciseOrder(exercises: List<SessionExerciseUi>) {
        val entities = exercises.mapIndexed { index, ex ->
            // The whole row is rewritten here, so the note must be carried over or it is lost.
            WorkoutExerciseEntity(
                id = ex.workoutExerciseId,
                sessionId = sessionId,
                exerciseId = ex.exerciseId,
                order = index,
                restSeconds = ex.restSeconds,
                notes = ex.notes,
                supersetGroup = ex.supersetGroup
            )
        }
        repository.reorderExercises(entities)
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { index, ex -> ex.copy(order = index) })
        }
    }

    /** Session-only exercise note: it does not touch the routine. */
    fun setExerciseNotes(workoutExerciseId: Long, notes: String?) {
        val clean = notes?.trim()?.ifBlank { null }
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId == workoutExerciseId) ex.copy(notes = clean) else ex
                }
            )
        }
        viewModelScope.launch { repository.setExerciseNotes(workoutExerciseId, clean) }
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
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId }
            repository.removeSet(setId)
            // The deleted set may have held the record: it goes back to the best one left.
            exercise?.let {
                touchedExerciseIds += it.exerciseId
                repository.recomputePrs(listOf(it.exerciseId))
            }
            refreshSets(workoutExerciseId)
            recomputeVolume()
        }
    }

    /**
     * Rest is set per exercise and propagates to its not-yet-completed sets. Inside a superset it
     * belongs to the round, so it is written to every member: otherwise the duration would depend
     * on who closes the round.
     */
    fun setRestSeconds(workoutExerciseId: Long, seconds: Int) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val safeSeconds = seconds.coerceIn(0, 600)
        val targets = _uiState.value.exercises.filter { it.sharesRound(exercise) }
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (targets.none { it.workoutExerciseId == ex.workoutExerciseId }) ex
                    else ex.copy(
                        restSeconds = safeSeconds,
                        sets = ex.sets.map { if (it.completedAt == null) it.copy(restSecondsPlanned = safeSeconds) else it }
                    )
                }
            )
        }
        viewModelScope.launch {
            targets.forEach { target ->
                repository.setExerciseRestSeconds(target.workoutExerciseId, safeSeconds)
                // Completed sets are left alone: their rest was already consumed.
                target.sets.filter { it.completedAt == null }.forEach { set ->
                    repository.updateSet(
                        set.copy(restSecondsPlanned = safeSeconds).toEntity(target.workoutExerciseId)
                    )
                }
            }
        }
    }

    /**
     * Superset of the exercise; a null [group] takes it out of the round. An exercise joining a
     * round moves next to its members and inherits their rest — see [Superset.regroup].
     */
    fun setSupersetGroup(workoutExerciseId: Long, group: Int?) {
        viewModelScope.launch {
            val current = _uiState.value.exercises
            val regrouped = Superset.regroup(
                current.map { Superset.Member(it.workoutExerciseId, it.supersetGroup) },
                workoutExerciseId,
                group
            )
            val byId = current.associateBy { it.workoutExerciseId }
            val reordered = regrouped.mapNotNull { member ->
                byId[member.id]?.copy(supersetGroup = member.group)
            }
            _uiState.update { it.copy(exercises = reordered) }
            persistExerciseOrder(reordered)
            // The round has a single rest value: a joining exercise takes the one of its members.
            if (group != null) {
                reordered.firstOrNull { it.supersetGroup == group && it.workoutExerciseId != workoutExerciseId }
                    ?.let { companion -> setRestSeconds(workoutExerciseId, companion.restSeconds) }
            }
        }
    }

    /** First free group number for a new superset. */
    fun nextSupersetGroup(): Int = Superset.nextGroup(_uiState.value.exercises.map { it.supersetGroup })

    private fun dissolveOrphanSupersets(exercises: List<SessionExerciseUi>): List<SessionExerciseUi> {
        val cleaned = Superset.dissolveOrphans(
            exercises.map { Superset.Member(it.workoutExerciseId, it.supersetGroup) }
        ).associateBy({ it.id }, { it.group })
        return exercises.map { ex -> ex.copy(supersetGroup = cleaned[ex.workoutExerciseId]) }
    }

    /**
     * Set type (warmup, normal, failure, drop). Changing it on a completed set moves it in or out
     * of the volume, so the total is recomputed at once; an assigned PR survives, except when the
     * set becomes a warmup, which cannot hold a record.
     */
    fun setSetType(workoutExerciseId: Long, setId: Long, type: SetType) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val set = exercise.sets.find { it.id == setId } ?: return
        val updated = set.copy(setType = type, isPR = set.isPR && type.countsAsWorking)
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
            // A warmup cannot hold a record, so the one it held goes back to the best set left.
            if (set.completedAt != null) {
                repository.recomputePrs(listOf(exercise.exerciseId))
                touchedExerciseIds += exercise.exerciseId
                refreshSets(workoutExerciseId)
            }
        }
    }

    fun updateSetValues(workoutExerciseId: Long, setId: Long, actualReps: Int?, weight: Double?) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val set = exercise.sets.find { it.id == setId } ?: return
        val updated = set.copy(actualReps = actualReps, weight = weight?.coerceIn(0.0, MAX_WEIGHT_KG))
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId != workoutExerciseId) ex
                    // Sets below inherit what is being typed here, so placeholders are recomputed
                    // on every keystroke and not only on a database refresh.
                    else ex.copy(
                        sets = withSuggestions(
                            ex.sets.map { if (it.id == setId) updated else it },
                            ex.lastRecordedWeight,
                            ex.lastRecordedReps,
                            ex.weightType
                        )
                    )
                }
            )
        }
        recomputeVolume()
        viewModelScope.launch {
            repository.updateSet(updated.toEntity(workoutExerciseId))
        }
        // Correcting a set already checked off can create or void a record, and the badge lived on
        // the row as it was written when the set was closed. The flags are recomputed, once the
        // typing settles: this runs on every keystroke.
        if (set.completedAt != null) schedulePrRecompute(workoutExerciseId, exercise.exerciseId)
    }

    /**
     * Recomputes the PR flags of an exercise after a debounce and redraws its rows. Debounced
     * because the caller is a keystroke: "8" on its way to "85" is a different record.
     */
    private fun schedulePrRecompute(workoutExerciseId: Long, exerciseId: Long) {
        touchedExerciseIds += exerciseId
        prRecomputeJob?.cancel()
        prRecomputeJob = viewModelScope.launch {
            delay(PR_RECOMPUTE_DEBOUNCE_MS)
            repository.recomputePrs(listOf(exerciseId))
            refreshSets(workoutExerciseId)
        }
    }

    fun completeSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val set = exercise.sets.find { it.id == setId } ?: return@launch
            // Completing an empty set would record 0 kg and 0 reps: the greyed-out suggestions
            // shown in the fields are recorded instead.
            val filled = set.copy(
                actualReps = set.actualReps ?: set.suggestedReps,
                weight = set.weight ?: set.suggestedWeight
            )
            val completed = repository.completeSet(
                set = filled.toEntity(workoutExerciseId),
                exerciseId = exercise.exerciseId,
                weightType = exercise.weightType,
                // When editing the past, "now" would push the set to the top of the history, so it
                // is placed inside the day the workout happened.
                completedAt = if (!isPast) System.currentTimeMillis() else pastCompletionTime(exercise, set)
            )
            refreshSets(workoutExerciseId)
            feedback.haptic()
            // Rest starts after every set, warmups included: a warmup is out of volume and PR,
            // but between it and the next set one still waits. In a superset rest belongs to the
            // round: it starts only once every member has completed the set with the same index.
            // On a past workout it never starts.
            if (!isPast && isRoundComplete(workoutExerciseId, set.setIndex)) {
                startRestTimer(completed.restSecondsPlanned)
            }
        }
    }

    /** Undoes completion: the set becomes editable again and leaves the volume. */
    fun uncompleteSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val set = exercise.sets.find { it.id == setId } ?: return@launch
            // Planned rest goes back to the exercise value: setRestSeconds only touches pending
            // sets, so a reopened set kept the old duration and restarted the timer with it.
            repository.updateSet(
                set.copy(
                    completedAt = null,
                    isPR = false,
                    restSecondsPlanned = exercise.restSeconds
                ).toEntity(workoutExerciseId)
            )
            // Reopening a set takes it out of the history: if it held the record, the record goes
            // back to the best set left.
            repository.recomputePrs(listOf(exercise.exerciseId))
            touchedExerciseIds += exercise.exerciseId
            refreshSets(workoutExerciseId)
        }
    }

    /**
     * Instant assigned to a set completed while editing a past workout: inside the recorded
     * duration, following the order exercises and sets appear in. PR assignment and the "last
     * time" lookup both depend on completion order.
     */
    private fun pastCompletionTime(exercise: SessionExerciseUi, set: SessionSetUi): Long {
        val state = _uiState.value
        val position = state.exercises.indexOfFirst { it.workoutExerciseId == exercise.workoutExerciseId }
            .coerceAtLeast(0)
        val offset = (position * MAX_SETS_PER_EXERCISE + set.setIndex) * 60_000L
        val end = state.startTime + state.elapsedSeconds * 1000L
        return (state.startTime + offset).coerceAtMost(end.coerceAtLeast(state.startTime))
    }

    /**
     * A round is over when every superset member has completed the set with the same index.
     * Members with fewer sets do not block it. Outside a superset the round is the single set,
     * so it is always complete.
     */
    private fun isRoundComplete(workoutExerciseId: Long, setIndex: Int): Boolean {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return true
        if (exercise.supersetGroup == null) return true
        return _uiState.value.exercises
            .filter { it.supersetGroup == exercise.supersetGroup }
            .all { companion ->
                val set = companion.sets.getOrNull(setIndex) ?: return@all true
                set.completedAt != null
            }
    }

    private fun startRestTimer(totalSeconds: Int) = restTimer.start(totalSeconds)

    fun adjustTimer(deltaSeconds: Int) = restTimer.adjust(deltaSeconds)

    fun skipTimer() = restTimer.skip()

    /**
     * Closes the session with the date and duration confirmed in the finish sheet; the end time is
     * derived from the chosen start, so history and statistics see the workout on the right day.
     *
     * `onFinished` receives `false` when the session had no completed sets: it was deleted and
     * there is no summary to open.
     */
    fun finishWorkout(
        startTime: Long,
        durationSeconds: Int,
        onNeedsRoutineAnswer: (List<RoutineChange>) -> Unit,
        onFinished: (saved: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            skipTimer()
            // Compared against the routine before closing: a session without completed sets never
            // reaches the history, and closing it first would leave nothing to copy over when the
            // answer arrives.
            val routineId = _uiState.value.routineId
            val changes = if (routineId == null) emptyList() else {
                repository.routineChangesFor(sessionId, routineId)
            }
            if (changes.isNotEmpty()) {
                pendingFinish = PendingFinish(startTime, durationSeconds)
                onNeedsRoutineAnswer(changes)
                return@launch
            }
            closeSession(startTime, durationSeconds, onFinished)
        }
    }

    /**
     * Answer to the routine-sync question: [applyToRoutine] rewrites the routine from the session
     * (see [WorkoutRepository.applySessionToRoutine]); either way the session is then closed.
     */
    fun answerRoutineSync(applyToRoutine: Boolean, onFinished: (saved: Boolean) -> Unit) {
        val pending = pendingFinish ?: return
        pendingFinish = null
        viewModelScope.launch {
            val routineId = _uiState.value.routineId
            if (applyToRoutine && routineId != null) {
                repository.applySessionToRoutine(sessionId, routineId)
            }
            closeSession(pending.startTime, pending.durationSeconds, onFinished)
        }
    }

    private suspend fun closeSession(
        startTime: Long,
        durationSeconds: Int,
        onFinished: (saved: Boolean) -> Unit
    ) {
        val saved = repository.finishSession(
            sessionId = sessionId,
            startTime = startTime,
            endTime = startTime + durationSeconds.coerceAtLeast(0) * 1000L
        )
        // Watch heart rate and calories are attached to the session just closed. If the watch has
        // not synced yet nothing happens: the summary screen retries.
        if (saved) runCatching { healthSync.sync(sessionId) }
        _uiState.update { it.copy(isFinished = true) }
        onFinished(saved)
    }

    /**
     * Saves the correction of a past workout: date and duration are written to the session and PR
     * flags are recomputed across the whole history, since a set corrected today can move the
     * maximum of an exercise recorded months ago
     * (see [com.eina.app.domain.recomputePrFlags]).
     */
    fun saveEdits(startTime: Long, durationSeconds: Int, onDone: (kept: Boolean) -> Unit) {
        viewModelScope.launch {
            repository.updateSessionTimes(sessionId, startTime, durationSeconds)
            repository.recomputePrs(touchedExerciseIds + repository.exerciseIdsOfSession(sessionId))
            touchedExerciseIds.clear()
            // A workout stripped of every completed set would stay as a ghost row the history
            // never draws (see [WorkoutRepository.purgeEmptySessions]), so it is deleted.
            val kept = repository.hasCompletedSets(sessionId)
            if (!kept) repository.purgeEmptySessions()
            _uiState.update {
                it.copy(startTime = startTime, elapsedSeconds = durationSeconds.coerceAtLeast(0))
            }
            onDone(kept)
        }
    }

    /** Cancels the workout: the session and its recorded sets are deleted, unlike finishing. */
    fun cancelWorkout(onCancelled: () -> Unit) {
        viewModelScope.launch {
            skipTimer()
            repository.cancelSession(sessionId)
            onCancelled()
        }
    }

    // No onCleared stopping the rest timer: leaving the screen keeps the workout running, and the
    // countdown must survive until the session is finished or cancelled.

    private companion object {
        const val DEFAULT_REST_SECONDS = 90

        /**
         * Assumed sets per exercise when placing a set in time while editing the past: it is only
         * a stride, used to keep exercises ordered relative to each other.
         */
        const val MAX_SETS_PER_EXERCISE = 10

        /** Quiet time after the last keystroke before PR flags are recomputed. */
        const val PR_RECOMPUTE_DEBOUNCE_MS = 400L
    }
}

/** Date and duration chosen in the finish sheet, held until the routine-sync answer arrives. */
private data class PendingFinish(val startTime: Long, val durationSeconds: Int)

/** Exercises sharing rest: the superset members, or the exercise alone when outside a round. */
private fun SessionExerciseUi.sharesRound(other: SessionExerciseUi): Boolean =
    if (other.supersetGroup == null) workoutExerciseId == other.workoutExerciseId
    else supersetGroup == other.supersetGroup

private fun SessionSetUi.toEntity(workoutExerciseId: Long) = SetEntryEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setIndex = setIndex,
    targetReps = targetReps,
    actualReps = actualReps,
    weight = weight,
    restSecondsPlanned = restSecondsPlanned,
    setType = setType,
    completedAt = completedAt,
    isPR = isPR,
    bodyweightSnapshotKg = bodyweightSnapshotKg
)
