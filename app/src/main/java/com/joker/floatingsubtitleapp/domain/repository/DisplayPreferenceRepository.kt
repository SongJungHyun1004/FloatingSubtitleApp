package com.joker.floatingsubtitleapp.domain.repository

import com.joker.floatingsubtitleapp.domain.model.SubtitleFontSize
import kotlinx.coroutines.flow.StateFlow

interface DisplayPreferenceRepository {
    /** 확정 줄/partial 텍스트 아래에 원문(듣는 언어 그대로)을 작고 흐리게 같이 보여줄지 여부. */
    val showOriginalText: StateFlow<Boolean>

    fun setShowOriginalText(enabled: Boolean)

    /** 자막 폰트 크기 프리셋(소/중/대). */
    val fontSize: StateFlow<SubtitleFontSize>

    fun setFontSize(size: SubtitleFontSize)
}