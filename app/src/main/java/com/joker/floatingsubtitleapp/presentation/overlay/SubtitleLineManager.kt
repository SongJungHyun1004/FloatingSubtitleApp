package com.joker.floatingsubtitleapp.presentation.overlay

import com.joker.floatingsubtitleapp.domain.model.SubtitleEvent
import com.joker.floatingsubtitleapp.domain.model.SubtitleUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자막 줄의 상태만 담당하는 순수 상태관리자.
 *
 * 확정된 줄은 시간이 지나도 지우지 않는다(무제한 누적, 의도된 선택) - 화면에서
 * 안 보이게 되는 건 SubtitleOverlay가 새 줄이 추가될 때마다 스크롤을 최신
 * 위치로 옮겨서 오래된 줄이 뷰포트 밖으로 밀려나기 때문이고, 데이터 자체는
 * 계속 들고 있는다.
 */
@Singleton
class SubtitleLineManager @Inject constructor() {

    companion object {
        private const val PLACEHOLDER = "인식된 자막이 여기에 표시됩니다."
    }

    private val _uiState = MutableStateFlow(SubtitleUiState(partialText = PLACEHOLDER))
    val uiState: StateFlow<SubtitleUiState> = _uiState.asStateFlow()

    fun onEvent(event: SubtitleEvent) {
        when (event) {
            is SubtitleEvent.PartialUpdated -> {
                _uiState.update {
                    it.copy(
                        partialText = event.text,
                        partialOriginalText = event.originalText
                    )
                }
            }
            is SubtitleEvent.LineFinalized -> {
                _uiState.update { it.copy(finalizedLines = it.finalizedLines + event.line) }
            }
        }
    }

    /** 새 캡처 세션을 시작하거나 서비스가 종료될 때 호출해서 상태를 초기화한다. */
    fun clear() {
        _uiState.value = SubtitleUiState(partialText = PLACEHOLDER)
    }
}