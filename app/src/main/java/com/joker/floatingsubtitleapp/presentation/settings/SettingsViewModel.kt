package com.joker.floatingsubtitleapp.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joker.floatingsubtitleapp.data.stt.ModelPrepareProgress
import com.joker.floatingsubtitleapp.data.stt.NetworkUtils
import com.joker.floatingsubtitleapp.data.stt.VoskModelManager
import com.joker.floatingsubtitleapp.data.stt.VoskModels
import com.joker.floatingsubtitleapp.domain.model.SelectedLanguages
import com.joker.floatingsubtitleapp.domain.repository.DisplayPreferenceRepository
import com.joker.floatingsubtitleapp.domain.repository.LanguagePreferenceRepository
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModelDownloadStatus { IDLE, DOWNLOADING, READY, FAILED }

/** Vosk STT 모델은 용량이 커서(수십MB) 진행률까지 표현할 수 있는 별도 상태를 쓴다. */
sealed interface SttModelStatus {
    data object Idle : SttModelStatus
    /** fraction: 0f~1f, -1f면 진행률 알 수 없는 단계(압축 해제 등) */
    data class Downloading(val fraction: Float) : SttModelStatus
    data object Ready : SttModelStatus
    data class Failed(val message: String) : SttModelStatus
}

data class SettingsUiState(
    val selected: SelectedLanguages = SelectedLanguages(),
    val sourceStatus: SttModelStatus = SttModelStatus.Idle,
    val targetStatus: ModelDownloadStatus = ModelDownloadStatus.IDLE,
    /** null이 아니면 "이동통신에서 큰 모델을 받아도 되는지" 확인 다이얼로그를 띄워야 함 */
    val pendingCellularConfirmLangCode: String? = null,
    val showOriginalText: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val translateRepository: TranslateRepository,
    private val voskModelManager: VoskModelManager,
    private val displayPreferenceRepository: DisplayPreferenceRepository
) : ViewModel() {

    private val _sourceStatus = MutableStateFlow<SttModelStatus>(SttModelStatus.Idle)
    private val _targetStatus = MutableStateFlow(ModelDownloadStatus.IDLE)
    private val _pendingCellularConfirm = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        languagePreferenceRepository.selectedLanguages,
        _sourceStatus,
        _targetStatus,
        _pendingCellularConfirm,
        displayPreferenceRepository.showOriginalText
    ) { selected, sourceStatus, targetStatus, pending, showOriginalText ->
        SettingsUiState(selected, sourceStatus, targetStatus, pending, showOriginalText)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        val current = languagePreferenceRepository.selectedLanguages.value
        // 앱을 다시 켰을 때 이미 받아져 있는 모델이면 즉시 Ready로 확인된다
        // (VoskModelManager가 .complete 마커를 보고 재다운로드 안 함).
        prepareSttModel(current.sourceLang, skipCellularCheck = true)
        downloadTranslateModel(current.targetLang)
    }

    /** 언어 선택 UI에서 원본(음성인식) 언어를 바꿀 때 호출. */
    fun selectSourceLang(code: String) {
        languagePreferenceRepository.setSourceLang(code)
        prepareSttModel(code, skipCellularCheck = false)
    }

    /** 언어 선택 UI에서 대상(번역) 언어를 바꿀 때 호출. */
    fun selectTargetLang(code: String) {
        languagePreferenceRepository.setTargetLang(code)
        downloadTranslateModel(code)
    }

    /** 설정 화면의 "원문 같이 보기" 스위치를 토글할 때 호출. 재시작 불필요, 즉시 반영. */
    fun toggleShowOriginalText() {
        val current = displayPreferenceRepository.showOriginalText.value
        displayPreferenceRepository.setShowOriginalText(!current)
    }

    /** 셀룰러 확인 다이얼로그에서 "계속"을 눌렀을 때. */
    fun confirmCellularDownload() {
        val code = _pendingCellularConfirm.value ?: return
        _pendingCellularConfirm.value = null
        startSttDownload(code)
    }

    /** 셀룰러 확인 다이얼로그에서 "취소"를 눌렀을 때. */
    fun cancelCellularDownload() {
        _pendingCellularConfirm.value = null
        _sourceStatus.value = SttModelStatus.Idle
    }

    private fun prepareSttModel(langCode: String, skipCellularCheck: Boolean) {
        // en은 assets 번들이라 네트워크가 아예 필요 없어서 확인 없이 바로 진행.
        val needsNetworkCheck = !skipCellularCheck && langCode != "en"

        if (needsNetworkCheck && !NetworkUtils.isOnWifi(context)) {
            val sizeMb = VoskModels.infoFor(langCode)?.approxSizeMb
            _pendingCellularConfirm.value = langCode
            _sourceStatus.value = SttModelStatus.Downloading(-1f) // 확인 대기 중임을 표시
            // 정확한 용량은 다이얼로그 쪽에서 VoskModels.infoFor(code)로 다시 조회해서 보여준다.
            return
        }

        startSttDownload(langCode)
    }

    private fun startSttDownload(langCode: String) {
        viewModelScope.launch {
            try {
                voskModelManager.prepareModel(langCode).collect { progress ->
                    _sourceStatus.value = when (progress) {
                        is ModelPrepareProgress.InProgress -> SttModelStatus.Downloading(progress.fraction)
                        is ModelPrepareProgress.Done -> SttModelStatus.Ready
                    }
                }
            } catch (e: Exception) {
                _sourceStatus.value = SttModelStatus.Failed(e.message ?: "음성인식 모델 준비 실패")
            }
        }
    }

    private fun downloadTranslateModel(code: String) {
        _targetStatus.value = ModelDownloadStatus.DOWNLOADING
        viewModelScope.launch {
            val result = translateRepository.downloadModelIfNeeded(code)
            _targetStatus.value = if (result.isSuccess) ModelDownloadStatus.READY else ModelDownloadStatus.FAILED
        }
    }
}