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
import com.eina.app.data.transfer.ExerciseMediaStore
import com.eina.app.data.transfer.RoutineTransfer
import kotlinx.coroutines.flow.Flow

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineSetDao: RoutineSetDao,
    private val exerciseDao: ExerciseDao,
    private val mediaStore: ExerciseMediaStore
) {
    fun observeRoutines(): Flow<List<RoutineEntity>> = routineDao.getAll()

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getById(exerciseId)

    /** Whole library: the editor picker filters in memory, as the workout one does. */
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    suspend fun getRoutine(id: Long): RoutineEntity? = routineDao.getById(id)

    fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseEntity>> =
        routineExerciseDao.getForRoutine(routineId)

    /** Planned sets of the routine, ordered by exercise and set index. */
    fun observeRoutineSets(routineId: Long): Flow<List<RoutineSetEntity>> =
        routineSetDao.observeForRoutine(routineId)

    /** Planned set count per routine, shown on the list cards. */
    fun observeRoutineSetCounts(): Flow<List<RoutineSetCountRow>> = routineSetDao.observeSetCounts()

    /** Previews of every routine in one query, feeding the list cards. */
    fun observeRoutinePreviews(): Flow<List<RoutineExercisePreviewRow>> =
        routineExerciseDao.observeAllPreviews()

    /**
     * Insert when id == 0, update otherwise: OnConflictStrategy.REPLACE would delete and recreate
     * the row, triggering the cascade delete on routine_exercises.
     */
    suspend fun saveRoutine(routine: RoutineEntity): Long {
        return if (routine.id == 0L) {
            routineDao.insert(routine)
        } else {
            routineDao.update(routine)
            routine.id
        }
    }

    suspend fun deleteRoutine(routine: RoutineEntity) = routineDao.delete(routine)

    /**
     * Adds an exercise with a single empty set, as the workout screen does: removing extra sets
     * costs more gestures than adding the ones actually needed.
     */
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

    /** Appends a set, inheriting the values of the last working one. */
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

    /** Removes the set and compacts the indices: `setIndex` is the position in the table. */
    suspend fun removeRoutineSet(set: RoutineSetEntity) {
        routineSetDao.delete(set)
        routineSetDao.getForRoutineExercise(set.routineExerciseId)
            .forEachIndexed { index, item ->
                if (item.setIndex != index) routineSetDao.update(item.copy(setIndex = index))
            }
    }

    /**
     * Swaps the movement of a routine row keeping everything else: planned sets, rest, note,
     * position and superset. Nothing is recorded here, so the targets stay as a starting point.
     */
    suspend fun replaceRoutineExercise(routineExercise: RoutineExerciseEntity, newExerciseId: Long) {
        if (routineExercise.exerciseId == newExerciseId) return
        routineExerciseDao.update(routineExercise.copy(exerciseId = newExerciseId))
    }

    suspend fun updateRoutineExercise(routineExercise: RoutineExerciseEntity) =
        routineExerciseDao.update(routineExercise)

    suspend fun removeRoutineExercise(routineExercise: RoutineExerciseEntity) =
        routineExerciseDao.delete(routineExercise)

    /**
     * Routine in exchange format, or null if it no longer exists. See
     * [com.eina.app.data.transfer.RoutineTransfer] for the file format.
     */
    suspend fun exportRoutine(routineId: Long): String? {
        val routine = routineDao.getById(routineId) ?: return null
        val routineExercises = routineExerciseDao.getForRoutineOnce(routineId)
        val sets = routineExercises.associate { it.id to routineSetDao.getForRoutineExercise(it.id) }
        val exercises = routineExercises
            .map { it.exerciseId }
            .distinct()
            .mapNotNull { id -> exerciseDao.getById(id)?.let { id to it } }
            .toMap()
        // Images travel for custom exercises only: library ones are already bundled in the assets
        // of the importing device.
        val media = exercises.values
            .filter { it.isCustom }
            .mapNotNull { exercise ->
                val bytes = mediaStore.read(exercise.mediaUri) ?: return@mapNotNull null
                exercise.id to RoutineTransfer.encodeMedia(bytes, mediaStore.extensionOf(exercise.mediaUri))
            }
            .toMap()
        return RoutineTransfer.encode(routine, routineExercises, sets, exercises, media)
    }

    /**
     * Creates a routine from an exchange file. Exercises are matched to the library by English
     * name; unknown ones become custom exercises, so a routine written elsewhere arrives whole
     * even when it contains movements of its own.
     *
     * Returns the id of the created routine, or null if the file is not an Eina routine.
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
                    // Translated names and descriptions only come from v3 files and only for
                    // custom exercises; the catalog already has the others.
                    nameIt = item.nameIt,
                    nameFr = item.nameFr,
                    description = item.description,
                    descriptionIt = item.descriptionIt,
                    descriptionFr = item.descriptionFr,
                    loggingInstructions = item.loggingInstructions,
                    weightType = item.weightType,
                    bodyweightFactor = item.bodyweightFactor,
                    muscleGroupsPrimary = item.muscleGroupsPrimary,
                    muscleGroupsSecondary = item.muscleGroupsSecondary,
                    equipment = item.equipment,
                    // The image is rewritten into local storage: the exporting device path means
                    // nothing here.
                    mediaUri = item.media
                        ?.let { RoutineTransfer.decodeMedia(it) }
                        ?.let { bytes -> mediaStore.write(bytes, item.media.extension) },
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
        const val DEFAULT_SET_COUNT = 1
        const val DEFAULT_REST_SECONDS = 90
    }
}
