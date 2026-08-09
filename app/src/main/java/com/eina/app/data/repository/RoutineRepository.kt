package com.eina.app.data.repository

import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineExercisePreviewRow
import com.eina.app.data.db.RoutineSetDao
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.RoutineSetCountRow
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.transfer.RoutineTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineSetDao: RoutineSetDao,
    private val exerciseDao: ExerciseDao
) {
    fun observeRoutines(): Flow<List<RoutineEntity>> = routineDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    /** Libreria intera: il foglio di scelta dell'editor routine filtra in memoria come in sessione. */
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    suspend fun getRoutine(id: Long): RoutineEntity? = routineDao.getById(id)

    fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseEntity>> =
        routineExerciseDao.getForRoutine(routineId)

    /** Serie pianificate della routine, in ordine di esercizio e di serie. */
    fun observeRoutineSets(routineId: Long): Flow<List<RoutineSetEntity>> =
        routineSetDao.observeForRoutine(routineId)

    /** Serie pianificate per routine: il conteggio delle card dell'elenco. */
    fun observeRoutineSetCounts(): Flow<List<RoutineSetCountRow>> = routineSetDao.observeSetCounts()

    /** Anteprime di tutte le routine in un colpo solo: alimenta le card dell'elenco. */
    fun observeRoutinePreviews(): Flow<List<RoutineExercisePreviewRow>> =
        routineExerciseDao.observeAllPreviews()

    /** Insert se id == 0 (nuova routine), update altrimenti: evita OnConflictStrategy.REPLACE che
     * cancellerebbe e ricreerebbe la riga, triggerando la cascade delete su routine_exercises. */
    suspend fun saveRoutine(routine: RoutineEntity): Long {
        return if (routine.id == 0L) {
            routineDao.insert(routine)
        } else {
            routineDao.update(routine)
            routine.id
        }
    }

    suspend fun deleteRoutine(routine: RoutineEntity) = routineDao.delete(routine)

    /** Esercizio aggiunto alla scheda con tre serie normali vuote, come in allenamento. */
    suspend fun addExerciseToRoutine(routineId: Long, exerciseId: Long, order: Int): Long {
        val routineExerciseId = routineExerciseDao.insert(
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = exerciseId,
                order = order,
                restSeconds = DEFAULT_REST_SECONDS
            )
        )
        repeat(DEFAULT_SET_COUNT) { index ->
            routineSetDao.insert(RoutineSetEntity(routineExerciseId = routineExerciseId, setIndex = index))
        }
        return routineExerciseId
    }

    /** Serie in coda all'esercizio: eredita i valori dell'ultima, come farebbe in palestra. */
    suspend fun addSetToRoutineExercise(routineExerciseId: Long) {
        val existing = routineSetDao.getForRoutineExercise(routineExerciseId)
        val last = existing.lastOrNull { it.setType.countsAsWorking } ?: existing.lastOrNull()
        routineSetDao.insert(
            RoutineSetEntity(
                routineExerciseId = routineExerciseId,
                setIndex = existing.size,
                targetReps = last?.targetReps,
                targetWeight = last?.targetWeight
            )
        )
    }

    suspend fun updateRoutineSet(set: RoutineSetEntity) = routineSetDao.update(set)

    /** Toglie la serie e ricompatta gli indici: `setIndex` resta la posizione in tabella. */
    suspend fun removeRoutineSet(set: RoutineSetEntity) {
        routineSetDao.delete(set)
        routineSetDao.getForRoutineExercise(set.routineExerciseId)
            .forEachIndexed { index, item ->
                if (item.setIndex != index) routineSetDao.update(item.copy(setIndex = index))
            }
    }

    suspend fun updateRoutineExercise(routineExercise: RoutineExerciseEntity) =
        routineExerciseDao.update(routineExercise)

    suspend fun removeRoutineExercise(routineExercise: RoutineExerciseEntity) =
        routineExerciseDao.delete(routineExercise)

    /**
     * Routine in formato di scambio, o null se non esiste piu'. Vedi
     * [com.eina.app.data.transfer.RoutineTransfer] per il formato del file.
     */
    suspend fun exportRoutine(routineId: Long): String? {
        val routine = routineDao.getById(routineId) ?: return null
        val routineExercises = routineExerciseDao.getForRoutine(routineId).first()
        val sets = routineExercises.associate { it.id to routineSetDao.getForRoutineExercise(it.id) }
        val exercises = routineExercises
            .map { it.exerciseId }
            .distinct()
            .mapNotNull { id -> exerciseDao.getById(id)?.let { id to it } }
            .toMap()
        return RoutineTransfer.encode(routine, routineExercises, sets, exercises)
    }

    /**
     * Crea una routine da un file di scambio. Gli esercizi si riagganciano per nome inglese a
     * quelli gia' in libreria; quelli sconosciuti diventano esercizi custom, cosi' una scheda
     * scritta da un preparatore arriva intera anche se contiene movimenti suoi.
     *
     * Ritorna l'id della routine creata, o null se il file non e' una routine Eina.
     */
    suspend fun importRoutine(json: String): Long? {
        val payload = RoutineTransfer.decode(json) ?: return null
        val routineId = routineDao.insert(
            RoutineEntity(
                name = payload.name,
                notes = payload.notes,
                linkedPlaylistUri = payload.linkedPlaylistUri,
                linkedPlaylistType = payload.linkedPlaylistType
            )
        )
        payload.exercises.forEachIndexed { index, item ->
            val exerciseId = exerciseDao.getByName(item.name)?.id ?: exerciseDao.insert(
                ExerciseEntity(
                    name = item.name,
                    description = item.description,
                    loggingInstructions = "",
                    weightType = item.weightType,
                    muscleGroupsPrimary = item.muscleGroupsPrimary,
                    muscleGroupsSecondary = item.muscleGroupsSecondary,
                    equipment = item.equipment,
                    isCustom = true,
                    source = RoutineTransfer.FORMAT
                )
            )
            val routineExerciseId = routineExerciseDao.insert(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    order = index,
                    restSeconds = item.restSeconds,
                    notes = item.notes,
                    supersetGroup = item.supersetGroup
                )
            )
            item.sets.forEachIndexed { setIndex, set ->
                routineSetDao.insert(
                    RoutineSetEntity(
                        routineExerciseId = routineExerciseId,
                        setIndex = setIndex,
                        targetReps = set.targetReps,
                        targetWeight = set.targetWeight,
                        setType = set.setType
                    )
                )
            }
        }
        return routineId
    }

    private companion object {
        const val DEFAULT_SET_COUNT = 3
        const val DEFAULT_REST_SECONDS = 90
    }
}
