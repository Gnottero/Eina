package com.eina.app.data.repository

import com.eina.app.data.db.BodyMetricDao
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineSetDao
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.SetEntryDao
import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.WorkoutExerciseDao
import com.eina.app.data.db.WorkoutExerciseEntity
import com.eina.app.data.db.WorkoutSessionDao
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.domain.isNewPR
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
     * Una sessione senza nemmeno un esercizio non ha niente da raccontare: invece di salvarla
     * viene eliminata, come se fosse stata annullata. Ritorna `false` in quel caso.
     */
    suspend fun finishSession(
        sessionId: Long,
        startTime: Long? = null,
        endTime: Long? = null
    ): Boolean {
        val session = workoutSessionDao.getById(sessionId) ?: return false
        if (workoutExerciseDao.getForSessionOnce(sessionId).isEmpty()) {
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
     */
    suspend fun deleteSession(sessionId: Long) {
        workoutSessionDao.deleteById(sessionId)
    }

    /** Cancella tutto lo storico, sessione in corso compresa. Irreversibile: si conferma prima. */
    suspend fun deleteAllSessions() {
        workoutSessionDao.deleteAll()
    }

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = workoutSessionDao.getById(sessionId)

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
            WorkoutExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, order = order)
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
        setEntryDao.getForWorkoutExercise(workoutExerciseId).first().forEach { set ->
            if (set.actualReps == null && set.weight == null && set.completedAt == null) return@forEach
            setEntryDao.update(
                set.copy(
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

    suspend fun removeExercise(workoutExerciseId: Long) {
        workoutExerciseDao.deleteById(workoutExerciseId)
    }

    suspend fun reorderExercises(orderedWorkoutExercises: List<WorkoutExerciseEntity>) {
        orderedWorkoutExercises.forEachIndexed { index, workoutExercise ->
            if (workoutExercise.order != index) {
                workoutExerciseDao.update(workoutExercise.copy(order = index))
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
     */
    suspend fun completeSet(set: SetEntryEntity, exerciseId: Long, weightType: WeightType): SetEntryEntity {
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
            completedAt = System.currentTimeMillis(),
            bodyweightSnapshotKg = bodyweightSnapshotKg
        )
        val isPR = isNewPR(weightType, candidate, historicalSets)
        val finalSet = candidate.copy(isPR = isPR)
        setEntryDao.update(finalSet)
        return finalSet
    }
}

/** Valori della routine proposti in sessione: recupero dell'esercizio e target della prima serie. */
data class RoutineTarget(
    val restSeconds: Int,
    val targetReps: Int?,
    val targetWeight: Double?
)
