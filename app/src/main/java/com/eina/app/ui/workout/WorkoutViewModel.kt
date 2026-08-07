package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.db.WorkoutSessionEntity
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    /** Allenamento ancora aperto: finche' c'e', la schermata offre "Riprendi" invece di "Inizia". */
    val activeSession: StateFlow<WorkoutSessionEntity?> = repository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startNewSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = repository.startSession()
            onCreated(sessionId)
        }
    }
}
