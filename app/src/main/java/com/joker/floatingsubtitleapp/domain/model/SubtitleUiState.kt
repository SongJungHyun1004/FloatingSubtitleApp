package com.joker.floatingsubtitleapp.domain.model

/**
 * 오버레이가 실제로 그리는 화면 상태.
 *
 * @param partialText 실시간 번역 미리보기 (회색으로 표시)
 * @param partialOriginalText 그 미리보기의 번역 전 원문 - "원문 같이 보기"
 *                            설정이 켜졌을 때만 화면에 표시된다.
 */
data class SubtitleUiState(
    val finalizedLines: List<SubtitleLine> = emptyList(),
    val partialText: String = "",
    val partialOriginalText: String = ""
)