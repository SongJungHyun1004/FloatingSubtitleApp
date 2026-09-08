package com.joker.floatingsubtitleapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _lines = MutableStateFlow<List<RecordedLine>>(emptyList())
    val lines: StateFlow<List<RecordedLine>> = _lines.asStateFlow()

    private var loadedSessionId: Long? = null

    /** 같은 화면 인스턴스가 다른 세션 id로 재사용될 수 있어서, 매번 이 함수로 갱신한다. */
    fun load(sessionId: Long) {
        if (loadedSessionId == sessionId) return
        loadedSessionId = sessionId
        viewModelScope.launch {
            _lines.value = historyRepository.getLines(sessionId)
        }
    }
}