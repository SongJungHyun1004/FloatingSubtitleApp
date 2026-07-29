package com.joker.floatingsubtitleapp.domain.model

data class SubtitleState(
    val finalizedLines: List<String> = emptyList(),
    val partialText: String = ""
)