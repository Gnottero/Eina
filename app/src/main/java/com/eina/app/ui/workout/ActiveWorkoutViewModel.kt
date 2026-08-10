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
     * Allenamento gia' registrato, aperto dallo storico per correggerlo. Cambiano tre cose: il
     * cronometro non scorre (la durata e' quella salvata), chiudere una serie non fa partire il
     * recupero — non si e' in palestra — e l'istante di completamento resta dentro la giornata
     * dell'allenamento invece di essere "adesso".
     */
    private val isPast: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(sessionId = sessionId, isPast = isPast))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    /** Target della routine di partenza, per exerciseId: alimentano i segnaposto dei campi. */
    private var routineTargets: Map<Long, RoutineTarget> = emptyMap()

    /** Fine allenamento in attesa della risposta sulla scheda: vedi [finishWorkout]. */
    private var pendingFinish: PendingFinish? = null

    /**
     * Esercizi usciti dalla sessione mentre la si correggeva. Vanno ricordati: quando si salva
     * non sono piu' fra i suoi esercizi, ma il loro record puo' essere proprio la serie appena
     * tolta e va rifatto anche per loro.
     */
    private val touchedExerciseIds = mutableSetOf<Long>()

    init {
        repository.observeExercises()
            .onEach { list -> _uiState.update { it.copy(availableExercises = list) } }
            .launchIn(viewModelScope)
        // Il recupero vive nel controller condiviso (vedi RestTimerController): qui si rispecchia
        // soltanto, cosi' rientrando nella schermata si ritrova il conto alla rovescia in corso.
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
                        // Su un allenamento passato la durata e' quella registrata, non il tempo
                        // trascorso da allora: il ticker non gira e va scritta qui.
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
            // Il recupero e' una colonna della voce di sessione: leggerlo dalla prima serie lo
            // faceva tornare al valore della scheda appena quella serie era gia' segnata.
            restSeconds = workoutExercise.restSeconds,
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
        var lastWeight: Double? = if (weightType.usesDecimalField) fallbackWeight else null
        var lastReps: Int? = fallbackReps
        return sets.map { set ->
            // Senza campo decimale in tabella non si propone nemmeno un carico: completare la
            // serie scriverebbe un valore che l'utente non ha mai visto ne' potuto correggere.
            // Sulla distanza il campo c'e', e quel che si propone sono i chilometri.
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
     * Cambia il movimento di una voce senza toccarne il posto: resta nel suo superset, con lo
     * stesso recupero e la stessa nota. I valori gia' registrati si azzerano, erano di un altro
     * esercizio (vedi [WorkoutRepository.replaceExercise]).
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
            // Tolto un compagno, un superset rimasto da solo non e' piu' un superset.
            val remaining = dissolveOrphanSupersets(
                _uiState.value.exercises.filterNot { it.workoutExerciseId == workoutExerciseId }
            )
            _uiState.update { it.copy(exercises = remaining) }
            persistExerciseOrder(remaining)
            recomputeVolume()
        }
    }

    /**
     * Ordine scelto trascinando le voci nel foglio di riordino: arriva gia' completo, quindi si
     * riscrive in blocco invece di scambiare vicini una posizione per volta. Gli id sono quelli
     * dei blocchi appiattiti, cosi' i compagni di superset restano attaccati.
     */
    fun applyOrder(orderedWorkoutExerciseIds: List<Long>) {
        viewModelScope.launch {
            val byId = _uiState.value.exercises.associateBy { it.workoutExerciseId }
            val reordered = orderedWorkoutExerciseIds.mapNotNull { byId[it] }
            // Un ordine parziale riscriverebbe la lista perdendo pezzi: meglio non fare nulla.
            if (reordered.size != byId.size) return@launch
            if (reordered.map { it.workoutExerciseId } == _uiState.value.exercises.map { it.workoutExerciseId }) return@launch
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
                repository.setExerciseRestSeconds(target.workoutExerciseId, safeSeconds)
                // Le serie gia' svolte non si toccano: il loro recupero e' stato consumato. La
                // durata dell'esercizio pero' vive sulla sua riga, quindi non si perde piu'.
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
            val completed = repository.completeSet(
                set = filled.toEntity(workoutExerciseId),
                exerciseId = exercise.exerciseId,
                weightType = exercise.weightType,
                // Correggendo un allenamento passato, "adesso" metterebbe la serie in cima allo
                // storico: la si colloca dentro la giornata in cui e' stata fatta.
                completedAt = if (!isPast) System.currentTimeMillis() else pastCompletionTime(exercise, set)
            )
            refreshSets(workoutExerciseId)
            feedback.haptic()
            // In un superset il recupero non spetta alla singola serie ma al giro: parte solo
            // quando ogni compagno ha chiuso la sua serie di pari indice. Su un allenamento gia'
            // registrato non parte affatto: si sta scrivendo, non ci si sta allenando.
            if (!isPast && completed.setType.countsAsWorking && isRoundComplete(workoutExerciseId, set.setIndex)) {
                startRestTimer(completed.restSecondsPlanned)
            }
        }
    }

    /** Annulla il completamento: la serie torna modificabile e esce dal volume. */
    fun uncompleteSet(workoutExerciseId: Long, setId: Long) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises.find { it.workoutExerciseId == workoutExerciseId } ?: return@launch
            val set = exercise.sets.find { it.id == setId } ?: return@launch
            // Il recupero pianificato torna a quello dell'esercizio: setRestSeconds tocca solo le
            // serie ancora da fare, quindi una serie riaperta si teneva il recupero di prima e
            // rimarcandola faceva partire il timer con la durata vecchia.
            repository.updateSet(
                set.copy(
                    completedAt = null,
                    isPR = false,
                    restSecondsPlanned = exercise.restSeconds
                ).toEntity(workoutExerciseId)
            )
            refreshSets(workoutExerciseId)
        }
    }

    /**
     * Istante in cui collocare una serie chiusa mentre si corregge un allenamento passato:
     * dentro la durata registrata, nell'ordine in cui gli esercizi e le serie compaiono. Serve
     * ai record, che si assegnano seguendo l'ordine di completamento, e all'"ultima volta".
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

    private fun startRestTimer(totalSeconds: Int) = restTimer.start(totalSeconds)

    fun adjustTimer(deltaSeconds: Int) = restTimer.adjust(deltaSeconds)

    fun skipTimer() = restTimer.skip()

    /**
     * Chiude la sessione con la data e la durata confermate a fine allenamento: la fine si
     * ricalcola dall'inizio scelto, cosi' storico e statistiche vedono l'allenamento nel giorno
     * in cui e' stato fatto davvero.
     *
     * `onFinished` riceve `false` quando la sessione era senza esercizi: in quel caso e' stata
     * eliminata e non c'e' nessun riepilogo da aprire.
     */
    fun finishWorkout(
        startTime: Long,
        durationSeconds: Int,
        onNeedsRoutineAnswer: (List<RoutineChange>) -> Unit,
        onFinished: (saved: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            skipTimer()
            // Il confronto con la scheda si fa prima di chiudere, e la domanda si fa prima di
            // scrivere: una sessione senza serie svolte non finisce nello storico, ma le
            // modifiche fatte in palestra valgono lo stesso — se la si chiudesse subito, quando
            // arriva la risposta non ci sarebbe piu' niente da copiare sulla scheda.
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
     * Risposta alla domanda di fine allenamento: `applyToRoutine` riscrive la scheda com'e'
     * andata la sessione (vedi [WorkoutRepository.applySessionToRoutine]), poi in entrambi i casi
     * l'allenamento si chiude davvero.
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
        // Battiti e calorie dell'orologio si attaccano alla sessione appena chiusa. Se
        // l'orologio non ha ancora sincronizzato non succede niente: ci riprova il riepilogo.
        if (saved) runCatching { healthSync.sync(sessionId) }
        _uiState.update { it.copy(isFinished = true) }
        onFinished(saved)
    }

    /**
     * Chiude la correzione di un allenamento passato: data e durata scelte si scrivono sulla
     * sessione e i record si rifanno su tutto lo storico, perche' una serie corretta oggi puo'
     * spostare il massimo di un esercizio in un allenamento di mesi fa
     * (vedi [com.eina.app.domain.recomputePrFlags]).
     */
    fun saveEdits(startTime: Long, durationSeconds: Int, onDone: (kept: Boolean) -> Unit) {
        viewModelScope.launch {
            repository.updateSessionTimes(sessionId, startTime, durationSeconds)
            repository.recomputePrs(touchedExerciseIds + repository.exerciseIdsOfSession(sessionId))
            touchedExerciseIds.clear()
            // Un allenamento a cui si sono tolte tutte le serie svolte non e' piu' un allenamento:
            // lo storico non lo disegnerebbe e resterebbe una riga fantasma
            // (vedi [WorkoutRepository.purgeEmptySessions]). Sparisce come chi lo annulla.
            val kept = repository.hasCompletedSets(sessionId)
            if (!kept) repository.purgeEmptySessions()
            _uiState.update {
                it.copy(startTime = startTime, elapsedSeconds = durationSeconds.coerceAtLeast(0))
            }
            onDone(kept)
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

    // Niente onCleared che fermi il recupero: uscire dalla schermata lascia l'allenamento in
    // corso, e il conto alla rovescia deve sopravvivere fino a "Termina" o "Annulla".

    private companion object {
        const val DEFAULT_REST_SECONDS = 90

        /**
         * Serie per esercizio ipotizzate quando si colloca nel tempo una serie chiusa correggendo
         * il passato: e' solo un passo, serve a tenere gli esercizi in ordine fra loro.
         */
        const val MAX_SETS_PER_EXERCISE = 10
    }
}

/** Data e durata scelte a fine allenamento, in attesa della risposta sulla scheda. */
private data class PendingFinish(val startTime: Long, val durationSeconds: Int)

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
