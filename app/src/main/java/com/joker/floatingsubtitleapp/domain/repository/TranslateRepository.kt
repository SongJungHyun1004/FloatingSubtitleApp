package com.joker.floatingsubtitleapp.domain.repository

interface TranslateRepository {
    /**
     * 텍스트를 번역합니다.
     * @param text 원문
     * @param sourceLang 원본 언어 (예: "en")
     * @param targetLang 대상 언어 (예: "ko")
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String>

    /**
     * 번역에 필요한 언어 모델을 다운로드합니다.
     */
    suspend fun downloadModelIfNeeded(langCode: String): Result<Unit>
}