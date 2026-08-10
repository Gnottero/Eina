package com.eina.app.data.repository

import com.eina.app.data.db.BodyMetricDao
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineSetDao
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.encodeHeartRateSamples
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.HeartRateSample
import com.eina.app.data.db.SetEntryDao
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.usesWeight
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.WorkoutExerciseDao
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.db.WorkoutSessionDao
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.domain.PlanItem
import com.eina.app.domain.RoutineChange
import com.eina.app.domain.routineChanges
import com.eina.app.domain.isNewPR
import com.eina.app.domain.recomputePrFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WorkoutRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val bodyMetricDao: BodyMetricDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineSetDao: RoutineSetDao,
    private val routineDao: RoutineDao
) {
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    suspend fun insertExercise(exercise: ExerciseEntity): Long = exerciseDao.insert(exercise)

    /**
     * Elimina un esercizio custom, se non lo usa nessuno. Ritorna false quando compare in una
     * routine o in un allenamento gia' registrato: li' il nome serve ancora.
     * Gli esercizi di libreria non si toccano: li riscriverebbe il seeder al primo avvio utile.
     */
    suspend fun deleteCustomExercise(exercise: ExerciseEntity): Boolean {
        if (!exercise.isCustom) return false
        if (exerciseDao.countUsages(exercise.id) > 0) return false
        exerciseDao.delete(exercise)
        return true
    }

    /** Sessione ancora aperta, se esiste: alimenta il banner "Riprendi" e blocca nuovi avvii. */
    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeActive()

    /**
     * Avvia un allenamento libero. Se ce n'e' gia' uno in corso non ne crea un secondo:
     * ritorna quello aperto, cosi' il chiamante ci rientra invece di duplicarlo.
     */
    suspend fun startSession(): Long {
        workoutSessionDao.getActive()?.let { return it.id }
        return workoutSessionDao.insert(WorkoutSessionEntity(startTime = System.currentTimeMillis()))
    }

    /**
     * Crea sessione legata alla routine e prepara esercizi/set vuoti.
     * I valori della routine restano *target* (targetReps, recupero pianificato): non vengono
     * scritti in `weight`/`actualReps`, cosi' una serie appena creata non risulta gia' svolta.
     */
    suspend fun startSessionFromRoutine(routineId: Long): Long {
        workoutSessionDao.getActive()?.let { return it.id }
        val sessionId = workoutSessionDao.insert(
            WorkoutSessionEntity(routineId = routineId, startTime = System.currentTimeMillis())
        )
        val routineExercises = routineExerciseDao.getForRoutine(routineId).first().sortedBy { it.order }
        routineExercises.forEachIndexed { index, routineExercise ->
            val workoutExerciseId = workoutExerciseDao.insert(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = routineExercise.exerciseId,
                    order = index,
                    restSeconds = routineExercise.restSeconds,
                    // La nota della routine parte come nota della sessione: modificarla durante
                    // l'allenamento non deve riscrivere il template. Stessa storia per il
                    // superset: il giro della scheda si puo' rifare in palestra senza toccarla.
                    notes = routineExercise.notes,
                    supersetGroup = routineExercise.supersetGroup
                )
            )
            // Una serie della sessione per ogni serie della scheda, col suo tipo: il
            // riscaldamento scritto in routine e' gia' segnato come tale in palestra.
            routineSetDao.getForRoutineExercise(routineExercise.id).forEachIndexed { setIndex, routineSet ->
                setEntryDao.insert(
                    SetEntryEntity(
                        workoutExerciseId = workoutExerciseId,
                        setIndex = setIndex,
                        targetReps = routineSet.targetReps,
                        restSecondsPlanned = routineExercise.restSeconds,
                        setType = routineSet.setType
                    )
                )
            }
        }
        return sessionId
    }

    /**
     * Chiude la sessione. `startTime` ed `endTime` arrivano dalla conferma di fine allenamento,
     * dove sono correggibili: un allenamento fatto ieri va nello storico di ieri.
     *
     * Una sessione senza nemmeno una serie svolta non ha niente da raccontare: invece di salvarla
     * viene eliminata, come se fosse stata annullata. Ritorna `false` in quel caso.
     *
     * Il criterio e' la serie svolta e non l'esercizio in lista: lo storico si disegna sulle
     * serie completate, quindi un allenamento aperto, riempito di esercizi e mai fatto restava
     * una riga invisibile — nello storico non compariva, ma continuava a esistere.
     */
    suspend fun finishSession(
        sessionId: Long,
        startTime: Long? = null,
        endTime: Long? = null
    ): Boolean {
        val session = workoutSessionDao.getById(sessionId) ?: return false
        val hasCompletedSets = workoutExerciseDao.getForSessionOnce(sessionId).any { workoutExercise ->
            setEntryDao.getForWorkoutExercise(workoutExercise.id).first().any { it.completedAt != null }
        }
        if (!hasCompletedSets) {
            workoutSessionDao.deleteById(sessionId)
            return false
        }
        val start = startTime ?: session.startTime
        workoutSessionDao.update(
            session.copy(
                startTime = start,
                endTime = endTime ?: System.currentTimeMillis()
            )
        )
        return true
    }

    /**
     * Annulla la sessione: la riga sparisce e con lei, per cascade, esercizi e serie registrate.
     * Serve per l'allenamento aperto per sbaglio, che altrimenti resterebbe nello storico.
     */
    suspend fun cancelSession(sessionId: Long) {
        workoutSessionDao.deleteById(sessionId)
    }

    /**
     * Elimina un allenamento gia' registrato, con le sue serie. Stessa cancellazione di
     * [cancelSession], ma parte dallo storico: si conferma prima, e' irreversibile.
     *
     * I record degli esercizi che c'erano dentro si rifanno: se il massimo di sempre stava in
     * quella sessione, sparita lei il record torna a chi ce l'aveva prima.
     */
    suspend fun deleteSession(sessionId: Long) {
        val exerciseIds = workoutExerciseDao.getForSessionOnce(sessionId).map { it.exerciseId }
        workoutSessionDao.deleteById(sessionId)
        recomputePrs(exerciseIds)
    }

    /**
     * Corregge data e durata di un allenamento gia' registrato: e' la stessa scelta del foglio di
     * fine allenamento, riaperta dalla modifica di una sessione passata.
     */
    suspend fun updateSessionTimes(sessionId: Long, startTime: Long, durationSeconds: Int) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(
            session.copy(
                startTime = startTime,
                endTime = startTime + durationSeconds.coerceAtLeast(0) * 1000L
            )
        )
    }

    /**
     * Rifa' i record degli esercizi indicati su tutto lo storico. Vedi
     * [com.eina.app.domain.recomputePrFlags]: modificare un allenamento passato puo' creare o
     * annullare record anche in sessioni diverse da quella toccata.
     */
    suspend fun recomputePrs(exerciseIds: Collection<Long>) {
        exerciseIds.distinct().forEach { exerciseId ->
            val weightType = exerciseDao.getById(exerciseId)?.weightType ?: return@forEach
            recomputePrFlags(weightType, setEntryDao.getCompletedSetsForExercise(exerciseId))
                .forEach { setEntryDao.update(it) }
        }
    }

    /** Esercizi toccati da una sessione: la lista da passare a [recomputePrs] dopo averla corretta. */
    suspend fun exerciseIdsOfSession(sessionId: Long): List<Long> =
        workoutExerciseDao.getForSessionOnce(sessionId).map { it.exerciseId }

    /** Cancella tutto lo storico, sessione in corso compresa. Irreversibile: si conferma prima. */
    suspend fun deleteAllSessions() {
        workoutSessionDao.deleteAll()
    }

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = workoutSessionDao.getById(sessionId)

    /** La sessione osservata: il riepilogo si aggiorna quando arrivano i dati dell'orologio. */
    fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeById(sessionId)

    /** Routine di partenza della sessione: serve il link playlist durante l'allenamento. */
    suspend fun getRoutine(routineId: Long): RoutineEntity? = routineDao.getById(routineId)

    /**
     * Target della routine per exerciseId: servono alla UI come segnaposto, non come valori
     * registrati. Peso e ripetizioni arrivano dalla prima serie di lavoro della scheda — le
     * serie hanno ognuna i propri valori, ma a una serie aggiunta a mano in palestra serve un
     * numero solo da proporre.
     */
    suspend fun getRoutineTargets(routineId: Long): Map<Long, RoutineTarget> =
        routineExerciseDao.getForRoutine(routineId).first().associate { routineExercise ->
            val sets = routineSetDao.getForRoutineExercise(routineExercise.id)
            val reference = sets.firstOrNull { it.setType.countsAsWorking } ?: sets.firstOrNull()
            routineExercise.exerciseId to RoutineTarget(
                restSeconds = routineExercise.restSeconds,
                targetReps = reference?.targetReps,
                targetWeight = reference?.targetWeight
            )
        }

    /**
     * La scheda ridotta a quel che si confronta con l'allenamento svolto: vedi
     * [com.eina.app.domain.routineChanges].
     */
    suspend fun routinePlan(routineId: Long): List<PlanItem> =
        routineExerciseDao.getForRoutine(routineId).first().sortedBy { it.order }.mapNotNull { routineExercise ->
            val exercise = exerciseDao.getById(routineExercise.exerciseId) ?: return@mapNotNull null
            PlanItem(
                exerciseId = routineExercise.exerciseId,
                name = exercise.exerciseName(),
                setCount = routineSetDao.getForRoutineExercise(routineExercise.id).size,
                restSeconds = routineExercise.restSeconds
            )
        }

    /** Cosa e' cambiato fra la scheda e l'allenamento: lista vuota se l'ha seguita alla lettera. */
    suspend fun routineChangesFor(sessionId: Long, routineId: Long): List<RoutineChange> =
        routineChanges(routinePlan(routineId), sessionPlan(sessionId))

    /** L'allenamento svolto nella stessa forma della scheda, per poterli confrontare. */
    suspend fun sessionPlan(sessionId: Long): List<PlanItem> =
        workoutExerciseDao.getForSessionOnce(sessionId).mapNotNull { workoutExercise ->
            val exercise = exerciseDao.getById(workoutExercise.exerciseId) ?: return@mapNotNull null
            val sets = setEntryDao.getForWorkoutExercise(workoutExercise.id).first()
            PlanItem(
                exerciseId = workoutExercise.exerciseId,
                name = exercise.exerciseName(),
                setCount = sets.size,
                restSeconds = workoutExercise.restSeconds
            )
        }

    /**
     * Riscrive la scheda com'e' andato l'allenamento: esercizi, ordine, superset, note, recupero
     * e serie (numero e tipo). I target diventano i valori registrati, perche' una scheda
     * aggiornata dopo un allenamento serve a ripartire da li' la volta dopo.
     *
     * Le voci si rifanno da capo invece di correggerle una a una: le differenze possono essere
     * di ogni genere (una voce in mezzo tolta, due aggiunte, l'ordine ribaltato) e un merge
     * incrementale avrebbe piu' casi che righe.
     */
    suspend fun applySessionToRoutine(sessionId: Long, routineId: Long) {
        val sessionExercises = workoutExerciseDao.getForSessionOnce(sessionId)
        // La cascade di routine_exercises porta via anche le sue routine_sets.
        routineExerciseDao.getForRoutine(routineId).first().forEach { routineExerciseDao.delete(it) }
        sessionExercises.forEachIndexed { index, workoutExercise ->
            val sets = setEntryDao.getForWorkoutExercise(workoutExercise.id).first()
            val routineExerciseId = routineExerciseDao.insert(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = workoutExercise.exerciseId,
                    order = index,
                    restSeconds = workoutExercise.restSeconds,
                    notes = workoutExercise.notes,
                    supersetGroup = workoutExercise.supersetGroup
                )
            )
            sets.forEachIndexed { setIndex, set ->
                routineSetDao.insert(
                    RoutineSetEntity(
                        routineExerciseId = routineExerciseId,
                        setIndex = setIndex,
                        // Una serie lasciata a meta' non deve cancellare il target che c'era.
                        targetReps = set.actualReps ?: set.targetReps,
                        targetWeight = set.weight,
                        setType = set.setType
                    )
                )
            }
        }
    }

    /**
     * Allenamento di prova nello storico, con battiti e calorie come se li avesse depositati un
     * orologio. Serve a vedere riepilogo, grafici e card condivisibile pieni di dati senza dover
     * andare in palestra, e a provare la modifica di un allenamento passato su qualcosa di
     * cancellabile.
     *
     * DECISIONE: i campioni del cuore si scrivono a mano invece di passare da Health Connect —
     * la sorgente vera vuole un orologio collegato, che e' esattamente quel che qui manca.
     *
     * Ritorna l'id della sessione creata, o null se la libreria e' vuota (niente esercizi da
     * mettere dentro: succede solo se il seed non e' ancora passato).
     */
    suspend fun insertSampleSession(): Long? {
        val library = exerciseDao.getAll().first()
        if (library.isEmpty()) return null
        // Tre esercizi riconoscibili, con ripiego sui primi della libreria: il catalogo e' chiuso
        // ma un esercizio potrebbe non esserci piu' dopo una revisione dei nomi.
        val picks = listOf(
            "Barbell Bench Press - Medium Grip",
            "Wide-Grip Lat Pulldown",
            "Barbell Shoulder Press"
        ).mapIndexedNotNull { index, name ->
            library.firstOrNull { it.name == name } ?: library.getOrNull(index)
        }.distinctBy { it.id }
        if (picks.isEmpty()) return null

        val start = System.currentTimeMillis() - SAMPLE_DAYS_AGO * 86_400_000L
        val durationMillis = SAMPLE_DURATION_MINUTES * 60_000L
        val samples = sampleHeartRate(start, durationMillis)
        val sessionId = workoutSessionDao.insert(
            WorkoutSessionEntity(
                startTime = start,
                endTime = start + durationMillis,
                avgHeartRateBpm = samples.map { it.bpm }.average().toInt(),
                maxHeartRateBpm = samples.maxOf { it.bpm },
                caloriesKcal = 486.0,
                heartRateSamples = samples.encodeHeartRateSamples()
            )
        )

        // Un riscaldamento e tre serie di lavoro in progressione: e' la forma piu' comune, e
        // mostra sia la sigla W sia il badge del record.
        val plan = listOf(
            listOf(40.0 to 10, 60.0 to 10, 65.0 to 8, 70.0 to 6),
            listOf(35.0 to 12, 50.0 to 10, 55.0 to 9, 55.0 to 8),
            listOf(20.0 to 12, 35.0 to 10, 37.5 to 8, 40.0 to 6)
        )
        picks.forEachIndexed { index, exercise ->
            val workoutExerciseId = workoutExerciseDao.insert(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    order = index,
                    restSeconds = 90
                )
            )
            plan[index % plan.size].forEachIndexed { setIndex, (weight, reps) ->
                setEntryDao.insert(
                    SetEntryEntity(
                        workoutExerciseId = workoutExerciseId,
                        setIndex = setIndex,
                        targetReps = reps,
                        actualReps = reps,
                        // Gli esercizi a corpo libero o a tempo non hanno un carico da scrivere:
                        // il campione resta comunque leggibile, con le sole ripetizioni.
                        weight = weight.takeIf { exercise.weightType.usesWeight },
                        restSecondsPlanned = 90,
                        setType = if (setIndex == 0) SetType.WARMUP else SetType.NORMAL,
                        // Sparse lungo la sessione: l'ordine di completamento e' quel che decide
                        // i record, e tutte allo stesso istante non racconterebbe un allenamento.
                        completedAt = start + (index * 4 + setIndex) * 4L * 60_000L
                    )
                )
            }
        }

        // I record si assegnano qui e non a mano: la sessione di prova puo' cadere prima o dopo
        // allenamenti veri, e solo il confronto con tutto lo storico sa dove sta il massimo.
        recomputePrs(picks.map { it.id })
        return sessionId
    }

    /** Curva plausibile: si sale nel riscaldamento, si oscilla fra le serie, si scende alla fine. */
    private fun sampleHeartRate(start: Long, durationMillis: Long): List<HeartRateSample> {
        val stepMillis = 3 * 60_000L
        val steps = (durationMillis / stepMillis).toInt().coerceAtLeast(2)
        return (0..steps).map { step ->
            val progress = step.toDouble() / steps
            val base = 96 + 62 * kotlin.math.sin(progress * Math.PI).coerceAtLeast(0.0)
            // Ondeggia serie per serie: un cuore sotto carico non disegna una collina liscia.
            val wave = 10 * kotlin.math.sin(step * 1.7)
            HeartRateSample(
                timeMillis = start + step * stepMillis,
                bpm = (base + wave).toInt().coerceIn(80, 171)
            )
        }
    }

    suspend fun getSessionExercises(sessionId: Long): List<WorkoutExerciseEntity> =
        workoutExerciseDao.getForSessionOnce(sessionId)

    suspend fun getSetsForWorkoutExercise(workoutExerciseId: Long): List<SetEntryEntity> =
        setEntryDao.getForWorkoutExercise(workoutExerciseId).first()

    suspend fun getLastTimeSets(exerciseId: Long, currentSessionId: Long): List<SetEntryEntity> =
        setEntryDao.getLastTimeSets(exerciseId, currentSessionId)

    /** Ultimi peso/ripetizioni registrati per l'esercizio, in qualunque sessione. */
    suspend fun getLastRecordedValues(exerciseId: Long): Pair<Double?, Int?> =
        setEntryDao.getLastRecordedWeight(exerciseId) to setEntryDao.getLastRecordedReps(exerciseId)

    suspend fun addExercise(sessionId: Long, exerciseId: Long, order: Int, defaultRestSeconds: Int = 90): Long {
        val workoutExerciseId = workoutExerciseDao.insert(
            WorkoutExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                order = order,
                restSeconds = defaultRestSeconds
            )
        )
        setEntryDao.insert(
            SetEntryEntity(
                workoutExerciseId = workoutExerciseId,
                setIndex = 0,
                restSecondsPlanned = defaultRestSeconds
            )
        )
        return workoutExerciseId
    }

    /**
     * Sostituisce il movimento di una voce dell'allenamento: la panca diventa manubri perche' il
     * castello e' occupato, senza rifare la voce da capo. La riga resta la stessa, quindi ordine,
     * superset e note restano dov'erano — un esercizio in un giro ci resta.
     *
     * Le serie sopravvivono come impianto (quante sono, di che tipo, con che recupero) ma i valori
     * registrati vengono azzerati: peso e ripetizioni erano di un altro esercizio, e lasciarli
     * li' li conterebbe nel volume e nella progressione del nuovo. Il record eventualmente
     * assegnato decade per lo stesso motivo.
     *
     * Ritorna false se la voce non esiste o se l'esercizio scelto e' gia' quello.
     */
    suspend fun replaceExercise(workoutExerciseId: Long, newExerciseId: Long): Boolean {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return false
        if (current.exerciseId == newExerciseId) return false
        workoutExerciseDao.update(current.copy(exerciseId = newExerciseId))
        // Le serie si rifanno da capo invece di ripulirle riga per riga: i campi in tabella
        // tengono il testo digitato finche' la serie ha lo stesso id, e ripulire il database
        // lascerebbe a schermo i numeri del vecchio esercizio, pronti da confermare.
        setEntryDao.getForWorkoutExercise(workoutExerciseId).first().forEach { set ->
            setEntryDao.deleteById(set.id)
            setEntryDao.insert(
                set.copy(
                    id = 0,
                    actualReps = null,
                    weight = null,
                    completedAt = null,
                    isPR = false,
                    bodyweightSnapshotKg = null
                )
            )
        }
        return true
    }

    suspend fun setExerciseNotes(workoutExerciseId: Long, notes: String?) {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return
        workoutExerciseDao.update(current.copy(notes = notes))
    }

    /** Recupero dell'esercizio in sessione: vedi [WorkoutExerciseEntity.restSeconds]. */
    suspend fun setExerciseRestSeconds(workoutExerciseId: Long, restSeconds: Int) {
        val current = workoutExerciseDao.getById(workoutExerciseId) ?: return
        if (current.restSeconds == restSeconds) return
        workoutExerciseDao.update(current.copy(restSeconds = restSeconds))
    }

    suspend fun removeExercise(workoutExerciseId: Long) {
        workoutExerciseDao.deleteById(workoutExerciseId)
    }

    /**
     * Riscrive la lista degli esercizi di sessione: la posizione nella lista *e'* il campo
     * `order`, e con essa viaggiano superset e note.
     *
     * Il confronto va fatto con la riga sul database, non con l'`order` di quella in arrivo: il
     * chiamante costruisce le righe gia' numerate, quindi confrontarle con il proprio indice
     * dava sempre "uguale" e non veniva scritto mai niente. Ordine e superset cambiati in
     * palestra sparivano uscendo dall'allenamento, e al rientro tornava fuori la scheda.
     */
    suspend fun reorderExercises(orderedWorkoutExercises: List<WorkoutExerciseEntity>) {
        orderedWorkoutExercises.forEachIndexed { index, workoutExercise ->
            val updated = workoutExercise.copy(order = index)
            if (workoutExerciseDao.getById(workoutExercise.id) != updated) {
                workoutExerciseDao.update(updated)
            }
        }
    }

    suspend fun addSet(workoutExerciseId: Long, setIndex: Int, restSecondsPlanned: Int): Long =
        setEntryDao.insert(
            SetEntryEntity(
                workoutExerciseId = workoutExerciseId,
                setIndex = setIndex,
                restSecondsPlanned = restSecondsPlanned
            )
        )

    suspend fun removeSet(setId: Long) {
        setEntryDao.deleteById(setId)
    }

    suspend fun updateSet(set: SetEntryEntity) {
        setEntryDao.update(set)
    }

    /**
     * Completa una set: applica bodyweight snapshot per i weightType che lo richiedono, calcola isPR
     * confrontando con lo storico non-warmup, persiste. Ritorna la set aggiornata.
     *
     * `completedAt` si passa quando la serie non si sta chiudendo adesso: correggendo un
     * allenamento del mese scorso, "adesso" la collocherebbe in cima allo storico e ne farebbe
     * l'ultimo valore noto di quell'esercizio.
     */
    suspend fun completeSet(
        set: SetEntryEntity,
        exerciseId: Long,
        weightType: WeightType,
        completedAt: Long = System.currentTimeMillis()
    ): SetEntryEntity {
        val needsBodyweightSnapshot = weightType == WeightType.BODYWEIGHT ||
            weightType == WeightType.BODYWEIGHT_PLUS_LOAD ||
            weightType == WeightType.ASSISTED

        val bodyweightSnapshotKg = if (needsBodyweightSnapshot) {
            set.bodyweightSnapshotKg ?: bodyMetricDao.getLatest()?.bodyweightKg
        } else {
            null
        }

        val historicalSets = setEntryDao.getHistoricalSets(exerciseId)
        val candidate = set.copy(
            completedAt = completedAt,
            bodyweightSnapshotKg = bodyweightSnapshotKg
        )
        val isPR = isNewPR(weightType, candidate, historicalSets)
        val finalSet = candidate.copy(isPR = isPR)
        setEntryDao.update(finalSet)
        return finalSet
    }

    private companion object {
        const val SAMPLE_DAYS_AGO = 1
        const val SAMPLE_DURATION_MINUTES = 62
    }
}

/** Valori della routine proposti in sessione: recupero dell'esercizio e target della prima serie. */
data class RoutineTarget(
    val restSeconds: Int,
    val targetReps: Int?,
    val targetWeight: Double?
)
