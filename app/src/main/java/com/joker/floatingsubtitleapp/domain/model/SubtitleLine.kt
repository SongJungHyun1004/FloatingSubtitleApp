package com.joker.floatingsubtitleapp.domain.model

/**
 * 확정(Final)된 자막 한 줄.
 *
 * 더 이상 시간이 지나면 지워지지 않는다 - 화면에서는 스크롤로 밀려 올라가
 * 안 보이게 될 뿐, 목록 자체에서는 계속 유지된다(무제한 누적, 의도된 선택).
 */
data class SubtitleLine(
    val id: String,
    val text: String
)