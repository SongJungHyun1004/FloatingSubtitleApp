package com.joker.floatingsubtitleapp.presentation.settings

data class SupportedLanguage(
    val code: String,
    val displayName: String
)

/**
 * MLKit Translate가 지원하는 언어 중 일부를 골라놓은 목록.
 * code는 MLKit의 TranslateLanguage 상수와 동일한 값을 써야 한다
 * (예: TranslateLanguage.ENGLISH == "en").
 * 더 필요한 언어가 있으면 여기에 추가하면 된다.
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

    fun displayNameOf(code: String): String =
        all.firstOrNull { it.code == code }?.displayName ?: code
}