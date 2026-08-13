package com.eina.app.ui.routine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.repository.RoutineRepository
import com.eina.app.domain.Superset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineEditorUiState(
    val name: String = "",
    val notes: String = "",
    val linkedPlaylistUri: String = "",
    val linkedPlaylistType: PlaylistType? = null,
    val ready: Boolean = false
)

// flatMapLatest is experimental but stable: the routine id changes once, when the draft is
// created, and the observed routine has to follow it.
@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModel(
    private val repository: RoutineRepository,
    private val appContext: Context,
    initialRoutineId: Long
) : ViewModel() {
    private var routineId: Long = initialRoutineId
    private val routineIdFlow = MutableStateFlow(initialRoutineId)

    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState: StateFlow<RoutineEditorUiState> = _uiState

    val routineExercises: StateFlow<List<RoutineExerciseEntity>> = routineIdFlow
        .flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repository.observeRoutineExercises(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Planned sets grouped by routine exercise: one table row per set. */
    val routineSets: StateFlow<Map<Long, List<RoutineSetEntity>>> = routineIdFlow
        .flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repository.observeRoutineSets(id) }
        .map { sets -> sets.groupBy { it.routineExerciseId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Whole library, feeding the exercise picker sheet. */
    val availableExercises: StateFlow<List<ExerciseEntity>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Exercises referenced by the routine, for their translated name and weight type. Taken from
     * the already observed library instead of one query per row: editing a set used to trigger N
     * database reads returning the same names.
     */
    val exercises: StateFlow<Map<Long, ExerciseEntity>> = availableExercises
        .map { library -> library.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            if (routineId == 0L) {
                // DECISIONE: a draft routine is created upfront to have a valid id exercises can
                // reference through the foreign key before the user saves.
                routineId = repository.saveRoutine(RoutineEntity(name = ""))
                routineIdFlow.value = routineId
                _uiState.update { it.copy(ready = true) }
            } else {
                val existing = repository.getRoutine(routineId)
                _uiState.update {
                    it.copy(
                        name = existing?.name.orEmpty(),
                        notes = existing?.notes.orEmpty(),
                        linkedPlaylistUri = existing?.linkedPlaylistUri.orEmpty(),
                        linkedPlaylistType = existing?.linkedPlaylistType,
                        ready = true
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onPlaylistUriChange(value: String) = _uiState.update { it.copy(linkedPlaylistUri = value) }
    fun onPlaylistTypeChange(value: PlaylistType?) = _uiState.update { it.copy(linkedPlaylistType = value) }

    fun save(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveRoutine(
                RoutineEntity(
                    id = routineId,
                    name = state.name.trim().ifBlank { appContext.getString(R.string.routine_unnamed) },
                    notes = state.notes.trim().ifBlank { null },
                    linkedPlaylistUri = state.linkedPlaylistUri.trim().ifBlank { null },
                    linkedPlaylistType = state.linkedPlaylistType
                )
            )
            onSaved()
        }
    }

    /**
     * Leaving without saving must not leave the draft created on open in the list: it is deleted
     * only when it stayed genuinely empty (no name, no exercises).
     */
    fun discardIfEmpty(onDone: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val existing = repository.getRoutine(routineId)
            val untouched = state.name.isBlank() &&
                state.notes.isBlank() &&
                state.linkedPlaylistUri.isBlank() &&
                routineExercises.value.isEmpty() &&
                existing?.name.isNullOrBlank()
            if (untouched && existing != null) repository.deleteRoutine(existing)
            onDone()
        }
    }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.addExerciseToRoutine(routineId, exerciseId, routineExercises.value.size)
        }
    }

    /** Swaps the movement of a routine row; sets, rest, note, position and superset are kept. */
    fun replaceExercise(routineExercise: RoutineExerciseEntity, exerciseId: Long) {
        viewModelScope.launch { repository.replaceRoutineExercise(routineExercise, exerciseId) }
    }

    fun removeExercise(routineExercise: RoutineExerciseEntity) {
        viewModelScope.launch {
            repository.removeRoutineExercise(routineExercise)
            // A superset left with a single member is no longer a superset.
            val remaining = routineExercises.value.filterNot { it.id == routineExercise.id }
            val cleaned = Superset.dissolveOrphans(remaining.map { Superset.Member(it.id, it.supersetGroup) })
                .associateBy({ it.id }, { it.group })
            remaining.forEach { item ->
                val group = cleaned[item.id]
                if (group != item.supersetGroup) {
                    repository.updateRoutineExercise(item.copy(supersetGroup = group))
                }
            }
        }
    }

    /**
     * Superset of a routine exercise; a null [group] takes it out of the round. A joining exercise
     * moves next to its members (see [Superset.regroup]) and takes their rest, which belongs to
     * the round rather than to the single exercise.
     */
    fun setSupersetGroup(routineExercise: RoutineExerciseEntity, group: Int?) {
        viewModelScope.launch {
            val current = routineExercises.value
            val regrouped = Superset.regroup(
                current.map { Superset.Member(it.id, it.supersetGroup) },
                routineExercise.id,
                group
            )
            val byId = current.associateBy { it.id }
            val companionRest = current
                .firstOrNull { it.supersetGroup == group && it.id != routineExercise.id }
                ?.restSeconds
            regrouped.forEachIndexed { index, member ->
                val item = byId[member.id] ?: return@forEachIndexed
                val rest = if (member.id == routineExercise.id && companionRest != null) companionRest else item.restSeconds
                val updated = item.copy(order = index, supersetGroup = member.group, restSeconds = rest)
                if (updated != item) repository.updateRoutineExercise(updated)
            }
        }
    }

    /**
     * Order chosen by dragging in the reorder sheet: `order` is the position in the list, so only
     * the rows that actually moved are rewritten.
     */
    fun applyOrder(orderedRoutineExerciseIds: List<Long>) {
        viewModelScope.launch {
            val byId = routineExercises.value.associateBy { it.id }
            if (orderedRoutineExerciseIds.size != byId.size) return@launch
            orderedRoutineExerciseIds.forEachIndexed { index, id ->
                val item = byId[id] ?: return@forEachIndexed
                if (item.order != index) repository.updateRoutineExercise(item.copy(order = index))
            }
        }
    }

    /** First free group number for a new superset. */
    fun nextSupersetGroup(): Int = Superset.nextGroup(routineExercises.value.map { it.supersetGroup })

    /** Routine exercise note, copied into the session at every start. */
    fun updateNotes(routineExercise: RoutineExerciseEntity, notes: String?) {
        viewModelScope.launch {
            repository.updateRoutineExercise(
                routineExercise.copy(notes = notes?.trim()?.ifBlank { null })
            )
        }
    }

    fun addSet(routineExercise: RoutineExerciseEntity) {
        viewModelScope.launch { repository.addSetToRoutineExercise(routineExercise.id) }
    }

    fun removeSet(set: RoutineSetEntity) {
        viewModelScope.launch { repository.removeRoutineSet(set) }
    }

    fun updateSetValues(set: RoutineSetEntity, targetReps: Int?, targetWeight: Double?) {
        viewModelScope.launch {
            repository.updateRoutineSet(set.copy(targetReps = targetReps, targetWeight = targetWeight))
        }
    }

    fun setSetType(set: RoutineSetEntity, type: SetType) {
        viewModelScope.launch { repository.updateRoutineSet(set.copy(setType = type)) }
    }

    fun updateRestSeconds(routineExercise: RoutineExerciseEntity, restSeconds: Int) {
        viewModelScope.launch {
            repository.updateRoutineExercise(routineExercise.copy(restSeconds = restSeconds))
            // Superset rest belongs to the round: changing it on one member changes it for all,
            // otherwise the pause would depend on who closes the round.
            val group = routineExercise.supersetGroup ?: return@launch
            routineExercises.value
                .filter { it.supersetGroup == group && it.id != routineExercise.id && it.restSeconds != restSeconds }
                .forEach { repository.updateRoutineExercise(it.copy(restSeconds = restSeconds)) }
        }
    }
}
