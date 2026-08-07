package com.eina.app.data.repository

import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.RoutineDao
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseDao
import com.eina.app.data.db.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

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

    suspend fun reorderRoutineExercises(ordered: List<RoutineExerciseEntity>) {
        ordered.forEachIndexed { index, routineExercise ->
            if (routineExercise.order != index) {
                routineExerciseDao.update(routineExercise.copy(order = index))
            }
        }
    }
}
