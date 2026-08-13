package com.eina.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.matchesQuery
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.primaryCategoryFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val query: String = "",
    val selectedCategory: MuscleGroupCategory? = null,
    val exercises: List<ExerciseEntity> = emptyList()
)

class LibraryViewModel(private val repository: WorkoutRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<MuscleGroupCategory?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeExercises(),
        query,
        selectedCategory
    ) { exercises, q, category ->
        val filtered = exercises.filter { exercise ->
            val matchesQuery = q.isBlank() || exercise.matchesQuery(q)
            val matchesCategory = category == null || primaryCategoryFor(exercise.muscleGroupsPrimary) == category
            matchesQuery && matchesCategory
        }
        LibraryUiState(query = q, selectedCategory = category, exercises = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onCategorySelected(category: MuscleGroupCategory?) {
        selectedCategory.value = if (selectedCategory.value == category) null else category
    }

    /**
     * Deletes an exercise created by the user (or imported with a routine). [onResult] receives
     * false when the exercise is still used by a routine or by the history.
     */
    fun deleteCustomExercise(exercise: ExerciseEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.deleteCustomExercise(exercise)) }
    }
}
