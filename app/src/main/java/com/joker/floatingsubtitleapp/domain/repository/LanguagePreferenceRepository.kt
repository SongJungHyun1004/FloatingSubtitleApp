package com.joker.floatingsubtitleapp.domain.repository

import com.joker.floatingsubtitleapp.domain.model.SelectedLanguages
import kotlinx.coroutines.flow.StateFlow

interface LanguagePreferenceRepository {
    /** 현재 선택된 원본/대상 언어. 앱 재실행 후에도 마지막 선택이 유지된다. */
    val selectedLanguages: StateFlow<SelectedLanguages>

    fun setSourceLang(code: String)
    fun setTargetLang(code: String)

    /** 원본/대상 언어를 서로 맞바꾼다. (번역앱 스타일의 스왑 버튼용) */
    fun swapLanguages()
}