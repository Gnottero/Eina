package com.eina.app.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.canonicalMuscleKey
import com.eina.app.ui.theme.categoryFor
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateExerciseUiState(
    // Fase 32: lo stesso form crea e corregge. Non nullo = si sta modificando un esercizio
    // custom che esiste gia', e il salvataggio riscrive quella riga invece di aggiungerne una.
    val exerciseId: Long? = null,
    val name: String = "",
    val description: String = "",
    val loggingInstructions: String = "",
    val weightType: WeightType = WeightType.FREE_WEIGHT,
    val primaryCategories: Set<MuscleGroupCategory> = emptySet(),
    val secondaryCategories: Set<MuscleGroupCategory> = emptySet(),
    val equipment: String = "",
    val mediaUri: String? = null,
    val mediaError: String? = null,
    val saved: Boolean = false
) {
    val canSave: Boolean get() = name.isNotBlank() && primaryCategories.isNotEmpty()
}

class CreateExerciseViewModel(
    private val repository: WorkoutRepository,
    private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateExerciseUiState())
    val uiState: StateFlow<CreateExerciseUiState> = _uiState

    /** Colonne dell'esercizio che il form non mostra e che la modifica non deve perdere. */
    private var editing: ExerciseEntity? = null

    /**
     * Carica un esercizio custom nel form. Si chiama una volta sola: rileggerlo a ogni
     * ricomposizione butterebbe via quello che si sta digitando.
     */
    fun load(exerciseId: Long) {
        if (editing?.id == exerciseId) return
        viewModelScope.launch {
            val exercise = repository.getExercise(exerciseId)?.takeIf { it.isCustom } ?: return@launch
            editing = exercise
            _uiState.value = CreateExerciseUiState(
                exerciseId = exercise.id,
                name = exercise.name,
                description = exercise.description,
                loggingInstructions = exercise.loggingInstructions,
                weightType = exercise.weightType,
                primaryCategories = exercise.muscleGroupsPrimary.map { categoryFor(it) }.toSet(),
                secondaryCategories = exercise.muscleGroupsSecondary.map { categoryFor(it) }.toSet(),
                equipment = exercise.equipment.orEmpty(),
                mediaUri = exercise.mediaUri
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onLoggingInstructionsChange(value: String) = _uiState.update { it.copy(loggingInstructions = value) }
    fun onWeightTypeChange(value: WeightType) = _uiState.update { it.copy(weightType = value) }
    fun onEquipmentChange(value: String) = _uiState.update { it.copy(equipment = value) }

    fun onPrimaryCategoryToggle(category: MuscleGroupCategory) = _uiState.update {
        it.copy(primaryCategories = it.primaryCategories.toggled(category))
    }

    fun onSecondaryCategoryToggle(category: MuscleGroupCategory) = _uiState.update {
        it.copy(secondaryCategories = it.secondaryCategories.toggled(category))
    }

    fun onMediaPicked(uri: Uri) = viewModelScope.launch {
        val copied = copyMediaToInternalStorage(uri)
        _uiState.update {
            if (copied == null) {
                // La copia puo' fallire su file remoti (Drive offline) o revocati: senza un
                // messaggio l'utente vedeva solo l'anteprima non comparire.
                it.copy(mediaError = appContext.getString(R.string.error_media_copy))
            } else {
                it.copy(mediaUri = copied, mediaError = null)
            }
        }
    }

    /** Nessuna app risponde alla richiesta di immagini: device senza galleria o picker disabilitato. */
    fun onMediaPickerUnavailable() = _uiState.update {
        it.copy(mediaError = appContext.getString(R.string.error_no_gallery))
    }

    private suspend fun copyMediaToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val mediaDir = File(appContext.filesDir, "exercise_media").apply { mkdirs() }
            val extension = appContext.contentResolver.getType(uri)?.substringAfterLast('/') ?: "gif"
            val destination = File(mediaDir, "${UUID.randomUUID()}.$extension")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination.absolutePath
        }.getOrNull()
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            // In modifica si parte dalla riga esistente: nomi e descrizioni tradotti e
            // bodyweightFactor non stanno nel form e resterebbero indietro ricostruendola.
            val base = editing ?: ExerciseEntity(
                name = "",
                description = "",
                loggingInstructions = "",
                weightType = WeightType.FREE_WEIGHT,
                muscleGroupsPrimary = emptyList(),
                muscleGroupsSecondary = emptyList(),
                isCustom = true,
                source = null
            )
            val exercise = base.copy(
                name = state.name.trim(),
                description = state.description.trim(),
                loggingInstructions = state.loggingInstructions.trim(),
                weightType = state.weightType,
                muscleGroupsPrimary = state.primaryCategories.map { canonicalMuscleKey(it) },
                muscleGroupsSecondary = state.secondaryCategories.map { canonicalMuscleKey(it) },
                equipment = state.equipment.trim().ifBlank { null },
                mediaUri = state.mediaUri
            )
            if (editing != null) {
                repository.updateCustomExercise(exercise)
                editing = exercise
            } else {
                repository.insertExercise(exercise)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> = if (contains(value)) this - value else this + value
