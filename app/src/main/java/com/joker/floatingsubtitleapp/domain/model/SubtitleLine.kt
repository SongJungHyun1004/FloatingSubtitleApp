package com.joker.floatingsubtitleapp.domain.model

/**
 * 확정(Final)된 자막 한 줄.
 *
 * 더 이상 시간이 지나면 지워지지 않는다 - 화면에서는 스크롤로 밀려 올라가
 * 안 보이게 될 뿐, 목록 자체에서는 계속 유지된다(무제한 누적, 의도된 선택).
 *
 * @param text 번역된 텍스트 (화면에 크고 진하게 표시됨)
 * @param originalText 번역 전 원문 (설정에서 "원문 같이 보기" 켰을 때만
 *                      화면에 작고 흐리게 같이 표시됨)
 */
data class SubtitleLine(
    val id: String,
    val text: String,
    val originalText: String
)