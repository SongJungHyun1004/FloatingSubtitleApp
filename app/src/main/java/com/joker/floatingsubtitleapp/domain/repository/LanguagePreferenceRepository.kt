package com.joker.floatingsubtitleapp.domain.repository

import com.joker.floatingsubtitleapp.domain.model.SelectedLanguages
import kotlinx.coroutines.flow.StateFlow

interface LanguagePreferenceRepository {
    /** 현재 선택된 원본/대상 언어. 앱 재실행 후에도 마지막 선택이 유지된다. */
    val selectedLanguages: StateFlow<SelectedLanguages>

    fun setSourceLang(code: String)
    fun setTargetLang(code: String)
}