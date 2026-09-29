package com.joker.floatingsubtitleapp.domain.model

/** 원문은 항상 번역문보다 작게 표시된다는 비율을 유지하면서 크기만 조절한다. */
enum class SubtitleFontSize(val translatedSp: Int, val originalSp: Int, val label: String) {
    SMALL(14, 10, "소"),
    MEDIUM(18, 13, "중"),
    LARGE(24, 17, "대")
}