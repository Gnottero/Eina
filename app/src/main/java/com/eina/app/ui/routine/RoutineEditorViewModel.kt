package com.eina.app.ui.routine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.R
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.repository.RoutineRepository
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

    val exerciseNames: StateFlow<Map<Long, String>> = routineExercises
        .map { list -> list.associate { it.exerciseId to (repository.getExercise(it.exerciseId)?.name ?: "?") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            if (routineId == 0L) {
                // DECISIONE: creiamo subito una routine "bozza" per avere un id valido a cui
                // agganciare esercizi via FK prima che l'utente prema Salva.
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
     * Uscire senza salvare non deve lasciare in lista la bozza creata all'apertura:
     * la si elimina solo se e' rimasta davvero vuota (nessun nome, nessun esercizio).
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

    fun removeExercise(routineExercise: RoutineExerciseEntity) {
        viewModelScope.launch { repository.removeRoutineExercise(routineExercise) }
    }

    /** Nota dell'esercizio nella routine: viene copiata nella sessione a ogni avvio. */
    fun updateNotes(routineExercise: RoutineExerciseEntity, notes: String?) {
        viewModelScope.launch {
            repository.updateRoutineExercise(
                routineExercise.copy(notes = notes?.trim()?.ifBlank { null })
            )
        }
    }

    fun updateTargets(routineExercise: RoutineExerciseEntity, targetSets: Int, targetReps: Int, targetWeight: Double?, restSeconds: Int) {
        viewModelScope.launch {
            repository.updateRoutineExercise(
                routineExercise.copy(
                    targetSets = targetSets,
                    targetReps = targetReps,
                    targetWeight = targetWeight,
                    restSeconds = restSeconds
                )
            )
        }
    }
}
