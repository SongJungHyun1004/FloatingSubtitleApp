package com.joker.floatingsubtitleapp.domain.model

sealed interface SpeechResult {
    data class Partial(val text: String) : SpeechResult
    data class Final(val text: String) : SpeechResult
}