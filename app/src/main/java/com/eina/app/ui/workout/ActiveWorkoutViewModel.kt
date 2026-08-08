package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.usesWeight
import com.eina.app.data.repository.WorkoutRepository
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
                session.routineId?.let { routineId ->
                    routineTargets = repository.getRoutineTargets(routineId)
                    val routine = repository.getRoutine(routineId)
                    _uiState.update {
                        it.copy(
                            playlistUri = routine?.linkedPlaylistUri,
                            playlistType = routine?.linkedPlaylistType
                        )
                    }
                }
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
        val (lastWeight, lastReps) = repository.getLastRecordedValues(exercise.id)
        val sets = withSuggestions(
            repository.getSetsForWorkoutExercise(workoutExercise.id).map { it.toUi(exercise.id, lastTimeSets) },
            lastWeight,
            lastReps,
            exercise.weightType
        )
        val exerciseUi = SessionExerciseUi(
            workoutExerciseId = workoutExercise.id,
            exerciseId = exercise.id,
            name = exercise.exerciseName(),
            weightType = exercise.weightType,
            bodyweightFactor = exercise.bodyweightFactor,
            order = workoutExercise.order,
            notes = workoutExercise.notes,
            supersetGroup = workoutExercise.supersetGroup,
            restSeconds = sets.firstOrNull()?.restSecondsPlanned
                ?: routineTargets[exercise.id]?.restSeconds
                ?: DEFAULT_REST_SECONDS,
            sets = sets,
            lastTimeSets = lastTimeSets,
            lastRecordedWeight = lastWeight,
            lastRecordedReps = lastReps
        )
        _uiState.update { state ->
            val others = state.exercises.filterNot { it.workoutExerciseId == workoutExercise.id }
            state.copy(exercises = (others + exerciseUi).sortedBy { it.order })
        }
        recomputeVolume()
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
     * Riempie i valori proposti serie per serie. Ordine di ripiego: la stessa serie dell'ultima
     * volta, poi il target della routine, poi l'ultimo valore visto (nella sessione in corso o,
     * come ultima spiaggia, nello storico dell'esercizio). Cosi' anche il peso compare come
     * segnaposto quando l'allenamento precedente non lo aveva registrato, e non solo le ripetizioni.
     */
    private fun withSuggestions(
        sets: List<SessionSetUi>,
        fallbackWeight: Double?,
        fallbackReps: Int?,
        weightType: WeightType
    ): List<SessionSetUi> {
        var lastWeight: Double? = if (weightType.usesWeight) fallbackWeight else null
        var lastReps: Int? = fallbackReps
        return sets.map { set ->
            // Senza campo kg in tabella non si propone nemmeno un carico: completare la serie
            // scriverebbe un peso che l'utente non ha mai visto ne' potuto correggere.
            val suggestedWeight = if (!weightType.usesWeight) null
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
            val workoutExercise = WorkoutExerciseEntity(id = workoutExerciseId, sessionId = sessionId, exerciseId = exercise.id, order = order)
            upsertExerciseUi(workoutExercise, exercise)
        }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            repository.removeExercise(workoutExerciseId)
            // Tolto un compagno, un superset rimasto da solo non e' piu' un superset.
            val remaining = dissolveOrphanSupersets(
                _uiState.value.exercises.filterNot { it.workoutExerciseId == workoutExerciseId }
            )
            _uiState.update { it.copy(exercises = remaining) }
            persistExerciseOrder(remaining)
            recomputeVolume()
        }
    }

    fun moveExercise(workoutExerciseId: Long, delta: Int) {
        viewModelScope.launch {
            val current = _uiState.value.exercises
            // Un superset si sposta tutto insieme: vedi Superset.moveBlock.
            val moved = Superset.moveBlock(
                current.map { Superset.Member(it.workoutExerciseId, it.supersetGroup) },
                workoutExerciseId,
                delta
            )
            val byId = current.associateBy { it.workoutExerciseId }
            val reordered = moved.mapNotNull { byId[it.id] }
            if (reordered.map { it.workoutExerciseId } == current.map { it.workoutExerciseId }) return@launch
            _uiState.update { it.copy(exercises = reordered) }
            persistExerciseOrder(reordered)
        }
    }

    private suspend fun persistExerciseOrder(exercises: List<SessionExerciseUi>) {
        val entities = exercises.mapIndexed { index, ex ->
            // La nota va riportata: qui si riscrive la riga intera, ometterla la cancellerebbe.
            WorkoutExerciseEntity(
                id = ex.workoutExerciseId,
                sessionId = sessionId,
                exerciseId = ex.exerciseId,
                order = index,
                notes = ex.notes,
                supersetGroup = ex.supersetGroup
            )
        }
        repository.reorderExercises(entities)
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { index, ex -> ex.copy(order = index) })
        }
    }

    /** Nota dell'esercizio in sessione: tocca solo questo allenamento, non la routine. */
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
            repository.removeSet(setId)
            refreshSets(workoutExerciseId)
        }
    }

    /**
     * Il recupero si imposta per esercizio e si propaga a tutte le sue serie non ancora svolte.
     * Dentro un superset il recupero e' del giro, non del singolo esercizio: si scrive su tutti i
     * compagni, altrimenti la durata dipenderebbe da chi chiude il giro.
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
                target.sets.filter { it.completedAt == null }.forEach { set ->
                    repository.updateSet(
                        set.copy(restSecondsPlanned = safeSeconds).toEntity(target.workoutExerciseId)
                    )
                }
            }
        }
    }

    /**
     * Superset dell'esercizio: `group` null lo tira fuori dal giro. L'esercizio che entra in un
     * giro si sposta accanto ai compagni e ne eredita il recupero — vedi [Superset.regroup].
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
            // Il giro ha un recupero solo: chi entra prende quello dei compagni.
            if (group != null) {
                reordered.firstOrNull { it.supersetGroup == group && it.workoutExerciseId != workoutExerciseId }
                    ?.let { companion -> setRestSeconds(workoutExerciseId, companion.restSeconds) }
            }
        }
    }

    /** Numero di gruppo libero per un superset nuovo. */
    fun nextSupersetGroup(): Int = Superset.nextGroup(_uiState.value.exercises.map { it.supersetGroup })

    private fun dissolveOrphanSupersets(exercises: List<SessionExerciseUi>): List<SessionExerciseUi> {
        val cleaned = Superset.dissolveOrphans(
            exercises.map { Superset.Member(it.workoutExerciseId, it.supersetGroup) }
        ).associateBy({ it.id }, { it.group })
        return exercises.map { ex -> ex.copy(supersetGroup = cleaned[ex.workoutExerciseId]) }
    }

    /**
     * Tipo della serie (riscaldamento, normale, cedimento, drop set). Cambiarlo su una serie gia'
     * segnata la fa entrare o uscire dal volume, quindi il totale si ricalcola subito; il record
     * gia' assegnato resta com'e' — passando a riscaldamento pero' decade, perche' un
     * riscaldamento non fa PR.
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
        viewModelScope.launch { repository.updateSet(updated.toEntity(workoutExerciseId)) }
    }

    fun updateSetValues(workoutExerciseId: Long, setId: Long, actualReps: Int?, weight: Double?) {
        val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val set = exercise.sets.find { it.id == setId } ?: return
        val updated = set.copy(actualReps = actualReps, weight = weight?.coerceIn(0.0, MAX_WEIGHT_KG))
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { ex ->
                    if (ex.workoutExerciseId != workoutExerciseId) ex
                    // Le serie sotto ereditano quello che si sta scrivendo qui: i segnaposto
                    // vanno ricalcolati a ogni tasto, non solo al refresh dal database.
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
    }

    fun completeSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val set = exercise.sets.find { it.id == setId } ?: return@launch
            // Chiudere una serie lasciata vuota registrerebbe 0 kg e 0 ripetizioni: si registrano
            // gli stessi valori mostrati in grigio nei campi.
            val filled = set.copy(
                actualReps = set.actualReps ?: set.suggestedReps,
                weight = set.weight ?: set.suggestedWeight
            )
            val completed = repository.completeSet(filled.toEntity(workoutExerciseId), exercise.exerciseId, exercise.weightType)
            refreshSets(workoutExerciseId)
            feedback.haptic()
            // In un superset il recupero non spetta alla singola serie ma al giro: parte solo
            // quando ogni compagno ha chiuso la sua serie di pari indice.
            if (completed.setType.countsAsWorking && isRoundComplete(workoutExerciseId, set.setIndex)) {
                startRestTimer(completed.restSecondsPlanned)
            }
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

    /**
     * Il giro e' finito quando ogni esercizio del superset ha chiuso la serie di pari indice.
     * I compagni con meno serie non lo bloccano: chi non ha quella serie non deve farla.
     * Fuori da un superset il "giro" e' la singola serie, quindi e' sempre finito.
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

    /**
     * Chiude la sessione con la data e la durata confermate a fine allenamento: la fine si
     * ricalcola dall'inizio scelto, cosi' storico e statistiche vedono l'allenamento nel giorno
     * in cui e' stato fatto davvero.
     *
     * `onFinished` riceve `false` quando la sessione era senza esercizi: in quel caso e' stata
     * eliminata e non c'e' nessun riepilogo da aprire.
     */
    fun finishWorkout(startTime: Long, durationSeconds: Int, onFinished: (saved: Boolean) -> Unit) {
        viewModelScope.launch {
            skipTimer()
            val saved = repository.finishSession(
                sessionId = sessionId,
                startTime = startTime,
                endTime = startTime + durationSeconds.coerceAtLeast(0) * 1000L
            )
            _uiState.update { it.copy(isFinished = true) }
            onFinished(saved)
        }
    }

    /**
     * Annulla l'allenamento: la sessione viene eliminata, con le serie gia' registrate.
     * E' l'uscita per l'allenamento aperto per sbaglio, distinta da "Termina" che invece salva.
     */
    fun cancelWorkout(onCancelled: () -> Unit) {
        viewModelScope.launch {
            skipTimer()
            repository.cancelSession(sessionId)
            onCancelled()
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

/**
 * Esercizi che condividono il recupero: i compagni di superset, o il solo esercizio stesso se
 * non e' in un giro.
 */
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
