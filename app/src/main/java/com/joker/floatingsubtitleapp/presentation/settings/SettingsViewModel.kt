package com.joker.floatingsubtitleapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joker.floatingsubtitleapp.domain.model.SelectedLanguages
import com.joker.floatingsubtitleapp.domain.repository.LanguagePreferenceRepository
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModelDownloadStatus { IDLE, DOWNLOADING, READY, FAILED }

data class SettingsUiState(
    val selected: SelectedLanguages = SelectedLanguages(),
    val sourceStatus: ModelDownloadStatus = ModelDownloadStatus.IDLE,
    val targetStatus: ModelDownloadStatus = ModelDownloadStatus.IDLE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val translateRepository: TranslateRepository
) : ViewModel() {

    // Pair(sourceStatus, targetStatus)
    private val _downloadStatus = MutableStateFlow(
        ModelDownloadStatus.IDLE to ModelDownloadStatus.IDLE
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        languagePreferenceRepository.selectedLanguages,
        _downloadStatus
    ) { selected, (sourceStatus, targetStatus) ->
        SettingsUiState(selected, sourceStatus, targetStatus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        // 앱을 다시 켰을 때, 저장돼 있던 언어의 모델이 실제로 준비돼 있는지 확인.
        // downloadModelIfNeeded()는 이미 받아져 있으면 그냥 바로 성공 처리되므로
        // 매번 불러도 비용이 크지 않다.
        val current = languagePreferenceRepository.selectedLanguages.value
        downloadModel(isSource = true, code = current.sourceLang)
        downloadModel(isSource = false, code = current.targetLang)
    }

    /** 언어 선택 UI에서 원본 언어를 바꿀 때 호출. 선택 즉시 모델 다운로드를 시작한다. */
    fun selectSourceLang(code: String) {
        languagePreferenceRepository.setSourceLang(code)
        downloadModel(isSource = true, code = code)
    }

    /** 언어 선택 UI에서 대상(번역) 언어를 바꿀 때 호출. 선택 즉시 모델 다운로드를 시작한다. */
    fun selectTargetLang(code: String) {
        languagePreferenceRepository.setTargetLang(code)
        downloadModel(isSource = false, code = code)
    }

    private fun downloadModel(isSource: Boolean, code: String) {
        updateStatus(isSource, ModelDownloadStatus.DOWNLOADING)
        viewModelScope.launch {
            val result = translateRepository.downloadModelIfNeeded(code)
            updateStatus(
                isSource,
                if (result.isSuccess) ModelDownloadStatus.READY else ModelDownloadStatus.FAILED
            )
        }
    }

    private fun updateStatus(isSource: Boolean, status: ModelDownloadStatus) {
        _downloadStatus.value = if (isSource) {
            status to _downloadStatus.value.second
        } else {
            _downloadStatus.value.first to status
        }
    }
}