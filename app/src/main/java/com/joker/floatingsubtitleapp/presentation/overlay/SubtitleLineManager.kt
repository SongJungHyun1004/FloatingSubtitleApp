package com.joker.floatingsubtitleapp.presentation.overlay

import com.joker.floatingsubtitleapp.domain.model.SubtitleEvent
import com.joker.floatingsubtitleapp.domain.model.SubtitleLine
import com.joker.floatingsubtitleapp.domain.model.SubtitleUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자막 줄의 생명주기만 담당하는 순수 상태관리자.
 *
 * OverlayController(WindowManager/뷰 붙이고 떼기)와 책임을 분리하기 위해
 * 만들었다. 이 클래스는 Android UI 시스템을 전혀 몰라도 되고, 테스트할 때도
 * WindowManager 없이 uiState만 관찰하면 된다.
 *
 * 각 확정 줄(SubtitleLine)은 고유 id로 자신만의 타이머를 갖는다 -> 새 줄이
 * 들어와도 기존 줄의 타이머는 전혀 영향받지 않는다 (기존 OverlayController의
 * 버그였던 "공용 타이머 1개 + 무조건 맨 앞줄 삭제" 문제 해결).
 */
@Singleton
class SubtitleLineManager @Inject constructor() {

    companion object {
        private const val LINE_LIFETIME_MS = 3000L   // 확정 줄이 화면에 유지되는 시간
        private const val EXIT_ANIM_MS = 300L         // 퇴장 애니메이션 재생 시간
        private const val PLACEHOLDER = "인식된 자막이 여기에 표시됩니다."
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SubtitleUiState(partialText = PLACEHOLDER))
    val uiState: StateFlow<SubtitleUiState> = _uiState.asStateFlow()

    // 줄 id -> 그 줄의 소멸 타이머 Job. 줄마다 독립적으로 관리된다.
    private val expiryJobs = mutableMapOf<String, Job>()

    fun onEvent(event: SubtitleEvent) {
        when (event) {
            is SubtitleEvent.PartialUpdated -> {
                _uiState.update { it.copy(partialText = event.text) }
            }
            is SubtitleEvent.LineFinalized -> {
                addLine(event.line)
            }
        }
    }

    private fun addLine(line: SubtitleLine) {
        _uiState.update { it.copy(finalizedLines = it.finalizedLines + line) }

        expiryJobs[line.id] = managerScope.launch {
            delay(LINE_LIFETIME_MS)
            markExiting(line.id)
            delay(EXIT_ANIM_MS)
            removeLine(line.id)
        }
    }

    private fun markExiting(id: String) {
        _uiState.update { state ->
            state.copy(
                finalizedLines = state.finalizedLines.map {
                    if (it.id == id) it.copy(isExiting = true) else it
                }
            )
        }
    }

    private fun removeLine(id: String) {
        _uiState.update { state ->
            state.copy(finalizedLines = state.finalizedLines.filterNot { it.id == id })
        }
        expiryJobs.remove(id)
    }

    /** 새 캡처 세션을 시작하거나 서비스가 종료될 때 호출해서 잔여 타이머/상태를 정리한다. */
    fun clear() {
        expiryJobs.values.forEach { it.cancel() }
        expiryJobs.clear()
        _uiState.value = SubtitleUiState(partialText = PLACEHOLDER)
    }
}