package com.joker.floatingsubtitleapp.domain.model

/**
 * 오버레이가 실제로 그리는 화면 상태.
 *
 * 기존 SubtitleState(finalizedLines: List<String>)를 대체한다.
 * String 대신 SubtitleLine을 쓰는 이유는 각 줄이 고유 id를 가져야
 * Compose에서 "이 줄이 사라진다"를 개별적으로 애니메이션할 수 있기 때문이다.
 *
 * 이 클래스가 GetSubtitleFlowUseCase / SubtitleService / OverlayController
 * / SubtitleOverlay 4개 파일 밖에서도 참조되고 있다면, 그쪽 코드도
 * List<String> -> List<SubtitleLine> 변경에 맞춰 함께 수정해야 한다.
 */
data class SubtitleUiState(
    val finalizedLines: List<SubtitleLine> = emptyList(),
    val partialText: String = ""
)