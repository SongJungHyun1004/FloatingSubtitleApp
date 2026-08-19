package com.joker.floatingsubtitleapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joker.floatingsubtitleapp.domain.repository.LanguagePreferenceRepository
import com.joker.floatingsubtitleapp.presentation.service.ServiceStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val isServiceRunning: Boolean = false,
    /** 서비스는 실행 중인데, 설정 화면에서 언어를 그새 바꿔서 지금 실행 중인
     *  세션의 언어와 달라진 상태 -> 재시작해야 새 언어가 적용됨을 알려야 함. */
    val languageChangedWhileRunning: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    serviceStateHolder: ServiceStateHolder,
    languagePreferenceRepository: LanguagePreferenceRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        serviceStateHolder.runningLanguages,
        languagePreferenceRepository.selectedLanguages
    ) { running, selected ->
        MainUiState(
            isServiceRunning = running != null,
            languageChangedWhileRunning = running != null &&
                    (running.sourceLang != selected.sourceLang || running.targetLang != selected.targetLang)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )
}