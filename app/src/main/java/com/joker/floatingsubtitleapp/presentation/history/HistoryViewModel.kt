package com.joker.floatingsubtitleapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import com.joker.floatingsubtitleapp.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val sessions: StateFlow<List<RecordedSessionSummary>> = historyRepository.sessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            historyRepository.deleteSession(sessionId)
        }
    }
}