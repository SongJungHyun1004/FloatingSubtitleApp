package com.joker.floatingsubtitleapp.presentation.settings

import com.joker.floatingsubtitleapp.data.stt.VoskModels

data class SupportedLanguage(
    val code: String,
    val displayName: String
)

/**
 * 번역(MLKit) 대상 언어로 고를 수 있는 전체 목록.
 * code는 MLKit의 TranslateLanguage 상수와 동일한 값을 써야 한다.
 */
object SupportedLanguages {
    val all = listOf(
        SupportedLanguage("en", "영어"),
        SupportedLanguage("ko", "한국어"),
        SupportedLanguage("ja", "일본어"),
        SupportedLanguage("zh", "중국어"),
        SupportedLanguage("es", "스페인어"),
        SupportedLanguage("fr", "프랑스어"),
        SupportedLanguage("de", "독일어"),
        SupportedLanguage("vi", "베트남어"),
        SupportedLanguage("th", "태국어"),
        SupportedLanguage("ru", "러시아어"),
    )

    /**
     * 음성인식(Vosk) 소스 언어로 고를 수 있는 목록.
     * Vosk가 공식 모델을 제공하지 않는 언어(예: 태국어)는 제외한다.
     */
    val sttSupported: List<SupportedLanguage> =
        all.filter { it.code in VoskModels.allSttSupportedLangCodes }

    fun displayNameOf(code: String): String =
        all.firstOrNull { it.code == code }?.displayName ?: code
}