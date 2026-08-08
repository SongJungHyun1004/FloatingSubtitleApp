package com.joker.floatingsubtitleapp.data.settings

import android.content.Context
import androidx.core.content.edit
import com.joker.floatingsubtitleapp.domain.model.SelectedLanguages
import com.joker.floatingsubtitleapp.domain.repository.LanguagePreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePreferenceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LanguagePreferenceRepository {

    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val DEFAULT_SOURCE = "en"
        private const val DEFAULT_TARGET = "ko"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedLanguages = MutableStateFlow(
        SelectedLanguages(
            sourceLang = prefs.getString(KEY_SOURCE_LANG, DEFAULT_SOURCE) ?: DEFAULT_SOURCE,
            targetLang = prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET) ?: DEFAULT_TARGET
        )
    )
    override val selectedLanguages: StateFlow<SelectedLanguages> = _selectedLanguages.asStateFlow()

    override fun setSourceLang(code: String) {
        prefs.edit { putString(KEY_SOURCE_LANG, code) }
        _selectedLanguages.value = _selectedLanguages.value.copy(sourceLang = code)
    }

    override fun setTargetLang(code: String) {
        prefs.edit { putString(KEY_TARGET_LANG, code) }
        _selectedLanguages.value = _selectedLanguages.value.copy(targetLang = code)
    }
}