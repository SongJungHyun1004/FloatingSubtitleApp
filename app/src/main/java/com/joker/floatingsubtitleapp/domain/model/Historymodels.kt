package com.joker.floatingsubtitleapp.domain.model

/** 기록 목록 화면에 보여줄 세션 요약 정보. */
data class RecordedSessionSummary(
    val id: Long,
    val startedAt: Long,
    val sourceLang: String,
    val targetLang: String
)

/** 세션 상세 화면에 보여줄 저장된 자막 한 줄. */
data class RecordedLine(
    val originalText: String,
    val translatedText: String
)