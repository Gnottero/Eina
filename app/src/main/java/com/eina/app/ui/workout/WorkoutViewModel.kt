package com.eina.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {
    fun startNewSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = repository.startSession()
            onCreated(sessionId)
        }
    }
}
