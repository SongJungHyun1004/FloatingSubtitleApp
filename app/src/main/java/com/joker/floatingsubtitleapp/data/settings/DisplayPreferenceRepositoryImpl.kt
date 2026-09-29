package com.joker.floatingsubtitleapp.data.settings

import android.content.Context
import androidx.core.content.edit
import com.joker.floatingsubtitleapp.domain.model.SubtitleFontSize
import com.joker.floatingsubtitleapp.domain.repository.DisplayPreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplayPreferenceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DisplayPreferenceRepository {

    companion object {
        private const val PREFS_NAME = "display_prefs"
        private const val KEY_SHOW_ORIGINAL_TEXT = "show_original_text"
        private const val KEY_FONT_SIZE = "font_size"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _showOriginalText = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_ORIGINAL_TEXT, false)
    )
    override val showOriginalText: StateFlow<Boolean> = _showOriginalText.asStateFlow()

    override fun setShowOriginalText(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_ORIGINAL_TEXT, enabled) }
        _showOriginalText.value = enabled
    }

    private val _fontSize = MutableStateFlow(
        runCatching {
            SubtitleFontSize.valueOf(prefs.getString(KEY_FONT_SIZE, null) ?: SubtitleFontSize.MEDIUM.name)
        }.getOrDefault(SubtitleFontSize.MEDIUM)
    )
    override val fontSize: StateFlow<SubtitleFontSize> = _fontSize.asStateFlow()

    override fun setFontSize(size: SubtitleFontSize) {
        prefs.edit { putString(KEY_FONT_SIZE, size.name) }
        _fontSize.value = size
    }
}