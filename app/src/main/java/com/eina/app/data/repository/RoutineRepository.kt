package com.eina.app.data.repository

import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineExercisePreviewRow
import com.eina.app.data.transfer.RoutineTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val exerciseDao: ExerciseDao
) {
    fun observeRoutines(): Flow<List<RoutineEntity>> = routineDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    suspend fun getRoutine(id: Long): RoutineEntity? = routineDao.getById(id)

    fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseEntity>> =
        routineExerciseDao.getForRoutine(routineId)

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

    suspend fun addExerciseToRoutine(routineId: Long, exerciseId: Long, order: Int): Long =
        routineExerciseDao.insert(
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = exerciseId,
                order = order,
                targetSets = 3,
                targetReps = 10,
                targetWeight = null,
                restSeconds = 90
            )
        )

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
        val exercises = routineExercises
            .map { it.exerciseId }
            .distinct()
            .mapNotNull { id -> exerciseDao.getById(id)?.let { id to it } }
            .toMap()
        return RoutineTransfer.encode(routine, routineExercises, exercises)
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
            routineExerciseDao.insert(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    order = index,
                    targetSets = item.targetSets,
                    targetReps = item.targetReps,
                    targetWeight = item.targetWeight,
                    restSeconds = item.restSeconds,
                    notes = item.notes
                )
            )
        }
        return routineId
    }

    suspend fun reorderRoutineExercises(ordered: List<RoutineExerciseEntity>) {
        ordered.forEachIndexed { index, routineExercise ->
            if (routineExercise.order != index) {
                routineExerciseDao.update(routineExercise.copy(order = index))
            }
        }
    }
}
